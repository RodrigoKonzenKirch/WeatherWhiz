package com.example.weatherwhiz.ui.mainscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.domain.Resource
import com.example.weatherwhiz.domain.models.QuizItem
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

    private val _wrongGuesses = MutableStateFlow(0)
    val wrongGuesses: StateFlow<Int> = _wrongGuesses.asStateFlow()
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

    fun loadNewQuiz(selectedCities: List<CityEntity>) {
        viewModelScope.launch {
            _quizState.value = QuizState.Loading

            when (val result = weatherRepository.fetchQuizData(selectedCities)) {
                is Resource.Success -> {
                    val allQuizItems = result.data
                    currentQuizItems = allQuizItems

                    val (shuffledNames, shuffledWeatherCards) = prepareQuizLists(allQuizItems)

                    _quizState.value = QuizState.Success(
                        cityNames = shuffledNames,
                        weatherCards = shuffledWeatherCards
                    )
                }
                is Resource.Error -> {
                    Log.e("QuizViewModel", "Error loading quiz: ${result.message}")
                    _quizState.value = QuizState.Error(result.message)
                }
                is Resource.Loading -> {
                    _quizState.value = QuizState.Loading
                }
            }
        }
    }

    private fun prepareQuizLists(items: List<QuizItem>): Pair<List<String>, List<WeatherCard>> {
        val cityNames = items.map { it.cityName }.shuffled()

        val weatherCards = items.map { item ->
            WeatherCard(
                cityId = item.cityId,
                temperature = item.temperature,
                weatherCode = item.weatherCode
            )
        }.shuffled()

        return Pair(cityNames, weatherCards)
    }

    fun getCityIdForName(cityName: String): Int? {
        return currentQuizItems.firstOrNull { it.cityName == cityName }?.cityId
    }

    fun checkMatch(nameIndex: Int, weatherIndex: Int) {
        val currentState = _quizState.value
        if (currentState !is QuizState.Success) {
            return
        }

        val selectedCityName = currentState.cityNames[nameIndex]
        val selectedWeatherCard = currentState.weatherCards[weatherIndex]

        val correctNameItem = currentQuizItems.firstOrNull { it.cityName == selectedCityName }

        val isCorrect = correctNameItem?.cityId == selectedWeatherCard.cityId

        if (isCorrect) {
            _matchedCityIds.value += selectedWeatherCard.cityId

            if (_matchedCityIds.value.size == currentQuizItems.size) {
                _quizState.value = QuizState.GameOver(
                    finalWrongGuesses = _wrongGuesses.value,
                    totalCities = currentQuizItems.size
                )
            }
        } else {
            _wrongGuesses.value += 1
            Log.d("QuizViewModel", "Incorrect match for $selectedCityName")
        }
    }

    fun resetQuiz() {
        currentQuizItems = emptyList()
        _matchedCityIds.value = emptySet()
        _wrongGuesses.value = 0
        _quizState.value = QuizState.Idle
    }
}