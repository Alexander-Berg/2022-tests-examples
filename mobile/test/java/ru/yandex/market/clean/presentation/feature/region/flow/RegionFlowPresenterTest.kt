package ru.yandex.market.clean.presentation.feature.region.flow

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.base.presentation.core.schedule.PresentationSchedulers
import ru.yandex.market.clean.domain.model.AutoDetectedRegion
import ru.yandex.market.clean.presentation.feature.region.confirm.RegionConfirmFragment
import ru.yandex.market.clean.presentation.feature.region.confirm.RegionConfirmTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.parcelable.toParcelable

class RegionFlowPresenterTest {

    private val useCases = mock<RegionFlowUseCases>()

    private val router = mock<Router>()

    private val schedulers = mock<PresentationSchedulers> {
        on { main } doReturn Schedulers.trampoline()
    }

    private val presenter = RegionFlowPresenter(
        useCases = useCases,
        router = router,
        schedulers = schedulers
    )

    private val view = mock<RegionFlowView>()

    @Test
    fun `Navigates to region confirm screen on attach`() {
        val autoDetectedRegion = AutoDetectedRegion()
        whenever(useCases.getAutoDetectedRegion()) doReturn Single.just(autoDetectedRegion)

        presenter.attachView(view)

        verify(router, times(1)).navigateTo(
            RegionConfirmTargetScreen(
                RegionConfirmFragment.Arguments(
                    regionId = autoDetectedRegion.currentRegionId,
                    autoDetectedRegionParcelable = autoDetectedRegion.toParcelable(),
                    isAutoDetecting = true
                )
            )
        )
    }

    @Test
    fun `Calls 'view finishFlow' if region detecting failed`() {
        whenever(useCases.getAutoDetectedRegion()) doReturn Single.error(RuntimeException())

        presenter.attachView(view)

        verify(view, times(1)).finishFlow()
    }

    @Test
    fun `Calls 'view finishFlow' on region confirmed`() {
        whenever(useCases.getAutoDetectedRegion()) doReturn Single.just(AutoDetectedRegion())

        presenter.attachView(view)
        presenter.onRegionConfirmed()

        verify(view, times(1)).finishFlow()
    }

    @Test
    fun `Calls 'view finishFlow' on region confirmation failed`() {
        whenever(useCases.getAutoDetectedRegion()) doReturn Single.just(AutoDetectedRegion())

        presenter.attachView(view)
        presenter.onRegionConfirmationFailed()

        verify(view, times(1)).finishFlow()
    }
}