package com.matejdro.catapult.tasklist.ui.task

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.matejdro.catapult.common.logging.ActionLogger
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.api.DirectoryListRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.mapData
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.CoroutineScopedService

@Stable
@Inject
@ContributesScopedService
class TaskListViewModel(
   private val resources: CoroutineResourceManager,
   private val directoryRepo: DirectoryListRepository,
   private val actionLogger: ActionLogger,
) : CoroutineScopedService(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<TaskListState>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<TaskListState>> = _uiState

   private var selectedDirectory: Int? = null

   fun load(id: Int) {
      actionLogger.logAction { "TaskListViewModel.load(id = $id)" }
      if (selectedDirectory == id) return
      selectedDirectory = id

      resources.launchResourceControlTask(_uiState) {
         emitAll(
            directoryRepo.getSingle(id).map { outcome ->
               outcome.mapData { TaskListState(it) }
            }
         )
      }
   }
}

@Immutable
data class TaskListState(
   val directory: CatapultDirectory,
)
