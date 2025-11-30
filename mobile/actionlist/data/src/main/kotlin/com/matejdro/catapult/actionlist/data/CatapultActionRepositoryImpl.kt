package com.matejdro.catapult.actionlist.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import com.matejdro.catapult.actionlist.sqldelight.generated.DbActionQueries
import com.matejdro.catapult.actionlist.sqldelight.generated.SelectAll
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
) : CatapultActionRepository {
   override fun getAll(directory: Int, limit: Int): Flow<Outcome<List<CatapultAction>>> {
      return dbActionQueries.selectAll(directory.toLong(), limit.toLong()).asFlow()
         .map<Query<SelectAll>, Outcome<List<CatapultAction>>> {
            withDefault {
               val list = it.awaitAsList()

               Outcome.Success(list.map { it.toCatapultAction() })
            }
         }
         .catch { emit(Outcome.Error(UnknownCauseException(cause = it))) }
   }

   override suspend fun insert(action: CatapultAction) = withIO<Unit> {
      dbActionQueries.insert(
         title = action.title,
         directoryId = action.directoryId.toLong(),
         taskerTaskName = action.taskerTaskName,
         targetDirectoryId = action.targetDirectoryId?.toLong()
      )
   }

   override suspend fun updateTitle(id: Int, title: String) {
      withIO {
         dbActionQueries.updateTitle(title, id.toLong())
      }
   }

   override suspend fun delete(id: Int) {
      withIO {
         dbActionQueries.delete(id.toLong())
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
      }
   }
}
