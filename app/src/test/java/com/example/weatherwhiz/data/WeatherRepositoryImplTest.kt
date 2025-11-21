package com.example.weatherwhiz.data

import com.example.weatherwhiz.data.local.CityDao
import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.data.remote.WeatherApiService
import com.example.weatherwhiz.data.remote.WeatherResponse
import com.example.weatherwhiz.data.remote.CurrentUnits
import com.example.weatherwhiz.data.remote.CurrentWeather
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class WeatherRepositoryImplTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var mockApiService: WeatherApiService

    @MockK(relaxed = true)
    private lateinit var mockCityDao: CityDao

    private lateinit var repository: WeatherRepositoryImpl

    private val city1 =
        CityEntity(id = 1, name = "London", latitude = 51.5, longitude = 0.1, isUserAdded = true)
    private val city2 =
        CityEntity(id = 2, name = "Paris", latitude = 48.8, longitude = 2.3, isUserAdded = true)
    private val cityList = listOf(city1, city2)

    private val response1 = WeatherResponse(
        current_units = CurrentUnits("°C", "km/h", "%"),
        current = CurrentWeather("...", 1, 15.0, 70, 3, 10.0)
    )
    private val response2 = WeatherResponse(
        current_units = CurrentUnits("°C", "km/h", "%"),
        current = CurrentWeather("...", 1, 20.0, 60, 1, 5.0)
    )

    @Before
    fun setUp() {
        repository = WeatherRepositoryImpl(mockApiService, mockCityDao)
    }

    @Test
    fun fetchQuizData_success_returnsAllQuizItems() = runTest {
        // Arrange: Define the behavior of the mocked API service
        coEvery { mockApiService.getCurrentWeather(city1.latitude, city1.longitude) } returns response1
        coEvery { mockApiService.getCurrentWeather(city2.latitude, city2.longitude) } returns response2

        // Act
        val result = repository.fetchQuizData(cityList)

        // Assert
        assertThat(result.size).isEqualTo(2) // Expect 2 items back
        assertThat(result[0].cityName).isEqualTo(city1.name)
        assertThat(result[0].temperature).isEqualTo(15.0) // Allow for rounding errors
    }

    @Test
    fun fetchQuizData_partialFailure_returnsSuccessfulItemsOnly() = runTest {
        // Arrange: Set up one call to succeed and one to throw an exception
        coEvery { mockApiService.getCurrentWeather(city1.latitude, city1.longitude) } returns response1
        // Mock the second call to throw a network exception
        coEvery { mockApiService.getCurrentWeather(city2.latitude, city2.longitude) } throws Exception("Network Error")

        // Act
        val result = repository.fetchQuizData(cityList)

        // Assert
        assertThat(result.size).isEqualTo(1) // Only the successful city (London) should be returned
        assertThat(result[0].cityName).isEqualTo(city1.name)

        assertThat(result[0].temperature).isEqualTo(15.0) // Allow for rounding errors
    }

    @Test
    fun fetchQuizData_emptyList_returnsEmptyList() = runTest {
        // Arrange: Mock the API call just in case, though it shouldn't be called
        coEvery { mockApiService.getCurrentWeather(any(), any()) } returns response1

        // Act
        val result = repository.fetchQuizData(emptyList())

        // Assert
        assertThat(result.size).isEqualTo(0) // Expect an empty list
    }

    @Test
    fun getAllCities_emitsDaoData() = runTest {
        // Arrange: Define the behavior of the mocked DAO.
        // It must return a Kotlin Flow containing the list of cities.
        every { mockCityDao.getAllCities() } returns flowOf(cityList)

        // Act: Collect the Flow provided by the Repository.
        // 'first()' collects the first emission and cancels the Flow.
        val result = repository.getAllCities().first()

        // Assert: Verify that the Repository returned the exact list of cities
        // provided by the mocked DAO.
        assertThat(result.size).isEqualTo(cityList.size)
        assertThat(result).isEqualTo(cityList)
    }

    @Test
    fun getAllCities_emptyList_returnsEmptyFlow() = runTest {
        // Arrange: Mock the DAO to return an empty Flow.
        every { mockCityDao.getAllCities() } returns flowOf(emptyList())

        // Act
        val result = repository.getAllCities().first()

        // Assert: The repository should correctly pass through the empty list.
        assertThat(result.size).isEqualTo(0)
        assertThat(result).isEqualTo(emptyList<CityEntity>())
    }

}