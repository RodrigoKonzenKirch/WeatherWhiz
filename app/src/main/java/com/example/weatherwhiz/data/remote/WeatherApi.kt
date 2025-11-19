package com.example.weatherwhiz.data.remote

import com.example.weatherwhiz.data.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("/v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") variables: String = "temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m", // Changed to 'current'
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}
