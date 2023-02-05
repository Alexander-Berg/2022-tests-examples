package com.yandex.sync.lib

import androidx.content.edit
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(SyncTestRunner::class)
class CalendarStorageTest {

    lateinit var storage: CalendarStorage

    @Before
    fun setUp() {
        storage = CalendarStorage(RuntimeEnvironment.application.baseContext)
    }

    @Test
    fun versionTypeMigration() {
        val previousVersion = CalendarStorage.CURRENT_VERSION - 1
        storage.sp.edit {
            putInt(CalendarStorage.CALENDAR_VERSION, previousVersion)
        }

        val account = "account"

        assertEquals(previousVersion, storage.getPreviousVersion(account))

        storage.updateVersion(account)
        assertEquals(CalendarStorage.CURRENT_VERSION, storage.getPreviousVersion(account))
    }

    @Test
    fun checkInitialVersion(){
        assertEquals(storage.getPreviousVersion("account"), CalendarStorage.NO_DATA)
    }
}