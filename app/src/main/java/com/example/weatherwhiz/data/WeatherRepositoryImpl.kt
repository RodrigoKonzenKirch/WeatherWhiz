package com.example.weatherwhiz.data

import com.example.weatherwhiz.data.remote.WeatherApiService
import com.example.weatherwhiz.domain.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: WeatherApiService,
    private val cityDao: CityDao
) : WeatherRepository {
    override fun getAllCities(): Flow<List<CityEntity>> = cityDao.getAllCities()

    override suspend fun fetchQuizData(cities: List<CityEntity>): List<QuizItem> =
        coroutineScope {
            val deferredResults = cities.map { city ->
                async(Dispatchers.IO) {
                    try {
                        val response = apiService.getCurrentWeather(city.latitude, city.longitude)

                        mapToQuizItem(city, response)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            val quizItems = deferredResults.awaitAll()
            quizItems.filterNotNull()
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