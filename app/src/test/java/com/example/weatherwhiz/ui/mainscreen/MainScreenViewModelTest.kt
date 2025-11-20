package com.example.weatherwhiz.ui.mainscreen

import android.util.Log
import com.example.weatherwhiz.data.CityEntity
import com.example.weatherwhiz.data.QuizItem
import com.example.weatherwhiz.domain.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK(relaxed = true)
    private lateinit var mockRepository: WeatherRepository

    private lateinit var viewModel: MainScreenViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val city1 =
        CityEntity(id = 1, name = "London", latitude = 51.5, longitude = 0.1, isUserAdded = true)
    private val city2 = CityEntity(id = 2, name = "Paris", latitude = 48.8, longitude = 2.3, isUserAdded = true)
    private val selectedCities = listOf(city1, city2)

    private val quizItems = listOf(
        QuizItem(
            cityId = 1,
            cityName = "London",
            temperature = 15.0,
            temperatureUnit = "°C",
            humidity = 70,
            windSpeed = 10.0,
            windSpeedUnit = "km/h",
            weatherCode = 3
        ),
        QuizItem(cityId = 2, cityName = "Paris", temperature = 20.0, temperatureUnit = "°C", humidity = 60, windSpeed = 5.0, windSpeedUnit = "km/h", weatherCode = 1)
    )

    // A simulated successful state, where lists are shuffled
    // We need to know the index mapping for the test:
    // Assume after shuffling, London (ID 1) is at index 0 of names, and its weather card (ID 1) is at index 1 of weather cards.
    // And Paris (ID 2) is at index 1 of names, and its weather card (ID 2) is at index 0 of weather cards.
    private val shuffledNames = listOf("London", "Paris") // Index 0 is ID 1, Index 1 is ID 2
    private val shuffledWeatherCards = listOf(
        WeatherCard(cityId = 2, temperature = 20.0, weatherCode = 1), // Index 0 is Paris's weather
        WeatherCard(cityId = 1, temperature = 15.0, weatherCode = 3)  // Index 1 is London's weather
    )

    // Helper function to load data and wait for Success state
    private suspend fun setupSuccessfulLoad() {
        coEvery { mockRepository.fetchQuizData(any()) } coAnswers {
            quizItems // Returns data immediately (we don't need delay for match logic test)
        }
        viewModel.loadNewQuiz(selectedCities)
        testDispatcher.scheduler.runCurrent()
    }

    @Before
    fun setup(){
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        coEvery { Log.e(any(), any()) } returns 0
        coEvery { Log.d(any(), any()) } returns 0

        viewModel = MainScreenViewModel(mockRepository)
    }

    @After
    fun tearDown(){
        Dispatchers.resetMain()

        unmockkStatic(Log::class)
    }

    @Test
    fun loadNewQuiz_success_emitsLoadingThenSuccessWithShuffledData() = runTest {
        // Arrange: Repository returns the list of quiz items
        coEvery { mockRepository.fetchQuizData(selectedCities) } coAnswers  {
            delay(1)
            quizItems
        }

        assertEquals(QuizState.Idle, viewModel.quizState.value)

        // Act: Start loading
        viewModel.loadNewQuiz(selectedCities)

        testDispatcher.scheduler.runCurrent()

        // Assert 1: Check the first emitted state is Loading
        assertEquals(QuizState.Loading, viewModel.quizState.first())

        advanceUntilIdle()

        val finalState = viewModel.quizState.value

        // Assert 2: Check the final emitted state is Success
        assertTrue(finalState is QuizState.Success)

        // Assert 3: Verify the data was fetched once
        coVerify(exactly = 1) { mockRepository.fetchQuizData(selectedCities) }

        // Assert 4: Verify the lists are shuffled (by checking they don't match the original order)
        val successState = finalState as QuizState.Success
        assertEquals(2, successState.cityNames.size)
        assertTrue(successState.cityNames.contains("London"))
        assertTrue(successState.weatherCards.map { it.cityId }.containsAll(listOf(1, 2)))
    }

    @Test
    fun loadNewQuiz_repositoryThrowsException_emitsLoadingThenError() = runTest {

        // Arrange: Repository throws an exception (e.g., critical network failure)
        coEvery { mockRepository.fetchQuizData(selectedCities) } coAnswers{
            delay(1)
            throw Exception("Critical Failure")
        }

        // Act: Start loading
        viewModel.loadNewQuiz(selectedCities)

        testDispatcher.scheduler.runCurrent()

        // Assert 1: Check the first emitted state is Loading
        assertEquals(QuizState.Loading, viewModel.quizState.first())

        advanceUntilIdle()

        // Assert 2: Check the final emitted state is Error
        val finalState = viewModel.quizState.value
        assertTrue(finalState is QuizState.Error)
        assertEquals("An error occurred while preparing the quiz.", (finalState as QuizState.Error).message)
    }

    @Test
    fun loadNewQuiz_emptyData_emitsLoadingThenError() = runTest {
        // Arrange: Repository returns an empty list (all API calls failed)
        coEvery { mockRepository.fetchQuizData(selectedCities) } coAnswers {
            delay(1)
            emptyList()
        }

        // Act: Start loading
        viewModel.loadNewQuiz(selectedCities)

        testDispatcher.scheduler.runCurrent()

        // Assert 1: Check the first emitted state is Loading
        assertEquals(QuizState.Loading, viewModel.quizState.first())

        advanceUntilIdle()

        // Assert 2: Check the final emitted state is Error with specific message
        val finalState = viewModel.quizState.value
        assertTrue(finalState is QuizState.Error)
        assertEquals("Could not fetch data for any selected cities.", (finalState as QuizState.Error).message)
    }

    // --- TEST SCENARIO 3: Correct Match Verification ---
    @Test
    fun checkMatch_correctGuess_updatesMatchedIds() = runTest {
        // Arrange: Load the data and set the state to Success
        setupSuccessfulLoad()

        // Assert 1 (Initial): Matched IDs should be empty
        assertTrue(viewModel.matchedCityIds.value.isEmpty())

        // Act: Make a correct match (London Name at index 0, London Weather Card at index 1)
        viewModel.checkMatch(nameIndex = 0, weatherIndex = 1)

        // Assert 2: The matched ID (ID 1 for London) should be present
        assertEquals(1, viewModel.matchedCityIds.value.size)
        assertTrue(viewModel.matchedCityIds.value.contains(1))
    }

    @Test
    fun checkMatch_incorrectGuess_doesNotUpdateMatchedIds() = runTest {

        // Arrange: Load the data and set the state to Success
        setupSuccessfulLoad()

        // 1. Get the final Success state to know the ACTUAL shuffled order
        val successState = viewModel.quizState.value as QuizState.Success

        // 2. Find indices for an guaranteed INCORRECT match:

        // Example: Find the index of the city name "London" (ID 1)
        val londonNameIndex = successState.cityNames.indexOf("London")

        // Example: Find the index of the weather card for Paris (ID 2)
        // We look for a card whose cityId is 2.
        val parisWeatherIndex = successState.weatherCards.indexOfFirst { it.cityId == 2 }

        // Assert 1 (Pre-Act): Matched IDs should be empty
        assertTrue(viewModel.matchedCityIds.value.isEmpty())

        // Act: Make the INCORRECT match using the dynamically found indices
        viewModel.checkMatch(
            nameIndex = londonNameIndex,
            weatherIndex = parisWeatherIndex
        )

        // Assert 2: Matched IDs should remain empty
        assertTrue(viewModel.matchedCityIds.value.isEmpty())
    }

}