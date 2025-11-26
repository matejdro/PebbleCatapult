package com.matejdro.catapult.tasklist.ui

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
class FolderListViewModel(
   private val directoryListRepository: DirectoryListRepository,
   private val resources: CoroutineResourceManager,
   private val actionLogger: ActionLogger,
) : CoroutineScopedService(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<FolderListState>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<FolderListState>> = _uiState

   override fun onServiceRegistered() {
      actionLogger.logAction { "FolderListViewModel.onServiceRegistered()" }

      resources.launchResourceControlTask(_uiState) {
         emitAll(
            directoryListRepository.getAllDirectories().map { outcome ->
               outcome.mapData { FolderListState(it) }
            }
         )
      }
   }

   fun add(title: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "FolderListViewModel.add(title = $title)" }

      directoryListRepository.insertDirectory(CatapultDirectory(0, title))
   }

   fun edit(id: Int, newTitle: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "FolderListViewModel.edit(id = $id, newTitle = $newTitle)" }

      directoryListRepository.updateDirectory(CatapultDirectory(id, newTitle))
   }

   fun delete(id: Int) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "FolderListViewModel.delete(id = $id)" }

      directoryListRepository.deleteDirectory(id)
   }
}

@Immutable
data class FolderListState(
   val folders: List<CatapultDirectory>,
)
