package com.matejdro.catapult

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matejdro.catapult.navigation.keys.DirectoryListKey
import com.matejdro.catapult.navigation.keys.HomeScreenKey
import com.matejdro.catapult.navigation.keys.OnboardingKey
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@AssistedInject
class MainViewModel(
   private val preferences: DataStore<Preferences>,
) : ViewModel() {
   private val _startingScreens = MutableStateFlow<List<ScreenKey>?>(null)
   val startingScreens: StateFlow<List<ScreenKey>?> = _startingScreens

   init {
      viewModelScope.launch {
         _startingScreens.value = if (preferences.data.first()[onboardingShown] == true) {
            listOf(HomeScreenKey, DirectoryListKey())
         } else {
            listOf(OnboardingKey)
         }

         preferences.edit {
            it[onboardingShown] = true
         }
      }
   }

   @AssistedFactory
   fun interface Factory {
      fun create(): MainViewModel
   }
}

private val onboardingShown = booleanPreferencesKey("onboarding_shown")
