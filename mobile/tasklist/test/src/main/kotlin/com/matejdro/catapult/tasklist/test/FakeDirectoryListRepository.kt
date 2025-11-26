package com.matejdro.catapult.tasklist.test

import com.matejdro.catapult.tasklist.api.CatapultDirectory
import com.matejdro.catapult.tasklist.api.DirectoryListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import si.inova.kotlinova.core.outcome.Outcome

class FakeDirectoryListRepository : DirectoryListRepository {
   private val direcories = MutableStateFlow<List<CatapultDirectory>>(emptyList())

   override fun getAll(): Flow<Outcome<List<CatapultDirectory>>> {
      return direcories.map { Outcome.Success(it) }
   }

   override suspend fun insert(directory: CatapultDirectory) {
      direcories.update { it + directory }
   }

   override suspend fun update(directory: CatapultDirectory) {
      direcories.update { list -> list.filter { it.id != directory.id } + directory }
   }

   override suspend fun delete(id: Int) {
      direcories.update { list -> list.filter { it.id != id } }
   }
}
