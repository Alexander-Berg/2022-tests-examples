// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.changes

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.db.changes.ChangesDao
import ru.yandex.direct.domain.Changes
import ru.yandex.direct.util.Optional
import java.util.*

class ChangesLocalRepositoryTest {
    @Test
    fun select_shouldCallDao() {
        MockedEnvironment().apply {
            dao.stub {
                on { getLatestChanges(changes.type) } doReturn(changes)
            }
            assertThat(repo.select(changes.type)).isEqualToComparingFieldByFieldRecursively(Optional.just(changes))
            verify(dao, times(1)).getLatestChanges(changes.type)
        }
    }

    @Test
    fun update_shouldCallDao() {
        MockedEnvironment().apply {
            repo.update(changes.type, Optional.just(changes))
            verify(dao, times(1)).deleteByTypeThenInsert(changes)
        }
    }

    @Test
    fun changes_shouldBeAvailableAfterUpdate() {
        SimpleEnvironment().apply {
            repo.update(changes.type, Optional.just(changes))
            assertThat(repo.select(changes.type)).isEqualTo(Optional.just(changes))
        }
    }

    private class MockedEnvironment {
        val dao = mock<ChangesDao>()
        val repo = ChangesLocalRepository(dao, mock())
        val changes = Changes(null, Changes.Type.REGIONS, Date(1))
    }

    private class SimpleEnvironment {
        val dao = SimpleChangesDao()
        val repo = ChangesLocalRepository(dao, mock())
        val changes = Changes(null, Changes.Type.REGIONS, Date(1))
    }

    private class SimpleChangesDao : ChangesDao(mock(), Gson()) {
        private val changesMap = mutableMapOf<Changes.Type, Changes>()

        override fun getChanges(type: Changes.Type): List<Changes> {
            val changes = changesMap[type]
            return if (changes == null) emptyList() else listOf(changes)
        }

        override fun deleteByTypeThenInsert(changes: Changes) {
            changesMap[changes.type] = changes
        }
    }
}