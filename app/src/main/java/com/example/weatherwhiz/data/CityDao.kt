package com.example.weatherwhiz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM cities ORDER BY name ASC")
    fun getAllCities(): Flow<List<CityEntity>>

    @Insert
    suspend fun insert(city: CityEntity)

    @Query("DELETE FROM cities WHERE isUserAdded = 1 AND id = :cityId")
    suspend fun delete(cityId: Int)
}
