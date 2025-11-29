package com.matejdro.catapult.actionlist.api

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface DirectoryListRepository {
   fun getAll(): Flow<Outcome<List<CatapultDirectory>>>
   fun getSingle(id: Int): Flow<Outcome<CatapultDirectory>>
   suspend fun insert(directory: CatapultDirectory)
   suspend fun update(directory: CatapultDirectory)
   suspend fun delete(id: Int)
}
