package com.example.weatherwhiz.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [City::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cityDao(): CityDao
}
