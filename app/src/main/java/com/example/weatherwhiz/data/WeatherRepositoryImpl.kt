package com.example.weatherwhiz.data

import com.example.weatherwhiz.data.local.CityDao
import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.data.remote.WeatherApiService
import com.example.weatherwhiz.data.remote.WeatherResponse
import com.example.weatherwhiz.di.IoDispatcher
import com.example.weatherwhiz.domain.Resource
import com.example.weatherwhiz.domain.WeatherRepository
import com.example.weatherwhiz.domain.models.QuizItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: WeatherApiService,
    private val cityDao: CityDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WeatherRepository {
    override fun getAllCities(): Flow<List<CityEntity>> = cityDao.getAllCities()

    override suspend fun fetchQuizData(cities: List<CityEntity>): Resource<List<QuizItem>> =
        coroutineScope {
            try {
                val deferredResults = cities.map { city ->
                    async(ioDispatcher) {
                        try {
                            val response = apiService.getCurrentWeather(city.latitude, city.longitude)
                            mapToQuizItem(city, response)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                val quizItems = deferredResults.awaitAll().filterNotNull()

                if (quizItems.isEmpty() && cities.isNotEmpty()) {
                    Resource.Error("Could not fetch weather data for any selected cities.")
                } else {
                    Resource.Success(quizItems)
                }
            } catch (e: Exception) {
                Resource.Error("An unexpected error occurred: ${e.message}", e)
            }
        }

    private fun mapToQuizItem(city: CityEntity, response: WeatherResponse): QuizItem {
        val weather = response.current
        val units = response.current_units

        return QuizItem(
            cityId = city.id,
            cityName = city.name,
            temperature = weather.temperature_2m,
            temperatureUnit = units.temperature_2m,
            humidity = weather.relative_humidity_2m,
            windSpeed = weather.wind_speed_10m,
            windSpeedUnit = units.wind_speed_10m,
            weatherCode = weather.weather_code
        )
    }
}