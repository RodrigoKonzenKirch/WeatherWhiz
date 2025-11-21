package com.example.weatherwhiz.data.remote

// Data class to map the top-level API response
data class WeatherResponse(
    // Includes metadata, but the core data is here:
    val current_units: CurrentUnits,
    val current: CurrentWeather
)