package com.matejdro.catapult.tasklist.api

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface DirectoryListRepository {
   fun getAllDirectories(): Flow<Outcome<List<CatapultDirectory>>>
   suspend fun insertDirectory(directory: CatapultDirectory)
   suspend fun updateDirectory(directory: CatapultDirectory)
   suspend fun deleteDirectory(id: Int)
}
