package com.example.weatherwhiz.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.weatherwhiz.R
import com.example.weatherwhiz.data.AppDatabase
import com.example.weatherwhiz.data.CityEntity
import com.example.weatherwhiz.data.CityDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        callback: RoomDatabase.Callback
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "weatherwhiz-database"
        ).addCallback(callback).build()
    }

    @Provides
    fun provideCityDao(appDatabase: AppDatabase): CityDao {
        return appDatabase.cityDao()
    }

    @Provides
    @Singleton
    fun provideCallback(
        @ApplicationContext context: Context,
        cityDaoProvider: Provider<CityDao>
    ): RoomDatabase.Callback {
        return object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val cityDao = cityDaoProvider.get()
                    val jsonString = context.resources.openRawResource(R.raw.cities).bufferedReader().use { it.readText() }
                    val typeToken = object : TypeToken<List<CityEntity>>() {}.type
                    val cities = Gson().fromJson<List<CityEntity>>(jsonString, typeToken)
                    cities.forEach { cityDao.insert(it) }
                }
            }
        }
    }
}
