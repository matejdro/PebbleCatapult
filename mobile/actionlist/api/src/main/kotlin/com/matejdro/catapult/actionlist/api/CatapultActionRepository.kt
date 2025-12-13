package com.matejdro.catapult.actionlist.api

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface CatapultActionRepository {
   fun getAll(directory: Int, limit: Int = Int.MAX_VALUE): Flow<Outcome<List<CatapultAction>>>
   fun getById(id: Int): Flow<Outcome<CatapultAction?>>
   suspend fun insert(action: CatapultAction)
   suspend fun updateTitle(id: Int, title: String)
   suspend fun delete(id: Int)
   suspend fun reorder(id: Int, toIndex: Int)
}
