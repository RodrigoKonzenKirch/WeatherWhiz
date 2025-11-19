package com.example.weatherwhiz.data

// Data class to map the nested 'current' weather conditions
data class CurrentWeather(
    val time: String, // Timestamp of the data
    val interval: Int,
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val weather_code: Int,
    val wind_speed_10m: Double
)