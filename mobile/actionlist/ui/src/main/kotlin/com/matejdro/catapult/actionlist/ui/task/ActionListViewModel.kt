package com.matejdro.catapult.actionlist.ui.task

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.actionlist.api.CatapultDirectory
import com.matejdro.catapult.actionlist.api.DirectoryListRepository
import com.matejdro.catapult.common.logging.ActionLogger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.flatMapLatestOutcome
import si.inova.kotlinova.core.outcome.mapData
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.CoroutineScopedService

@Stable
@Inject
@ContributesScopedService
class ActionListViewModel(
   private val resources: CoroutineResourceManager,
   private val directoryRepo: DirectoryListRepository,
   private val actionsRepo: CatapultActionRepository,
   private val actionLogger: ActionLogger,
) : CoroutineScopedService(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<ActionListState>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<ActionListState>> = _uiState

   private var selectedDirectory: Int? = null

   fun load(id: Int) {
      actionLogger.logAction { "TaskListViewModel.load(id = $id)" }
      if (selectedDirectory == id) return
      selectedDirectory = id

      resources.launchResourceControlTask(_uiState) {
         val flow = directoryRepo.getSingle(id).flatMapLatestOutcome { directory ->
            actionsRepo.getAll(directory.id).map { outcome ->
               outcome.mapData { list ->
                  ActionListState(directory, list)
               }
            }
         }

         emitAll(flow)
      }
   }

   fun add(title: String, targetTask: String?, targetDirectory: Int?) = resources.launchWithExceptionReporting {
      actionLogger.logAction {
         "TaskListViewModel.add(" +
            "title = $title, targetTask = ${targetTask ?: "null"}, targetDirectory = ${targetDirectory ?: "null"}" +
            ")"
      }

      val directoryId = selectedDirectory ?: return@launchWithExceptionReporting
      actionsRepo.insert(
         CatapultAction(
            title = title,
            directoryId = directoryId,
            taskerTaskName = targetTask,
            targetDirectoryId = targetDirectory
         )
      )
   }

   fun editActionTitle(id: Int, title: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "ActionListViewModel.editActionTitle(id = $id, title = $title)" }

      actionsRepo.updateTitle(id, title)
   }

   fun deleteAction(id: Int) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "TaskListViewModel.deleteAction(id = $id)" }

      actionsRepo.delete(id)
   }
}

@Immutable
data class ActionListState(
   val directory: CatapultDirectory,
   val actions: List<CatapultAction>,
)
