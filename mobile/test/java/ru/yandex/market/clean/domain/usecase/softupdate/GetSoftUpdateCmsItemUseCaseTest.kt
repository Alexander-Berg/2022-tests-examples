package ru.yandex.market.clean.domain.usecase.softupdate

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.AppUpdateRepository
import ru.yandex.market.clean.data.source.cms.CmsSoftUpdateMergedWidgetRepository
import ru.yandex.market.clean.domain.model.cms.CmsFont
import ru.yandex.market.clean.domain.model.cms.CmsItem
import ru.yandex.market.clean.domain.model.cms.CmsWidgetSubtitle
import ru.yandex.market.clean.domain.model.cms.CmsWidgetTitle
import ru.yandex.market.clean.domain.model.cms.garson.MergedWidgetParams
import ru.yandex.market.clean.domain.model.cms.garson.plusBenefitsGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.softUpdateGarsonTestInstance
import ru.yandex.market.feature.updater.InstallStatus

class GetSoftUpdateCmsItemUseCaseTest {

    private val softUpdateGarson = softUpdateGarsonTestInstance()
    private val itemTitleSubTile = MergedWidgetParams(
        CmsWidgetTitle.testInstance(),
        CmsWidgetSubtitle("subTitle", CmsFont.normalCmsFont(), null),
        false
    )
    private val item = mock<CmsItem>()
    private val firstInstallStatus = InstallStatus.Pending
    private val secondInstallStatus = InstallStatus.Installing
    private val installStatusSubject: BehaviorSubject<InstallStatus> =
        BehaviorSubject.createDefault(firstInstallStatus)
    private val appUpdateRepository = mock<AppUpdateRepository> {
        on { getInstallStatusChangesStream() } doReturn installStatusSubject
        on { checkForUpdate() } doReturn Observable.just(true)
    }
    private val cmsSoftUpdateMergedWidgetRepository = mock<CmsSoftUpdateMergedWidgetRepository> {
        on { getSoftUpdateItem(any(), eq(softUpdateGarson), eq(itemTitleSubTile)) } doReturn item
    }

    private val useCase = GetSoftUpdateCmsItemUseCase(appUpdateRepository, cmsSoftUpdateMergedWidgetRepository)

    @Test
    fun `Return value for SoftUpdateGarson`() {
        useCase.execute(softUpdateGarson, itemTitleSubTile).test()
            .assertNoErrors()
            .assertValue(listOf(item))
    }

    @Test
    fun `Update value on app info changes`() {
        val testSubscription = useCase.execute(softUpdateGarson, itemTitleSubTile).test()
        installStatusSubject.onNext(secondInstallStatus)

        testSubscription
            .assertNoErrors()
            .assertValueCount(2)

        val inOrderChecks = inOrder(cmsSoftUpdateMergedWidgetRepository)
        inOrderChecks.verify(cmsSoftUpdateMergedWidgetRepository)
            .getSoftUpdateItem(firstInstallStatus, softUpdateGarson, itemTitleSubTile)
        inOrderChecks.verify(cmsSoftUpdateMergedWidgetRepository)
            .getSoftUpdateItem(secondInstallStatus, softUpdateGarson, itemTitleSubTile)
    }

    @Test
    fun `Return empty for wrong garson`() {
        useCase.execute(plusBenefitsGarsonTestInstance(), itemTitleSubTile).test()
            .assertNoErrors()
            .assertValue(emptyList())
    }
}