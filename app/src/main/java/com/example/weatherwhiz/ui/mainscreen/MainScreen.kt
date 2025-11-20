package com.example.weatherwhiz.ui.mainscreen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val cities by viewModel.cities.collectAsState()
    val state = viewModel.quizState.collectAsStateWithLifecycle()
    val defaultCities = cities

    Scaffold { paddingValues ->
        Box(modifier.padding(paddingValues).fillMaxSize()) {
            when (state.value){
                is QuizState.Idle -> IdleView(
                    onStartQuizClicked = { viewModel.loadNewQuiz(defaultCities) }
                )
                is QuizState.Loading -> LoadingView()
                is QuizState.Error -> ErrorView(
                    message = (state.value as QuizState.Error).message,
                    onRetryClicked = { viewModel.loadNewQuiz(defaultCities) }
                )
                is QuizState.Success -> SuccessView(
                    successState = state.value as QuizState.Success,
                    viewModel = viewModel
                )



            }
        }

    }
}

@Composable
private fun IdleView(
    onStartQuizClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = "Weather Whiz Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Weather Whiz!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Test your global weather knowledge by matching cities to their current conditions.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // This button triggers the ViewModel to start fetching data
        Button(
            onClick = onStartQuizClicked,
            modifier = Modifier.fillMaxWidth(0.6f),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text("Start Quiz", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Fetching global forecasts...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Quiz failed to load.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Details: $message",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Button to retry the loading process
        Button(
            onClick = onRetryClicked,
            modifier = Modifier.fillMaxWidth(0.6f),
            contentPadding = PaddingValues(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Try Again", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SuccessView(
    modifier: Modifier = Modifier,
    successState: QuizState.Success,
    viewModel: MainScreenViewModel
) {

    val matchedIds by viewModel.matchedCityIds.collectAsStateWithLifecycle() // Collect the set of IDs
    // State to track the user's current selection
    var selectedCityNameIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedWeatherCardIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // Logic: Combine the two lists side-by-side
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // --- LEFT COLUMN: City Names ---
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(successState.cityNames) { index, cityName ->
                val cityId = viewModel.getCityIdForName(cityName)
                val isMatched = cityId != null && matchedIds.contains(cityId)

                CityNameCard(
                    cityName = cityName,
                    isSelected = index == selectedCityNameIndex,
                    isMatched = isMatched,
                    isEnable = !isMatched,
                    onClick = {
                        selectedCityNameIndex = index
                        // Check if a match is ready to be processed
                        if (selectedWeatherCardIndex != null) {
                            // Call ViewModel to check the match
                            viewModel.checkMatch(selectedCityNameIndex!!, selectedWeatherCardIndex!!)
                            // Reset selection states after check
                            selectedCityNameIndex = null
                            selectedWeatherCardIndex = null
                        }
                    }
                )
            }
        }

        // --- RIGHT COLUMN: Weather Cards ---
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(successState.weatherCards) { index, weatherCard ->
                val isMatched = matchedIds.contains(weatherCard.cityId)

                WeatherCardComposable(
                    card = weatherCard,
                    isSelected = index == selectedWeatherCardIndex,
                    // Pass current match status for visual feedback (e.g., Green/Red border)
                    isMatched = isMatched,
                    isEnable = !isMatched,
                    onClick = {
                        selectedWeatherCardIndex = index
                        if (selectedCityNameIndex != null) {
                            viewModel.checkMatch(selectedCityNameIndex!!, selectedWeatherCardIndex!!)
                            selectedCityNameIndex = null
                            selectedWeatherCardIndex = null
                        }
                    }
                )
            }
        }
    }

}

@Composable
fun CityNameCard(
    cityName: String,
    isSelected: Boolean,
    isMatched: Boolean,
    isEnable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Define the color based on the state
    val backgroundColor = when {
        isMatched -> MaterialTheme.colorScheme.tertiaryContainer // Green/Success color
        isSelected -> MaterialTheme.colorScheme.primaryContainer // Highlight selected
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // Define the border based on the state
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                onClick = onClick,
                enabled = isEnable
            )
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun WeatherCardComposable(
    card: WeatherCard,
    isSelected: Boolean,
    isMatched: Boolean,
    isEnable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Define the colors and border based on state (same logic as CityNameCard)
    val backgroundColor = when {
        isMatched -> MaterialTheme.colorScheme.tertiaryContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                enabled = isEnable
            )
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Icon and Temperature ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Data Transformation: Map WMO code to an Icon
                val weatherIcon = remember(card.weatherCode) { mapWmoCodeToIcon(card.weatherCode) }
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = null, // Content description for icon
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "${card.temperature}°C", // Assuming Celsius for display
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- Details (Humidity, Wind) ---
            Column {
                // Need to pass humidity and wind speed in WeatherCard data class
                // Text(text = "Humidity: ${card.humidity}%", style = MaterialTheme.typography.bodyMedium)
                // Text(text = "Wind: ${card.windSpeed} km/h", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = remember(card.weatherCode) { mapWmoCodeToDescription(card.weatherCode) },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Simplified mapping function (WMO codes are integers)
private fun mapWmoCodeToIcon(code: Int): ImageVector {
    return when (code) {
        in 0..1 -> Icons.Default.WbSunny // Clear sky
        in 2..3 -> Icons.Default.CloudQueue // Cloudy/Partly Cloudy
        in 51..67 -> Icons.Default.Thunderstorm // Rain/Drizzle
        in 71..75 -> Icons.Default.AcUnit // Snow/Hail
        else -> Icons.Default.CloudOff // Fallback
    }
}

// Simplified mapping function for the description
private fun mapWmoCodeToDescription(code: Int): String {
    return when (code) {
        0 -> "Clear Sky"
        1 -> "Mostly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        in 51..55 -> "Light Drizzle"
        in 61..65 -> "Moderate Rain"
        else -> "Weather Event"
    }
}