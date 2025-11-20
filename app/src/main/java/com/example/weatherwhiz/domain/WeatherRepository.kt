package com.example.weatherwhiz.domain

import com.example.weatherwhiz.data.CityEntity
import com.example.weatherwhiz.data.QuizItem
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getAllCities(): Flow<List<CityEntity>>

    suspend fun fetchQuizData(cities: List<CityEntity>): List<QuizItem>
}
