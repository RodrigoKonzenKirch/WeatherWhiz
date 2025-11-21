Project: Weather Whiz - Global Weather Matching Quiz

Weather Whiz is an interactive Android application designed to test a user's geographical and meteorological knowledge. The app fetches real-time weather data for multiple cities, scrambles the results, and challenges the user to correctly match each city name to its current weather conditions.

🎯 Key Functionalities

This application showcases the full lifecycle of data handling, from network retrieval to complex UI state management and user interaction.

- Concurrent Data Fetching: Efficiently retrieves current weather data for all selected cities simultaneously, optimizing network performance.
- The Matching Quiz: Presents a core-matching challenge where city names and weather cards are displayed in two randomized, distinct lists.
- Interactive Matching: Allows users to select pairs (City Name + Weather Card) using the tap-to-select interaction model.
- State Tracking: Visually indicates successfully matched pairs and prevents further interaction with completed items, ensuring a clear and intuitive user experience.
- Error & Loading States: Provides clear feedback to the user during asynchronous operations (loading spinner) and gracefully handles network failures (retry mechanism).

💻 Technology Stack & Architecture

This project is built using modern Android development practices, demonstrating expertise in reactive programming, dependency injection, and testing.

Core Technologies

- **Jetpack Compose:** Modern, declarative UI development; uses the Unidirectional Data Flow (UDF) pattern for stable state management.
- **Retrofit & OkHttp:** Type-safe REST client for integrating with the Open-Meteo API.
- **Room Persistence Library:** Local database for storing user-added cities and default city lists.
- **Kotlin Coroutines & Flow:** Manages asynchronous operations, specifically using async/awaitAll() for parallel API calls to maximize efficiency.
- **MVVM (Model-View-ViewModel):** Clear separation of concerns, testability, and maintainability.
- **Hilt:** Simplifies dependency management and ensures clean, modular code.
- **JUnit 4 & MockK:** Comprehensive unit testing for the Repository (network logic) and ViewModel (state management), including handling asynchronous flows.

⚙️ How to Run

Clone the repository: 

    git clone https://github.com/RodrigoKonzenKirch/WeatherWhiz.git

Open the project in Android Studio.

Build and run on an Android device or emulator. No external API key is required as the Open-Meteo API is used for data fetching.
