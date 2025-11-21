package com.example.weatherwhiz.data.remote

// Data class to map the nested 'current_units' object
// Used for displaying the correct units (e.g., °C or km/h)
data class CurrentUnits(
    val temperature_2m: String, // e.g., "°C"
    val wind_speed_10m: String, // e.g., "km/h"
    val relative_humidity_2m: String // e.g., "%"
)