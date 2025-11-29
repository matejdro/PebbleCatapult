package com.matejdro.catapult.tasklist.api

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface CatapultActionRepository {
   fun getAll(directory: Int): Flow<Outcome<List<CatapultAction>>>
   suspend fun insert(taskerTask: CatapultAction)
   suspend fun updateTitle(id: Int, title: String)
   suspend fun delete(id: Int)
   suspend fun reorder(id: Int, toIndex: Int)
}
