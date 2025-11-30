package com.matejdro.catapult.actionlist.test

import com.matejdro.catapult.actionlist.api.CatapultAction
import com.matejdro.catapult.actionlist.api.CatapultActionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import si.inova.kotlinova.core.outcome.Outcome

class FakeCatapultActionRepository : CatapultActionRepository {
   var numCollections = 0

   private val actions = MutableStateFlow<List<CatapultAction>>(emptyList())

   override fun getAll(directory: Int, limit: Int): Flow<Outcome<List<CatapultAction>>> {
      return actions
         .map { list -> Outcome.Success(list.filter { it.directoryId == directory }.take(limit)) }
         .onStart { numCollections++ }
   }

   override suspend fun insert(action: CatapultAction) {
      actions.update { it + action }
   }

   override suspend fun updateTitle(id: Int, title: String) {
      actions.update { list -> list.map { if (it.id == id) it.copy(title = title) else it } }
   }

   override suspend fun reorder(id: Int, toIndex: Int) {
      actions.update { list ->
         val existing = list.first { it.id == id }
         list.toMutableList().apply {
            remove(existing)
            add(toIndex, existing)
         }
      }
   }

   override suspend fun delete(id: Int) {
      actions.update { list -> list.filter { it.id != id } }
   }
}
