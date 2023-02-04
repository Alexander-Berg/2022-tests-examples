package ru.auto.feature.dealer

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.util.Clock
import ru.auto.ara.util.thenCompletable
import ru.auto.ara.util.thenSingle
import ru.auto.data.interactor.DealerPromoBannerInteractor
import ru.auto.data.model.dealer.DealerPromoBannerShowState
import ru.auto.data.repository.IDealerPromoBannerRepository
import java.util.*

@RunWith(AllureRunner::class) class DealerPromoBannerInteractorTest {

    private val repo = mock(IDealerPromoBannerRepository::class.java)
    private val interactor = DealerPromoBannerInteractor(repo)

    @Test
    fun `isBannerVisible should return true if repo returns zero show count`() {
        val state = DealerPromoBannerShowState(0, Date())
        whenever(repo.getShowState()).thenSingle(state)

        interactor.isBannerVisible()
            .test()
            .assertValue(true)
    }

    @Test
    fun `isBannerVisible should return true if repo returns non zero show count and date less then now`() {
        val date = Calendar.getInstance()
            .apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            .time

        val state = DealerPromoBannerShowState(1, date)
        whenever(repo.getShowState()).thenSingle(state)

        interactor.isBannerVisible()
            .test()
            .assertValue(true)
    }

    @Test
    fun `isBannerVisible should return false if repo returns non zero show count and date more then now`() {
        val date = Clock.nowCalendar()
            .apply {
                add(Calendar.DATE, 1)
            }
            .time

        val state = DealerPromoBannerShowState(1, date)
        whenever(repo.getShowState()).thenSingle(state)

        interactor.isBannerVisible()
            .test()
            .assertValue(false)
    }

    @Test
    fun `handleBannerDisappearance should save 14 days interval if repo returns zero show count`() {
        val savedState = DealerPromoBannerShowState(0, Date())
        whenever(repo.getShowState()).thenSingle(savedState)

        val calendar = Clock.nowCalendar().apply {
            add(Calendar.DATE, 14)
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newState = DealerPromoBannerShowState(1, calendar.time)
        whenever(repo.saveShowState(newState)).thenCompletable()

        interactor.handleBannerDisappearance()
            .test()
            .assertCompleted()
    }

    @Test
    fun `handleBannerDisappearance should save 1 month interval if repo returns 1 show count`() {
        val savedState = DealerPromoBannerShowState(1, Clock.now())
        whenever(repo.getShowState()).thenSingle(savedState)

        val calendar = Clock.nowCalendar().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newState = DealerPromoBannerShowState(2, calendar.time)
        whenever(repo.saveShowState(newState)).thenCompletable()

        interactor.handleBannerDisappearance()
            .test()
            .assertCompleted()
    }

}
