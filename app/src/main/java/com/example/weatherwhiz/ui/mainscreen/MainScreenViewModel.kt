package com.example.weatherwhiz.ui.mainscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwhiz.data.CityEntity
import com.example.weatherwhiz.data.QuizItem
import com.example.weatherwhiz.domain.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _quizState = MutableStateFlow<QuizState>(QuizState.Idle)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private var currentQuizItems: List<QuizItem> = emptyList()
    private val _matchedCityIds = MutableStateFlow<Set<Int>>(emptySet())
    val matchedCityIds: StateFlow<Set<Int>> = _matchedCityIds.asStateFlow()

    val cities: StateFlow<List<CityEntity>> = weatherRepository.getAllCities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ----------------------------------------------------
    // Function to start the data fetching and quiz preparation
    // ----------------------------------------------------
    fun loadNewQuiz(selectedCities: List<CityEntity>) {
        // Use viewModelScope for coroutines tied to the ViewModel's lifecycle
        viewModelScope.launch {
            _quizState.value = QuizState.Loading

            try {
                // 1. Fetch the unified data from the Repository
                val allQuizItems = weatherRepository.fetchQuizData(selectedCities)

                if (allQuizItems.isEmpty()) {
                    // Handle case where all API calls failed or city list was empty
                    _quizState.value = QuizState.Error("Could not fetch data for any selected cities.")
                    return@launch
                }

                currentQuizItems = allQuizItems

                // 2. Prepare the quiz lists (The core quiz logic!)
                val (shuffledNames, shuffledWeatherCards) = prepareQuizLists(allQuizItems)

                // 3. Set success state with the prepared data
                _quizState.value = QuizState.Success(
                    cityNames = shuffledNames,
                    weatherCards = shuffledWeatherCards
                )
            } catch (e: Exception) {
                // Set error state if something went wrong in the repository or beyond
                Log.e("QuizViewModel", "Error loading quiz: ${e.message}")
                _quizState.value = QuizState.Error("An error occurred while preparing the quiz.")
            }
        }
    }

    // ----------------------------------------------------
    // Helper function for shuffling the lists
    // ----------------------------------------------------
    private fun prepareQuizLists(items: List<QuizItem>): Pair<List<String>, List<WeatherCard>> {
        // Create the list of city names
        val cityNames = items.map { it.cityName }.shuffled()

        // Create the list of weather cards
        val weatherCards = items.map { item ->
            WeatherCard(
                cityId = item.cityId,
                temperature = item.temperature,
                weatherCode = item.weatherCode
                // Map other necessary display fields here
            )
        }.shuffled() // Shuffle the weather cards

        return Pair(cityNames, weatherCards)
    }

    fun getCityIdForName(cityName: String): Int? {
        return currentQuizItems.firstOrNull { it.cityName == cityName }?.cityId
    }

    fun checkMatch(nameIndex: Int, weatherIndex: Int) {

        // 1. Ensure the current state is Success and get the shuffled lists
        val currentState = _quizState.value
        if (currentState !is QuizState.Success) {
            // Quiz is not active, ignore the tap
            return
        }

        // 2. Get the specific data points chosen by the user:
        val selectedCityName = currentState.cityNames[nameIndex]
        val selectedWeatherCard = currentState.weatherCards[weatherIndex]

        // 3. Look up the correct cityId for the selected name from the original data
        // This assumes the name is unique, which is safe for this quiz scope.
        val correctNameItem = currentQuizItems.firstOrNull { it.cityName == selectedCityName }

        // 4. Verify the match: Does the weather card's cityId match the cityId associated with the name?
        val isCorrect = correctNameItem?.cityId == selectedWeatherCard.cityId

        if (isCorrect) {
            // Update the score/state for correct answers
            // Add the matched cityId to the set of successfully matched IDs
            _matchedCityIds.value += selectedWeatherCard.cityId
        } else {
            // Handle incorrect guess (e.g., provide feedback to the UI)
            Log.d("QuizViewModel", "Incorrect match for $selectedCityName")
        }

        // Optional: Add logic here to check if the quiz is complete

    }

}
