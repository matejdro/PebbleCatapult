package com.matejdro.catapult.actionlist.api

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface CatapultActionRepository {
   fun getAll(directory: Int, limit: Int = Int.MAX_VALUE, onlyEnabled: Boolean = false): Flow<Outcome<List<CatapultAction>>>
   fun getById(id: Int): Flow<Outcome<CatapultAction?>>
   suspend fun insert(action: CatapultAction)
   suspend fun update(id: Int, title: String, enabled: Boolean)
   suspend fun delete(id: Int)
   suspend fun reorder(id: Int, toIndex: Int)
}
