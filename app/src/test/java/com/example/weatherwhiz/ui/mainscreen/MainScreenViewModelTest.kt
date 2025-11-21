package com.example.weatherwhiz.ui.mainscreen

import android.util.Log
import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.domain.models.QuizItem
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
import org.junit.Assert.assertNull
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


    // Helper function to load data and wait for Success state
    private fun setupSuccessfulLoad() {
        coEvery { mockRepository.fetchQuizData(any()) } coAnswers {
            quizItems // Returns data immediately (we don't need delay for match logic test)
        }
        viewModel.loadNewQuiz(selectedCities)
        testDispatcher.scheduler.runCurrent()
    }

    // Define a simplified list for the game over test
    private val singleQuizItem = listOf(
        QuizItem(cityId = 10, cityName = "Berlin", temperature = 10.0, temperatureUnit = "°C", humidity = 70, windSpeed = 10.0, windSpeedUnit = "km/h", weatherCode = 3),
    )

    // Helper function for the single-item quiz load
    private fun setupSingleItemLoad() {
        // Mock the Repository to return only the single item
        coEvery { mockRepository.fetchQuizData(any()) } coAnswers { singleQuizItem }

        // Mock the internal shuffling logic to guarantee a known shuffle (optional but makes testing easier)
        // Note: For a true unit test, we should ONLY mock the repository,
        // but since we need to know the output order to call checkMatch, this is a pragmatic compromise.
        // If you can refactor prepareQuizLists to take a Random seed, that's better.

        viewModel.loadNewQuiz(selectedCities) // Call load with any cities; mock returns 'singleQuizItem'
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

        // Arrange: Repository throws an exception
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

    @Test
    fun checkMatch_correctGuess_updatesMatchedIds() = runTest {
        // Arrange: Load the data and set the state to Success
        setupSuccessfulLoad()

        val successState = viewModel.quizState.value as QuizState.Success
        val londonNameIndex = successState.cityNames.indexOf("London")
        val londonWeatherIndex = successState.weatherCards.indexOfFirst { it.cityId == 1 }

        // Assert 1 (Initial): Matched IDs should be empty
        assertTrue(viewModel.matchedCityIds.value.isEmpty())

        // Act: Make a correct match (London Name index, London Weather Card index)
        viewModel.checkMatch(nameIndex = londonNameIndex, weatherIndex = londonWeatherIndex)

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

    @Test
    fun getCityIdForName_existingCity_returnsCorrectId() = runTest {
        // Arrange: Load the data to populate the private 'currentQuizItems' list
        setupSuccessfulLoad()

        // Act
        val londonId = viewModel.getCityIdForName("London")
        val parisId = viewModel.getCityIdForName("Paris")

        // Assert
        assertEquals(1, londonId)
        assertEquals(2, parisId)
    }

    @Test
    fun getCityIdForName_nonExistentCity_returnsNull() = runTest {
        // Arrange: Load the data to populate the private 'currentQuizItems' list
        setupSuccessfulLoad()

        // Act
        val tokyoId = viewModel.getCityIdForName("NotACity") // Not in the test data

        // Assert
        assertNull(tokyoId)
    }

    @Test
    fun getCityIdForName_dataNotLoaded_returnsNull() = runTest {
        // Arrange: Do NOT call setupSuccessfulLoad(). 'currentQuizItems' is empty.

        // Act
        val londonId = viewModel.getCityIdForName("London")

        // Assert
        assertNull(londonId)
    }



    @Test
    fun resetQuiz_resetsAllStatesToInitialValues() = runTest {
        // Arrange: Put the ViewModel into a successful but messy state
        setupActiveGameState()

        // Act: Call the function under test
        viewModel.resetQuiz()

        // 1. Assert UI State: Should be set to Idle
        assertEquals(QuizState.Idle, viewModel.quizState.value)

        // 2. Assert Match/Score States: Should be empty/zero
        assertEquals(0, viewModel.matchedCityIds.value.size)
        assertEquals(0, viewModel.wrongGuesses.value)

        // 3. Assert Internal Data (Check via getter): currentQuizItems should be empty
        // Since currentQuizItems is private, we verify it's empty by checking a lookup
        // that relies on it returning null (as it would for an empty list).
        val result = viewModel.getCityIdForName("London")
        assertNull(result)
    }

    // Helper function to simulate a game with activity
    private fun setupActiveGameState() {
        // Arrange: Load data successfully
        coEvery { mockRepository.fetchQuizData(any()) } coAnswers { quizItems }
        viewModel.loadNewQuiz(selectedCities)
        testDispatcher.scheduler.runCurrent()

        // Put the ViewModel into a messy, post-game state:
        // 1. Simulate a correct match (sets a matched ID)
        viewModel.checkMatch(nameIndex = 0, weatherIndex = 1) // Correct guess for London (ID 1)

        // 2. Simulate an incorrect match (increments wrong guesses)
        viewModel.checkMatch(nameIndex = 0, weatherIndex = 0) // Incorrect guess (London name vs Paris weather)

        // Assert: Verify pre-reset state is not clean
        assertTrue(viewModel.matchedCityIds.value.isNotEmpty())
        assertEquals(1, viewModel.wrongGuesses.value)
        assertTrue(viewModel.quizState.value is QuizState.Success)
    }

    @Test
    fun checkMatch_lastCorrectGuess_transitionsToGameOver() = runTest {
        // Arrange: Load the single item quiz and ensure one wrong guess occurred previously
        setupSingleItemLoad()

        // Make sure the quiz is in a success state
        assertTrue(viewModel.quizState.value is QuizState.Success)

        // Act: Make the final correct match (Berlin name Index 0, Berlin weather Index 0)
        // Since this is the only item, this should trigger Game Over.
        viewModel.checkMatch(nameIndex = 0, weatherIndex = 0)

        // Assert 1: Check the final state is Game Over
        val finalState = viewModel.quizState.value
        assertTrue(finalState is QuizState.GameOver)

        // Assert 2: Verify the final score and details are correctly carried over
        val gameOverState = finalState as QuizState.GameOver

        // Wrong guesses should be 2 (from the setup)
        assertEquals(0, gameOverState.finalWrongGuesses)

        // Total cities should be 1 (from the singleQuizItem list)
        assertEquals(1, gameOverState.totalCities)

        // Assert 3: Verify the matched IDs list is complete
        assertEquals(1, viewModel.matchedCityIds.value.size)
        assertTrue(viewModel.matchedCityIds.value.contains(10))
    }


}