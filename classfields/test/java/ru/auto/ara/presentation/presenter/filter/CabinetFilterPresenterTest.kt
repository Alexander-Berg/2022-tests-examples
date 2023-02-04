package ru.auto.ara.presentation.presenter.filter

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.ara.dealer.filter.DealerFilterUpdater
import ru.auto.ara.filter.IDealerOffersFilterScreenFactory
import ru.auto.ara.filter.screen.user.DealerFilterScreen
import ru.auto.ara.presentation.viewstate.filter.CabinetFilterViewState
import ru.auto.ara.router.Navigator
import ru.auto.ara.router.command.GoBackCommand
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.error.ErrorFactory
import ru.auto.ara.util.statistics.AnalystManager
import ru.auto.data.model.Campaign
import ru.auto.data.model.DealerOffersFilter
import ru.auto.data.model.catalog.Subcategory
import ru.auto.data.model.data.offer.ALL
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.USED
import ru.auto.data.repository.IDealerCampaignsRepository
import ru.auto.data.repository.IFilterRepository
import ru.auto.data.repository.IUserOffersRepository
import rx.Observable

/**
 * @author aleien on 12.07.18.
 */
@RunWith(AllureRunner::class) class CabinetFilterPresenterTest : RxTest() {

    private val view = mock<CabinetFilterViewState>()
    private val navigator = mock<Navigator>()
    private val errorFactory = mock<ErrorFactory>().apply {
        whenever(createSnackError(any())).thenReturn("error snack")
    }
    private val filterRepository = mock<IFilterRepository>().apply {
        whenever(getFilter()).thenReturn(TEST_DEFAULT_FILTER)
    }

    private val dealerCampaignsRepo = mock<IDealerCampaignsRepository>().apply {
        whenever(getDealerCampaigns()).thenReturn(Observable.just(listOf(Campaign(CAR, listOf(USED)))))
    }
    private val subcategoryProvider = mock<OptionsProvider<Subcategory>>().apply {
        whenever(get(any())).thenReturn(listOf(Subcategory("id", "label", "alias")))
    }

    private val userFilterScreenFactory = mock<IDealerOffersFilterScreenFactory>().apply {
        whenever(create(any(), any(), any(), any())).thenReturn(DealerFilterScreen("name", listOf()))
    }

    private val analytics = mock<AnalystManager>()
    private val userOffersRepository = mock<IUserOffersRepository>()
    private val strings = mock<StringsProvider> {
        on { get(any()) } doReturn ""
    }

    private val filterUpdater = mock<DealerFilterUpdater>()

    private val presenter: CabinetFilterPresenter by lazy {
        CabinetFilterPresenter(
            view,
            navigator,
            errorFactory,
            filterRepository,
            dealerCampaignsRepo,
            userFilterScreenFactory,
            subcategoryProvider,
            analytics,
            userOffersRepository,
            strings,
            filterUpdater
        )
    }

    @Before
    fun setup() {
        presenter.onViewCreated()
    }

    @Test
    fun `should update filter in filter repository on showResults`() {
        presenter.onShowResults()

        verify(navigator).perform(GoBackCommand)
        verify(filterRepository).updateFilter(any())
    }

    companion object {
        private val TEST_DEFAULT_FILTER = DealerOffersFilter(ALL)
    }

}
