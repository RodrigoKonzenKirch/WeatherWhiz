package com.example.weatherwhiz.data

// Data class for a single item in the quiz (City + its Weather)
data class QuizItem(
    val cityId: Int, // From Room, useful for tracking
    val cityName: String, // The city name (the correct answer part)

    // The Weather variables for the quiz card (the answer pool)
    val temperature: Double,
    val temperatureUnit: String,
    val humidity: Int,
    val windSpeed: Double,
    val windSpeedUnit: String,
    val weatherCode: Int // Used to map to a visual icon/description
)