package ru.yandex.yandexbus.inhouse.ui.main.unsupportedcity

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest

class UnsupportedCityStorageTest : BaseTest() {

    private lateinit var prefs: SharedPreferences
    private lateinit var storage: UnsupportedCityStorage

    @Before
    override fun setUp() {
        super.setUp()
        prefs = context.getSharedPreferences("", Context.MODE_PRIVATE)
        storage = UnsupportedCityStorage(prefs)
    }

    @Test
    fun checkMultipleValuesSaved() {
        val ids = listOf(0, 25, 30)

        ids.forEach { assertFalse(storage.isWarningShown(it)) }
        ids.forEach { storage.saveWarningShown(it) }
        ids.forEach { assertTrue(storage.isWarningShown(it)) }
    }

    @Test
    fun valuesAreThereAfterStorageReCreation() {
        val ids = listOf(0, 25, 30)
        ids.forEach { storage.saveWarningShown(it) }

        val newStorage = UnsupportedCityStorage(prefs)
        ids.forEach { assertTrue(newStorage.isWarningShown(it)) }
    }

    @Test
    fun checkMarkAsNotShownWorks() {
        val id = 42
        assertFalse(storage.isWarningShown(id))

        storage.saveWarningShown(id)
        assertTrue(storage.isWarningShown(id))

        storage.saveWarningNotShown(id)
        assertFalse(storage.isWarningShown(id))
    }

}