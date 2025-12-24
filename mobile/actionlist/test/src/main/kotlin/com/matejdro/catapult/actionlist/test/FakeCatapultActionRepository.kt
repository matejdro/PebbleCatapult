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

   override fun getAll(directory: Int, limit: Int, onlyEnabled: Boolean): Flow<Outcome<List<CatapultAction>>> {
      return actions
         .map { list ->
            Outcome.Success(
               list.filter {
                  it.directoryId == directory && (!onlyEnabled || it.enabled)
               }.take(limit)
            )
         }
         .onStart { numCollections++ }
   }

   override fun getById(id: Int): Flow<Outcome<CatapultAction?>> {
      return actions
         .map { list -> Outcome.Success(list.firstOrNull { it.id == id }) }
         .onStart { numCollections++ }
   }

   override suspend fun insert(action: CatapultAction) {
      actions.update { it + action }
   }

   fun insert(vararg action: CatapultAction) {
      actions.update { it + action }
   }

   override suspend fun update(id: Int, title: String, enabled: Boolean) {
      actions.update { list -> list.map { if (it.id == id) it.copy(title = title, enabled = enabled) else it } }
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

   override suspend fun massToggle(
      directory: Int,
      enable: List<Int>,
      disable: List<Int>,
   ) {
      actions.update { list ->
         list.map {
            val enabled = if (enable.contains(it.id)) {
               true
            } else if (disable.contains(it.id)) {
               false
            } else {
               it.enabled
            }

            it.copy(enabled = enabled)
         }
      }
   }
}
