package com.yandex.vanga.db

import androidx.room.Room
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.yandex.vanga.BaseRobolectricTest
import com.yandex.vanga.InvalidKeysSeparator
import com.yandex.vanga.Logger
import com.yandex.vanga.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class Migration3to4Test : BaseRobolectricTest() {

    private lateinit var visitsBatchInsert: VisitsBatchInsert

    private lateinit var autoCloseStorage: AutoClosableVangaStorage

    val logger = object : Logger {
        override fun d(message: String) {
            println("d $message")
        }

        override fun logException(message: String, t: Throwable) {
            println("logException $message $t")
        }

        override fun e(message: String) {
            println("e $message")
        }
    }

    @Before
    fun setUp() {
        val storage = Room.databaseBuilder(RuntimeEnvironment.application, VangaStorage::class.java, "vanga.db")
                .allowMainThreadQueries()
                .build()

        storage.vangaDao.installTimeHelper = mock { on { getFirstInstallTime(any(), any()) } doReturn 0L }
        autoCloseStorage = AutoClosableVangaStorage(storage)
        storage.vangaDao.db = autoCloseStorage.getWritableDatabase()

        visitsBatchInsert = spy(storage.vangaDao.createVisitsBatchInsert(10))

        doAnswer { invocationOnMock ->
            val result = invocationOnMock.callRealMethod()
            visitsBatchInsert.execute()
            result
        }.`when`(visitsBatchInsert).add(any())
    }

    @After
    fun tearDown() {
        autoCloseStorage.close()
    }

    @Test
    fun `empty visits, all keys falls into migration candidates`() {
        val allKeys = setOf("validA", "validB", "validC")

        allKeys.forEach { autoCloseStorage.vangaDao.updateVisitsInfo(it, 1, 1, 0, visitsBatchInsert) }

        val keyValidator = object : InvalidKeysSeparator {
            override fun separateAndGetInvalidKeys(candidateKeys: List<String>): List<String> {
                assertThat(candidateKeys.toSet(), equalTo(allKeys))
                return emptyList()
            }
        }

        migration3To4Actual(keyValidator, autoCloseStorage.getWritableDatabase(), logger)
    }

    @Test
    fun `visits are present, all of keys falls into migration candidates`() {
        val allKeys = listOf("validA", "validB", "validC")

        allKeys.forEach { autoCloseStorage.vangaDao.updateVisitsInfo(it, 1, 1, 1, visitsBatchInsert) }

        val keyValidator = object : InvalidKeysSeparator {
            override fun separateAndGetInvalidKeys(candidateKeys: List<String>): List<String> {
                assertThat(candidateKeys.sorted(), equalTo(allKeys.sorted()))
                return emptyList()
            }
        }

        migration3To4Actual(keyValidator, autoCloseStorage.getWritableDatabase(), logger)
    }

    @Test
    fun `invalid keys are deleted from db`() {
        val validKeys = setOf("validA", "validB", "validC")
        val invalidKeys = setOf("invalidZ", "invalidY", "invalidZ")

        val allKeys = validKeys + invalidKeys

        allKeys.forEach { autoCloseStorage.vangaDao.updateVisitsInfo(it, 1, 1, 0, visitsBatchInsert) }

        val keyValidator = object : InvalidKeysSeparator {
            override fun separateAndGetInvalidKeys(candidateKeys: List<String>): List<String> {
                return invalidKeys.toList()
            }
        }

        migration3To4Actual(keyValidator, autoCloseStorage.getWritableDatabase(), logger)

        assertThat(invalidKeys.count { autoCloseStorage.vangaDao.getVangaItem(it) != null }, equalTo(0))
    }

    @Test
    fun `valid keys are remain in db`() {
        val validKeys = setOf("validA", "validB", "validC")
        val invalidKeys = setOf("invalidZ", "invalidY", "invalidZ")

        val allKeys = validKeys + invalidKeys

        allKeys.forEach { autoCloseStorage.vangaDao.updateVisitsInfo(it, 1, 1, 0, visitsBatchInsert) }

        val keyValidator = object : InvalidKeysSeparator {
            override fun separateAndGetInvalidKeys(candidateKeys: List<String>): List<String> {
                return invalidKeys.toList()
            }
        }

        migration3To4Actual(keyValidator, autoCloseStorage.getWritableDatabase(), logger)

        assertThat(validKeys.map { autoCloseStorage.vangaDao.getVangaItem(it)!!.key }.toSet(), equalTo(validKeys))
    }
}
