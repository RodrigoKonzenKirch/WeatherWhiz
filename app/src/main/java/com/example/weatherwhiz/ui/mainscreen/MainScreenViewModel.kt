package com.example.weatherwhiz.ui.mainscreen

import androidx.lifecycle.ViewModel
import com.example.weatherwhiz.domain.CityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val cityRepository: CityRepository,
//    private val weatherRepository: WeatherRepository
): ViewModel() {


}