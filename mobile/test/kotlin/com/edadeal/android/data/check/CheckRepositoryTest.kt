package com.edadeal.android.data.check

import android.os.Build
import com.edadeal.android.data.Prefs
import com.edadeal.android.data.datasync.DataSyncConfig
import com.edadeal.android.dto.Check
import com.edadeal.android.metrics.MonotonicTime
import com.edadeal.android.metrics.ScannerInputType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CheckRepositoryTest {

    private val check1 = Check("1", "a", ScannerInputType.Scan, "cb")
    private val check2 = Check("2", "b", ScannerInputType.Manual, "|auto|")
    private val check3 = Check("3", "c", ScannerInputType.Scan, "cb")

    private lateinit var checkRepository: CheckRepository

    @BeforeTest
    fun prepare() {
        val time = MonotonicTime(epochTimeDiff = 0) { 0 }
        val prefs = Prefs(RuntimeEnvironment.application, DataSyncConfig.DEV, isDev = true)
        checkRepository = PrefsCheckRepository(prefs, time)
    }

    @Test
    fun `getAll checks should contain previously added checks`() {
        val expectedCheck1 = check1
        val expectedCheck2 = check2
        assertEquals(expectedCheck1, checkRepository.getOrPutByQr(expectedCheck1.qr, expectedCheck1))
        assertEquals(expectedCheck2, checkRepository.getOrPutByQr(expectedCheck2.qr, expectedCheck2))
        val allChecks = checkRepository.getAll()
        assert(allChecks.contains(expectedCheck1))
        assert(allChecks.contains(expectedCheck2))
    }

    @Test
    fun `getOrPutByQr should return prev check with same qr`() {
        val check1WithDifferentTimeAndType = Check(check1.qr, check2.time.orEmpty(), check3.type, check1.scannerStrategy)
        assertEquals(check1, checkRepository.getOrPutByQr(check1.qr, check1))
        assertEquals(check1, checkRepository.getOrPutByQr(check1.qr, check1WithDifferentTimeAndType))
        val allChecks = checkRepository.getAll()
        assertEquals(1, allChecks.size)
        assertEquals(check1, allChecks.first())
    }

    @Test
    fun `deleteByQr should delete previously added check with same qr`() {
        val addedCheck = check1
        checkRepository.getOrPutByQr(addedCheck.qr, addedCheck)
        checkRepository.deleteByQr(addedCheck.qr)
        assertFalse(checkRepository.getAll().contains(addedCheck))
    }

    @Test
    fun `getCount should return actual count of checks`() {
        val checksToAdd = addMockChecks()
        assertEquals(checksToAdd.size, checkRepository.getCount())
    }

    @Test
    fun `clear should delete all saved checks`() {
        addMockChecks()
        checkRepository.clear()
        assert(checkRepository.getAll().isEmpty())
        assertEquals(0, checkRepository.getCount())
    }

    private fun addMockChecks(): List<Check> {
        val checksToAdd = listOf(check1, check2, check3)
        for (check in checksToAdd) {
            checkRepository.getOrPutByQr(check.qr, check)
        }
        return checksToAdd
    }
}
