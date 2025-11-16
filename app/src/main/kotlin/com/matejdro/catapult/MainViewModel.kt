package com.matejdro.catapult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matejdro.catapult.navigation.keys.HomeScreenKey
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@AssistedInject
class MainViewModel : ViewModel() {
   private val _startingScreen = MutableStateFlow<ScreenKey?>(null)
   val startingScreen: StateFlow<ScreenKey?> = _startingScreen

   init {
      viewModelScope.launch {
         _startingScreen.value = HomeScreenKey
      }
   }

   @AssistedFactory
   fun interface Factory {
      fun create(): MainViewModel
   }
}
