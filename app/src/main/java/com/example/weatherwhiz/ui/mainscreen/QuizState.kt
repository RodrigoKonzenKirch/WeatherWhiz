package com.example.weatherwhiz.ui.mainscreen

// Define the state the UI will observe
sealed class QuizState {
    data object Loading : QuizState()
    data class Success(
        val cityNames: List<String>, // Shuffled list of names
        val weatherCards: List<WeatherCard> // Shuffled list of weather data
    ) : QuizState()
    data class Error(val message: String) : QuizState()
    data object Idle : QuizState()
    data class GameOver(val finalWrongGuesses: Int, val totalCities: Int) : QuizState()
}

// A simple model for the weather cards displayed on the UI
data class WeatherCard(
    val cityId: Int, // The ID is needed to check the match!
    val temperature: Double,
    val weatherCode: Int, // For displaying the icon
    // ... other display properties
)