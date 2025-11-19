package com.example.weatherwhiz.data

import com.example.weatherwhiz.domain.CityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CityRepositoryImpl @Inject constructor(
    private val cityDao: CityDao
) : CityRepository {
    override fun getAllCities(): Flow<List<City>> = cityDao.getAllCities()
}