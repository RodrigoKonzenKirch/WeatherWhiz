package com.example.weatherwhiz.domain

import com.example.weatherwhiz.data.City
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getAllCities(): Flow<List<City>>
}
