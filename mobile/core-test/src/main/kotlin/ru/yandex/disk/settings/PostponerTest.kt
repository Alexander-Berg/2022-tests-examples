package ru.yandex.disk.settings

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.util.ScopedKeyValueStore
import ru.yandex.disk.util.SystemClock
import java.util.concurrent.TimeUnit

private const val KEY = "key"

class PostponerTest: TestCase2() {

    private val strategy = mock<PostponeStrategy>()
    private val keyValueStore = ScopedKeyValueStore(TestObjectsFactory.createSettings(RuntimeEnvironment.application!!), "test")
    private val clock = mock<SystemClock>()
    private val postponer = Postponer(KEY, strategy, keyValueStore, clock)

    private val ONE_DAY = TimeUnit.DAYS.toMillis(1)

    @Test
    fun `new postponer should not be postponed`() {
        assertThat(postponer.isPostponed(), equalTo(false))
    }

    @Test
    fun `postponer should be postponed after postponing`() {
        whenever(strategy.getPostponeInterval(1)) doReturn (System.currentTimeMillis() + ONE_DAY)

        postponer.postpone()
        assertThat(postponer.isPostponed(), equalTo(true))
        assertThat(postponer.isTimeToShow(), equalTo(false))
    }

    @Test
    fun `postponer should not be postponed after doNotPostponeAnymore called`() {
        whenever(strategy.getPostponeInterval(1)) doReturn (System.currentTimeMillis() + ONE_DAY)

        postponer.postpone()

        postponer.doNotPostponeAnymore()
        assertThat(postponer.isPostponeDisabled(), equalTo(true))
    }
}
