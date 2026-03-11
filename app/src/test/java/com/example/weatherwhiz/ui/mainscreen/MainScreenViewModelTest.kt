package com.example.weatherwhiz.ui.mainscreen

import android.util.Log
import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.domain.Resource
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
            Resource.Success(quizItems)
        }
        viewModel.loadNewQuiz(selectedCities)
        testDispatcher.scheduler.runCurrent()
    }

    private val singleQuizItem = listOf(
        QuizItem(cityId = 10, cityName = "Berlin", temperature = 10.0, temperatureUnit = "°C", humidity = 70, windSpeed = 10.0, windSpeedUnit = "km/h", weatherCode = 3),
    )
    private val twoQuizItem = listOf(
        QuizItem(cityId = 10, cityName = "Berlin", temperature = 10.0, temperatureUnit = "°C", humidity = 70, windSpeed = 10.0, windSpeedUnit = "km/h", weatherCode = 3),
        QuizItem(cityId = 20, cityName = "Paris", temperature = 20.0, temperatureUnit = "°C", humidity = 60, windSpeed = 5.0, windSpeedUnit = "km/h", weatherCode = 1)
    )

    // Helper function for the single-item quiz load
    private fun setupSingleItemLoad() {
        coEvery { mockRepository.fetchQuizData(any()) } coAnswers { Resource.Success(singleQuizItem) }

        val selectedCities = listOf(
            CityEntity(id = 10, name = "Berlin", latitude = 52.5, longitude = 13.0, isUserAdded = true)
        )

        viewModel.loadNewQuiz( selectedCities )

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
        coEvery { mockRepository.fetchQuizData(selectedCities) } coAnswers  {
            delay(1)
            Resource.Success(quizItems)
        }

        assertEquals(QuizState.Idle, viewModel.quizState.value)

        viewModel.loadNewQuiz(selectedCities)

        testDispatcher.scheduler.runCurrent()

        assertEquals(QuizState.Loading, viewModel.quizState.first())

        advanceUntilIdle()

        val finalState = viewModel.quizState.value

        assertTrue(finalState is QuizState.Success)

        coVerify(exactly = 1) { mockRepository.fetchQuizData(selectedCities) }

        val successState = finalState as QuizState.Success
        assertEquals(2, successState.cityNames.size)
        assertTrue(successState.cityNames.contains("London"))
        assertTrue(successState.weatherCards.map { it.cityId }.containsAll(listOf(1, 2)))
    }

    @Test
    fun loadNewQuiz_repositoryThrowsException_emitsLoadingThenError() = runTest {

        coEvery { mockRepository.fetchQuizData(selectedCities) } coAnswers{
            delay(1)
            Resource.Error("An error occurred while preparing the quiz.")
        }

        viewModel.loadNewQuiz(selectedCities)

        testDispatcher.scheduler.runCurrent()

        assertEquals(QuizState.Loading, viewModel.quizState.first())

        advanceUntilIdle()

        val finalState = viewModel.quizState.value
        assertTrue(finalState is QuizState.Error)
        assertEquals("An error occurred while preparing the quiz.", (finalState as QuizState.Error).message)
    }

    @Test
    fun loadNewQuiz_emptyData_emitsError() = runTest {
        coEvery { mockRepository.fetchQuizData(selectedCities) } coAnswers {
            delay(1)
            Resource.Error("Could not fetch data for any selected cities.")
        }

        viewModel.loadNewQuiz(selectedCities)

        testDispatcher.scheduler.runCurrent()

        assertEquals(QuizState.Loading, viewModel.quizState.first())

        advanceUntilIdle()

        val finalState = viewModel.quizState.value
        assertTrue(finalState is QuizState.Error)
        assertEquals("Could not fetch data for any selected cities.", (finalState as QuizState.Error).message)
    }

    @Test
    fun checkMatch_correctGuess_updatesMatchedIds() = runTest {
        setupSuccessfulLoad()

        val successState = viewModel.quizState.value as QuizState.Success
        val londonNameIndex = successState.cityNames.indexOf("London")
        val londonWeatherIndex = successState.weatherCards.indexOfFirst { it.cityId == 1 }

        assertTrue(viewModel.matchedCityIds.value.isEmpty())

        viewModel.checkMatch(nameIndex = londonNameIndex, weatherIndex = londonWeatherIndex)

        assertEquals(1, viewModel.matchedCityIds.value.size)
        assertTrue(viewModel.matchedCityIds.value.contains(1))
    }

    @Test
    fun checkMatch_incorrectGuess_doesNotUpdateMatchedIds() = runTest {

        setupSuccessfulLoad()

        val successState = viewModel.quizState.value as QuizState.Success

        val londonNameIndex = successState.cityNames.indexOf("London")

        val parisWeatherIndex = successState.weatherCards.indexOfFirst { it.cityId == 2 }

        assertTrue(viewModel.matchedCityIds.value.isEmpty())

        viewModel.checkMatch(
            nameIndex = londonNameIndex,
            weatherIndex = parisWeatherIndex
        )

        assertTrue(viewModel.matchedCityIds.value.isEmpty())
    }

    @Test
    fun getCityIdForName_existingCity_returnsCorrectId() = runTest {
        setupSuccessfulLoad()

        val londonId = viewModel.getCityIdForName("London")
        val parisId = viewModel.getCityIdForName("Paris")

        assertEquals(1, londonId)
        assertEquals(2, parisId)
    }

    @Test
    fun getCityIdForName_nonExistentCity_returnsNull() = runTest {
        setupSuccessfulLoad()

        val tokyoId = viewModel.getCityIdForName("NotACity")

        assertNull(tokyoId)
    }

    @Test
    fun getCityIdForName_dataNotLoaded_returnsNull() = runTest {
        // Do NOT call setupSuccessfulLoad(). 'currentQuizItems' is empty.

        val londonId = viewModel.getCityIdForName("London")

        assertNull(londonId)
    }



    @Test
    fun resetQuiz_resetsAllStatesToInitialValues() = runTest {
        setupActiveGameState()

        viewModel.resetQuiz()

        assertEquals(QuizState.Idle, viewModel.quizState.value)

        assertEquals(0, viewModel.matchedCityIds.value.size)
        assertEquals(0, viewModel.wrongGuesses.value)

        val result = viewModel.getCityIdForName("London")
        assertNull(result)
    }

    private fun setupActiveGameState() {
        coEvery { mockRepository.fetchQuizData(any()) } coAnswers { Resource.Success(twoQuizItem) }
        viewModel.loadNewQuiz(selectedCities)
        testDispatcher.scheduler.runCurrent()

        viewModel.checkMatch(nameIndex = 0, weatherIndex = 1) // Correct guess for London (ID 1)

        viewModel.checkMatch(nameIndex = 0, weatherIndex = 0) // Incorrect guess (London name vs Paris weather)

        assertTrue(viewModel.matchedCityIds.value.isNotEmpty())
        assertEquals(1, viewModel.wrongGuesses.value)
        assertTrue(viewModel.quizState.value is QuizState.Success)
    }

    @Test
    fun checkMatch_lastCorrectGuess_transitionsToGameOver() = runTest {
        setupSingleItemLoad()

        assertTrue(viewModel.quizState.value is QuizState.Success)

        viewModel.checkMatch(nameIndex = 0, weatherIndex = 0)

        val finalState = viewModel.quizState.value
        assertTrue(finalState is QuizState.GameOver)

        val gameOverState = finalState as QuizState.GameOver

        assertEquals(0, gameOverState.finalWrongGuesses)

        assertEquals(1, gameOverState.totalCities)

        assertEquals(1, viewModel.matchedCityIds.value.size)
        assertTrue(viewModel.matchedCityIds.value.contains(10))
    }

}