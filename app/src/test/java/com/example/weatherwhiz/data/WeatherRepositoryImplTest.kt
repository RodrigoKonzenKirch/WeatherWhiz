package com.example.weatherwhiz.data

import com.example.weatherwhiz.data.local.CityDao
import com.example.weatherwhiz.data.local.CityEntity
import com.example.weatherwhiz.data.remote.WeatherApiService
import com.example.weatherwhiz.data.remote.WeatherResponse
import com.example.weatherwhiz.data.remote.CurrentUnits
import com.example.weatherwhiz.data.remote.CurrentWeather
import com.example.weatherwhiz.domain.Resource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException


class WeatherRepositoryImplTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var mockApiService: WeatherApiService

    @MockK(relaxed = true)
    private lateinit var mockCityDao: CityDao

    private lateinit var repository: WeatherRepositoryImpl

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ioDispatcher = UnconfinedTestDispatcher()

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
        repository = WeatherRepositoryImpl(mockApiService, mockCityDao, ioDispatcher)
    }

    @Test
    fun fetchQuizData_success_returnsAllQuizItems() = runTest {
        coEvery { mockApiService.getCurrentWeather(city1.latitude, city1.longitude) } returns response1
        coEvery { mockApiService.getCurrentWeather(city2.latitude, city2.longitude) } returns response2

        val result = repository.fetchQuizData(cityList)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data.size).isEqualTo(cityList.size)
    }

    @Test
    fun fetchQuizData_allCitiesFail_returnsError() = runTest {
        coEvery { mockApiService.getCurrentWeather(any(), any()) } throws IOException("No internet")

        val result = repository.fetchQuizData(cityList)

        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).message).contains("Network error")
    }

    @Test
    fun fetchQuizData_networkError_returnsNetworkErrorMessage() = runTest {
        coEvery { mockApiService.getCurrentWeather(any(), any()) } throws IOException()

        val result = repository.fetchQuizData(listOf(city1))

        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).message).contains("Network error")
    }

    @Test
    fun fetchQuizData_httpError_returnsServerErrorMessage() = runTest {
        val errorResponse = Response.error<Any>(500, "".toResponseBody(null))
        val httpException = HttpException(errorResponse)
        coEvery { mockApiService.getCurrentWeather(any(), any()) } throws httpException

        val result = repository.fetchQuizData(listOf(city1))

        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).message).contains("Server error")
    }

    @Test
    fun fetchQuizData_emptyList_returnsEmptyList() = runTest {
        val result = repository.fetchQuizData(emptyList())

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data.size).isEqualTo(0)
    }

    @Test
    fun getAllCities_emitsDaoData() = runTest {
        every { mockCityDao.getAllCities() } returns flowOf(cityList)

        val result = repository.getAllCities().first()

        assertThat(result.size).isEqualTo(cityList.size)
        assertThat(result).isEqualTo(cityList)
    }
}