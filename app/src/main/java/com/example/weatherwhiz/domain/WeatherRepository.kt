package com.example.weatherwhiz.domain

import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.domain.models.QuizItem
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getAllCities(): Flow<List<CityEntity>>

    suspend fun fetchQuizData(cities: List<CityEntity>): List<QuizItem>
}
