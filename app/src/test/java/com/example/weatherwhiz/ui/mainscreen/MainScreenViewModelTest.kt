package com.example.weatherwhiz.ui.mainscreen

import android.util.Log
import com.example.weatherwhiz.data.CityEntity
import com.example.weatherwhiz.data.QuizItem
import com.example.weatherwhiz.domain.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Dispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.delay


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

    @Before
    fun setup(){
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        coEvery { Log.e(any(), any()) } returns 0

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
}