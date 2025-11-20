package com.example.weatherwhiz.data

import com.example.weatherwhiz.domain.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val cityDao: CityDao
) : WeatherRepository {
    override fun getAllCities(): Flow<List<City>> = cityDao.getAllCities()
}