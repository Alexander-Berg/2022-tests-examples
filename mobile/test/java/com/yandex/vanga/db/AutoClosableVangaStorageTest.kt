package com.yandex.vanga.db

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.yandex.vanga.BaseRobolectricTest
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Test

class AutoClosableVangaStorageTest : BaseRobolectricTest() {

    private val mockedVangaDao: VangaDao = mock()
    private val vangaDb: VangaStorage = mock {
        on { vangaDao } doReturn mockedVangaDao
    }

    @Test
    fun `on close AutoClosableVangaStorage, db must be closed`() {
        val storage = AutoClosableVangaStorage(vangaDb)

        storage.close()

        verify(vangaDb).close()
    }

    @Test
    fun `on get vangaDao from AutoClosableVangaStorage, dao from db must be returned`() {
        val storage = AutoClosableVangaStorage(vangaDb)

        val dao = storage.vangaDao

        Assert.assertThat(dao, `is`(mockedVangaDao))
    }
}
