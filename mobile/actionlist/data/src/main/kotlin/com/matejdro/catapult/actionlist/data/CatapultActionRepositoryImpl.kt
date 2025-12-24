package com.matejdro.catapult.actionlist.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.actionlist.sqldelight.generated.DbActionQueries
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectAll
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectSingle
import com.matejdro.catapult.bluetooth.WatchSyncer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import dispatch.core.withIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.exceptions.UnknownCauseException
import si.inova.kotlinova.core.outcome.Outcome

@Inject
@ContributesBinding(AppScope::class)
class CatapultActionRepositoryImpl(
   private val dbActionQueries: DbActionQueries,
   private val watchSyncer: WatchSyncer,
) : CatapultActionRepository {
   override fun getAll(directory: Int, limit: Int, onlyEnabled: Boolean): Flow<Outcome<List<CatapultAction>>> {
      val enabledNot = if (onlyEnabled) 0L else 2L
      return dbActionQueries.selectAll(directory.toLong(), enabledNot, limit.toLong()).asFlow()
         .map<Query<SelectAll>, Outcome<List<CatapultAction>>> {
            withDefault {
               val list = it.awaitAsList()

               Outcome.Success(list.map { it.toCatapultAction() })
            }
         }
         .catch { emit(Outcome.Error(UnknownCauseException(cause = it))) }
   }

   override fun getById(id: Int): Flow<Outcome<CatapultAction?>> {
      return dbActionQueries.selectSingle(id.toLong()).asFlow()
         .map<Query<SelectSingle>, Outcome<CatapultAction?>> {
            withDefault {
               val entry = it.awaitAsOneOrNull()

               Outcome.Success(entry?.toCatapultAction())
            }
         }
         .catch { emit(Outcome.Error(UnknownCauseException(cause = it))) }
   }

   override suspend fun insert(action: CatapultAction) = withIO<Unit> {
      dbActionQueries.insert(
         title = action.title,
         directoryId = action.directoryId.toLong(),
         taskerTaskName = action.taskerTaskName,
         targetDirectoryId = action.targetDirectoryId?.toLong(),
         enabled = if (action.enabled) 1L else 0L,
      )

      watchSyncer.syncDirectory(action.directoryId)
   }

   override suspend fun update(id: Int, title: String, enabled: Boolean) {
      withIO {
         val directoryId = dbActionQueries.getDirectoryId(id.toLong()).executeAsOne().toInt()
         dbActionQueries.update(title, if (enabled) 1L else 0L, id.toLong())

         watchSyncer.syncDirectory(directoryId)
      }
   }

   override suspend fun delete(id: Int) {
      withIO {
         val directoryId = dbActionQueries.getDirectoryId(id.toLong()).executeAsOne().toInt()
         dbActionQueries.delete(id.toLong())

         watchSyncer.syncDirectory(directoryId)
      }
   }

   override suspend fun reorder(id: Int, toIndex: Int) {
      withIO {
         val currentAction = dbActionQueries.selectSingleRaw(id.toLong()).executeAsOne()
         val fromIndex = currentAction.sortOrder
         val directoryId = currentAction.directoryId

         if (toIndex > fromIndex) {
            dbActionQueries.reorderUpwards(
               id = id.toLong(),
               fromIndex = fromIndex.toLong(),
               toIndex = toIndex.toLong(),
               directoryId = directoryId
            )
         } else {
            dbActionQueries.reorderDownwards(
               id = id.toLong(),
               fromIndex = fromIndex.toLong(),
               toIndex = toIndex.toLong(),
               directoryId = directoryId
            )
         }

         watchSyncer.syncDirectory(directoryId.toInt())
      }
   }

   override suspend fun massToggle(directory: Int, enable: List<Int>, disable: List<Int>) {
      withIO {
         dbActionQueries.toggle(enable.map { it.toLong() }, disable.map { it.toLong() })
         watchSyncer.syncDirectory(directory)
      }
   }
}
