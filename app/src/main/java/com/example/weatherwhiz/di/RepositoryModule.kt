package com.example.weatherwhiz.di

import com.example.weatherwhiz.domain.CityRepository
import com.example.weatherwhiz.data.CityRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {
    @Binds
    abstract fun bindCityRepository(impl: CityRepositoryImpl): CityRepository
}
