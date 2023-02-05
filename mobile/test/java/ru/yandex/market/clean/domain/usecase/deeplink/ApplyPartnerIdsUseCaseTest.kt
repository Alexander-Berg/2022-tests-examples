package ru.yandex.market.clean.domain.usecase.deeplink

import android.net.Uri
import android.os.Build
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.domain.clid.model.ClidInfo
import ru.yandex.market.domain.clid.usecase.SaveClidInfoUseCase
import ru.yandex.market.domain.pof.model.PofInfo
import ru.yandex.market.domain.pof.usecase.CheckReferralInfoUseCase
import ru.yandex.market.domain.pof.usecase.SavePofInfoUseCase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ApplyPartnerIdsUseCaseTest {
    val currentTime = System.currentTimeMillis()

    val saveClidInfoUseCase = mock<SaveClidInfoUseCase>()

    val savePofInfoUseCase = mock<SavePofInfoUseCase>()

    val checkReferralInfoUseCase = mock<CheckReferralInfoUseCase>()

    val dateTimeProvider = mock<DateTimeProvider> {
        on { currentUtcTimeInMillis } doReturn currentTime
    }

    private val scheduler = WorkerScheduler(Schedulers.trampoline())

    val useCase = ApplyPartnerIdsUseCase(
        dateTimeProvider = dateTimeProvider,
        saveClidInfoUseCase = saveClidInfoUseCase,
        savePofInfoUseCase = savePofInfoUseCase,
        checkReferralInfoUseCase = checkReferralInfoUseCase,
        workerScheduler = scheduler
    )

    @Test
    fun `testApplyPofInfoFromUri with new clid`() {
        val clid = "clid"
        val wprid = "wprid"
        val icookie = "icookie"
        val baobabEventId = "baobabEventId"
        val utmSourceService = "utmSourceService"

        val deeplinkUri = Uri.parse(
            "yamarket://home?src_pof=$clid&wprid=$wprid&icookie=$icookie&baobab_event_id=$baobabEventId&utm_source_service=$utmSourceService",
        )

        val pofInfo = PofInfo(
            pof = clid,
            wprid = wprid,
            icookie = icookie,
            baobabEventId = baobabEventId,
            utmSourceService = utmSourceService,
            lastUpdateTimestamp = currentTime
        )

        useCase.applyPofInfoFromUri(deeplinkUri)
            .test()

        verify(savePofInfoUseCase, times(1)).execute(pofInfo)
    }


    @Test
    fun `testApplyPofInfoFromUri without referral`() {
        val clid = "clid"
        val wprid = "wprid"
        val icookie = "icookie"
        val baobabEventId = "baobabEventId"
        val utmSourceService = "utmSourceService"

        val deeplinkUri = Uri.parse(
            "yamarket://home?clid=$clid&wprid=$wprid&icookie=$icookie&baobab_event_id=$baobabEventId&utm_source_service=$utmSourceService",
        )

        val pofInfo = PofInfo(
            pof = clid,
            wprid = wprid,
            icookie = icookie,
            baobabEventId = baobabEventId,
            utmSourceService = utmSourceService,
            lastUpdateTimestamp = currentTime
        )

        useCase.applyPofInfoFromUri(deeplinkUri)
            .test()

        verify(savePofInfoUseCase, times(1)).execute(pofInfo, false)
    }

    @Test
    fun `testApplyPofInfoFromUri with referral`() {
        val clid = "clid"
        val wprid = "wprid"
        val icookie = "icookie"
        val baobabEventId = "baobabEventId"
        val utmSourceService = "utmSourceService"

        val deeplinkUri = Uri.parse(
            "yamarket://home?clid=$clid&wprid=$wprid&icookie=$icookie&baobab_event_id=$baobabEventId&utm_source_service=$utmSourceService",
        )

        val pofInfo = PofInfo(
            pof = clid,
            wprid = wprid,
            icookie = icookie,
            baobabEventId = baobabEventId,
            utmSourceService = utmSourceService,
            lastUpdateTimestamp = currentTime
        )

        useCase.applyPofInfoFromUri(deeplinkUri, true)
            .test()

        verify(savePofInfoUseCase, times(1)).execute(pofInfo, true)
    }


    @Test
    fun testApplyClidInfoFromUri() {
        val clid = "clid"
        val ymclid = "ymclid"
        val mclid = "mclid"
        val vid = "vid"
        val distrType = "distrType"
        val pp = "pp"

        val deeplinkUri = Uri.parse(
            "yamarket://home?clid=$clid&ymclid=$ymclid&mclid=$mclid&vid=$vid&distr_type=$distrType&pp=$pp",
        )

        val clidInfo = ClidInfo(
            clid = clid,
            ymclid = ymclid,
            mclid = mclid,
            vid = vid,
            distrType = distrType,
            lastUpdateTimestamp = currentTime,
            pp = pp,
            sourceType = ClidInfo.SourceType.URI_DEEPLINK
        )

        useCase.applyClidInfoFromUri(deeplinkUri)
            .test()

        verify(saveClidInfoUseCase, times(1)).execute(clidInfo)
    }

    @Test
    fun testCheckIfReferralParamsAreSet() {
        whenever(useCase.checkIfReferralParamsAreSet()) doReturn Single.fromCallable { false }

        useCase.checkIfReferralParamsAreSet()
            .test()

        verify(checkReferralInfoUseCase, times(1)).execute()
    }

}
