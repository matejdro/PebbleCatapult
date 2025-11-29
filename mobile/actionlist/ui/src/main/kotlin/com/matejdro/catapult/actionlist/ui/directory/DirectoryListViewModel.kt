package com.matejdro.catapult.actionlist.ui.directory

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.api.DirectoryListRepository
import com.matejdro.catapult.common.logging.ActionLogger
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
class DirectoryListViewModel(
   private val directoryListRepository: DirectoryListRepository,
   private val resources: CoroutineResourceManager,
   private val actionLogger: ActionLogger,
) : CoroutineScopedService(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<DirectoryListState>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<DirectoryListState>> = _uiState

   override fun onServiceRegistered() {
      actionLogger.logAction { "FolderListViewModel.onServiceRegistered()" }

      resources.launchResourceControlTask(_uiState) {
         emitAll(
            directoryListRepository.getAll().map { outcome ->
               outcome.mapData { DirectoryListState(it) }
            }
         )
      }
   }

   fun add(title: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "FolderListViewModel.add(title = $title)" }

      directoryListRepository.insert(CatapultDirectory(0, title))
   }

   fun edit(id: Int, newTitle: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "FolderListViewModel.edit(id = $id, newTitle = $newTitle)" }

      directoryListRepository.update(CatapultDirectory(id, newTitle))
   }

   fun delete(id: Int) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "FolderListViewModel.delete(id = $id)" }

      directoryListRepository.delete(id)
   }
}

@Immutable
data class DirectoryListState(
   val directories: List<CatapultDirectory>,
)
