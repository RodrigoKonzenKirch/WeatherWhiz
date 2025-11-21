package com.example.weatherwhiz.ui.mainscreen

import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.weatherwhiz.MainActivity
import com.example.weatherwhiz.R
import com.example.weatherwhiz.data.CityEntity
import com.example.weatherwhiz.ui.theme.WeatherWhizTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainScreenTest{

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Mock the actual ViewModel class
    private val mockViewModel = mockk<MainScreenViewModel>(relaxed = true)

    // Create controllable MutableStateFlows for the test
    private val quizStateFlow = MutableStateFlow<QuizState>(QuizState.Loading)
    private val wrongGuessesFlow = MutableStateFlow<Int>(0)
    private val cityNamesFlow = MutableStateFlow<List<CityEntity>>(emptyList())
    private val matchedCityIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    // Test data
    val cityNamesList = listOf("CityA", "CityB")
    val weatherCardsList = listOf(WeatherCard(0,10.0,0), WeatherCard(0, 20.0, 1))


    @Before
    fun setUp() {
        hiltRule.inject()

        every { mockViewModel.quizState } returns quizStateFlow
        every { mockViewModel.wrongGuesses } returns wrongGuessesFlow
        every { mockViewModel.cities} returns cityNamesFlow
        every { mockViewModel.matchedCityIds } returns matchedCityIdsFlow

    }

    @Test
    fun mainScreen_onIdleState_displaysIdleViewAndInitialScreenContent() {
        // Arrange:
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // Define expected values
        val iconContentDescription = composeTestRule.activity.getString(R.string.idle_screen_description_weather_whiz_logo)
        val welcomeMessage = composeTestRule.activity.getString(R.string.idle_screen_welcome_to_weather_whiz)
        val idleMessage = composeTestRule.activity.getString(R.string.idle_screen_explanation_message)
        val idleButtonMessage = composeTestRule.activity.getString(R.string.idle_screen_button_start_quiz)

        // Set initial state to Idle
        quizStateFlow.value = QuizState.Idle

        // Assert: IdleScreen components are displayed
        composeTestRule.onNodeWithContentDescription(iconContentDescription)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(welcomeMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(idleMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(idleButtonMessage)
            .assertIsDisplayed()

    }

    @Test
    fun mainScreen_onErrorState_displaysErrorViewWithMessageAndRetryButton() {
        // Arrange:
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // Define expected values
        val errorMessage = composeTestRule.activity.getString(R.string.error_screen_failed_to_load_error_message)
        val iconContentDescription = composeTestRule.activity.getString(R.string.error_screen_Icon_error)
        val retryButtonMessage = composeTestRule.activity.getString(R.string.error_view_button_try_again)
        val detailedErrorMessage = "This is an error message"
        val detailedErrorMessageTag = composeTestRule.activity.getString(R.string.error_screen_test_tag_detailed_error_message)

        // Set initial state to Error
        quizStateFlow.value = QuizState.Error(detailedErrorMessage)

        // Assert: ErrorScreen components are displayed
        composeTestRule.onNodeWithText(errorMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(iconContentDescription)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(retryButtonMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(detailedErrorMessageTag)
            .assertIsDisplayed()

    }

    @Test
    fun mainScreen_onLoadingState_displaysLoadingViewAndCorrectScore() {
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // Arrange: Define expected values
        val expectedLoadingMessage = composeTestRule.activity.getString(R.string.loading_screen_fetching_global_forecasts )
        val topBarWrongGuessMessage = composeTestRule.activity.getString(R.string.top_bar_wrong_guesses)

        // Set initial state to Loading
        quizStateFlow.value = QuizState.Loading

        // Assert 1: The LoadingView should be displayed
        // We look for a unique piece of text from the LoadingView
        composeTestRule.onNodeWithText(expectedLoadingMessage)
            .assertIsDisplayed()

        // Assert 2: The TopBar should show the correct wrong guesses count
        composeTestRule.onNodeWithContentDescription(topBarWrongGuessMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("0")
            .assertIsDisplayed()
    }


    @Test
    fun mainScreen_transitionsFromLoadingToSuccess() {
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // Arrange: Define expected value
        val expectedLoadingMessage = composeTestRule.activity.getString(R.string.loading_screen_fetching_global_forecasts )

        // Assert A: Loading is visible
        composeTestRule.onNodeWithText(expectedLoadingMessage)
            .assertIsDisplayed()

        // Act: Change the state to Success (simulating data finished loading)
        val successData = QuizState.Success(
            cityNames = cityNamesList,
            weatherCards = weatherCardsList
        )
        quizStateFlow.value = successData

        // Assert B: Loading view is gone
        composeTestRule.onNodeWithText(expectedLoadingMessage)
            .assertDoesNotExist()

        // Assert C: SuccessView (Quiz Content) is displayed (check for a unique element)
        composeTestRule.onNodeWithText("CityA")
            .assertIsDisplayed()
    }

    @Test
    fun onGameOverState_displaysGameOverView_withWrongGuesses() {
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // Arrange: Define expected values
        val gameOverIconDescription = composeTestRule.activity.getString(R.string.game_over_screen_icon)
        val gameOverMessage = composeTestRule.activity.getString(R.string.game_over_quiz_complete_message)
        val zeroWrongGuessesMessage = composeTestRule.activity.getString(R.string.game_over_zero_wrong_guesses)
        val restartButtonMessage = composeTestRule.activity.getString(R.string.game_over_screen_button_play_new_game)
        val scoreSummaryMessage = composeTestRule.activity.getString(R.string.game_over_screen_test_tag_score_summary)

        // Set state to gameOver
        quizStateFlow.value = QuizState.GameOver(1, 1)

        composeTestRule.onNodeWithContentDescription(gameOverIconDescription)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(gameOverMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(restartButtonMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(scoreSummaryMessage)
            .assertIsDisplayed()
        // Message that should not be displayed when wrong guesses are more than 0
        composeTestRule.onNodeWithText(zeroWrongGuessesMessage)
            .assertIsNotDisplayed()

    }

    @Test
    fun onGameOverState_displaysGameOverView_zeroWrongGuesses() {
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // Arrange: Define expected values
        val gameOverIconDescription = composeTestRule.activity.getString(R.string.game_over_screen_icon)
        val gameOverMessage = composeTestRule.activity.getString(R.string.game_over_quiz_complete_message)
        val zeroWrongGuessesMessage = composeTestRule.activity.getString(R.string.game_over_zero_wrong_guesses)
        val restartButtonMessage = composeTestRule.activity.getString(R.string.game_over_screen_button_play_new_game)
        val scoreSummaryMessage = composeTestRule.activity.getString(R.string.game_over_screen_test_tag_score_summary)

        // Set state to gameOver
        quizStateFlow.value = QuizState.GameOver(0, 1)

        // Assert: GameOverView components are displayed
        composeTestRule.onNodeWithContentDescription(gameOverIconDescription)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(gameOverMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(restartButtonMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(scoreSummaryMessage)
            .assertIsDisplayed()
        // Message should be displayed when wrong guesses are 0
        composeTestRule.onNodeWithText(zeroWrongGuessesMessage)
            .assertIsDisplayed()

    }

    @Test
    fun onSuccessState_displaysSuccessView_withWeatherCards() {
        composeTestRule.activity.setContent {
            WeatherWhizTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = mockViewModel
                )
            }
        }

        // set state to Success
        quizStateFlow.value = QuizState.Success( cityNamesList, weatherCardsList )

        // Assert succesScreen are displayed
        composeTestRule.onNodeWithText(cityNamesList[0])
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(weatherCardsList[0].temperature.toString())

    }

}
