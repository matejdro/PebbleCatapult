package com.matejdro.catapult.tasklist.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.api.DirectoryListRepository
import com.matejdro.catapult.tasklist.exception.MissingDirectoryException
import com.matejdro.catapult.tasklist.sqldelight.generated.DbDirectoryQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.Outcome

@Inject
@ContributesBinding(AppScope::class)
class DirectoryListRepositoryImpl(
   private val dbDirectoryQueries: DbDirectoryQueries,
) : DirectoryListRepository {
   override fun getAll(): Flow<Outcome<List<CatapultDirectory>>> {
      return dbDirectoryQueries.selectAll().asFlow().map {
         withDefault {
            val list = it.awaitAsList()

            if (list.isEmpty()) {
               insert(CatapultDirectory(0, "Starting Directory"))
            }

            Outcome.Success(list.map { it.toDirectory() })
         }
      }
   }

   override fun getSingle(id: Int): Flow<Outcome<CatapultDirectory>> {
      return dbDirectoryQueries.selectSingle(id.toLong()).asFlow().map {
         withDefault {
            val value = it.awaitAsOneOrNull()

            if (value == null) {
               Outcome.Error(MissingDirectoryException())
            } else {
               Outcome.Success(value.toDirectory())
            }
         }
      }
   }

   override suspend fun insert(directory: CatapultDirectory) = withDefault<Unit> {
      dbDirectoryQueries.insert(directory.toDb())
   }

   override suspend fun update(directory: CatapultDirectory) {
      require(directory.id > 1) { "Starting directory cannot be updated" }

      withDefault {
         dbDirectoryQueries.update(directory.toDb())
      }
   }

   override suspend fun delete(id: Int) {
      require(id > 1) { "Starting directory cannot be deleted" }

      withDefault {
         dbDirectoryQueries.delete(id.toLong())
      }
   }
}
