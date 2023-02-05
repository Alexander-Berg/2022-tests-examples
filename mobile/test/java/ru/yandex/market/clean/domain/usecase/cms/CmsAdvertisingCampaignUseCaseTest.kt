package ru.yandex.market.clean.domain.usecase.cms

import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.cms.CmsAdvertisingCampaignItem
import ru.yandex.market.clean.domain.model.cms.garson.AdvertisingCampaignGarson
import ru.yandex.market.clean.domain.model.cms.garson.CmsWidgetGarson
import ru.yandex.market.clean.domain.model.cms.garson.PlusBenefitsGarson
import ru.yandex.market.domain.auth.usecase.GetAuthStatusStreamUseCase
import ru.yandex.market.domain.cashback.model.WelcomeCashbackInfo
import ru.yandex.market.domain.cashback.repository.WelcomeCashbackRepository
import ru.yandex.market.domain.cashback.usecase.GetWelcomeCashbackInfoUseCase
import ru.yandex.market.domain.media.model.MeasuredImageReference
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance
import java.math.BigDecimal

@RunWith(Enclosed::class)
class CmsAdvertisingCampaignUseCaseTest {

    @RunWith(Parameterized::class)
    class ValuesTest(
        private val isAuthorized: Boolean,
        private val hasWidgetInteraction: Boolean,
        private val welcomeCashbackInfo: WelcomeCashbackInfo,
        private val garson: CmsWidgetGarson,
        private val expectedValue: List<CmsAdvertisingCampaignItem>
    ) {
        private val authStatusSubject: PublishSubject<Boolean> = PublishSubject.create()
        private val welcomeCashbackInfoUseCaseSubject: SingleSubject<WelcomeCashbackInfo> = SingleSubject.create()
        private val advertisingCampaignWashInteractedSubject: PublishSubject<Boolean> = PublishSubject.create()

        private val getAuthStatusStreamUseCase = mock<GetAuthStatusStreamUseCase> {
            on { getAuthStatusStream() } doReturn authStatusSubject
        }
        private val getWelcomeCashbackInfoUseCase = mock<GetWelcomeCashbackInfoUseCase> {
            on { execute() } doReturn welcomeCashbackInfoUseCaseSubject
        }
        private val welcomeCashbackRepository = mock<WelcomeCashbackRepository> {
            on { getAdvertisingCampaignWashInteracted() } doReturn advertisingCampaignWashInteractedSubject
        }

        private val useCase = CmsAdvertisingCampaignUseCase(
            welcomeCashbackRepository,
            getAuthStatusStreamUseCase,
            getWelcomeCashbackInfoUseCase
        )

        @Test
        fun test() {
            val testObservable = useCase.execute(garson).test()

            authStatusSubject.onNext(isAuthorized)
            advertisingCampaignWashInteractedSubject.onNext(hasWidgetInteraction)
            welcomeCashbackInfoUseCaseSubject.onSuccess(welcomeCashbackInfo)

            testObservable
                .assertNoErrors()
                .assertNotComplete()
                .assertValue(expectedValue)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                //0
                arrayOf(
                    true,
                    false,
                    WelcomeCashbackInfo(
                        isWelcomeCashbackEmitOrderAvailable = true,
                        cashbackAmount = BigDecimal(500),
                        priceFrom = BigDecimal(3500)
                    ),
                    AdvertisingCampaignGarson(measuredImageReferenceTestInstance()),
                    listOf(
                        CmsAdvertisingCampaignItem(
                            welcomeCashback = BigDecimal(500),
                            orderThreshold = BigDecimal(3500),
                            isAuthorized = true,
                            measuredImageReferenceTestInstance()
                        )
                    )
                ),
                //1
                arrayOf(
                    false,
                    false,
                    WelcomeCashbackInfo(
                        isWelcomeCashbackEmitOrderAvailable = true,
                        cashbackAmount = BigDecimal(400),
                        priceFrom = BigDecimal(4500)
                    ),
                    AdvertisingCampaignGarson(MeasuredImageReference.empty()),
                    listOf(
                        CmsAdvertisingCampaignItem(
                            welcomeCashback = BigDecimal(400),
                            orderThreshold = BigDecimal(4500),
                            isAuthorized = false,
                            MeasuredImageReference.empty()
                        )
                    )
                ),
                //2
                arrayOf(
                    true,
                    true,
                    WelcomeCashbackInfo(
                        isWelcomeCashbackEmitOrderAvailable = true,
                        cashbackAmount = BigDecimal(400),
                        priceFrom = BigDecimal(4500)
                    ),
                    AdvertisingCampaignGarson(measuredImageReferenceTestInstance()),
                    emptyList<CmsAdvertisingCampaignItem>()
                ),
                //3
                arrayOf(
                    false,
                    true,
                    WelcomeCashbackInfo(
                        isWelcomeCashbackEmitOrderAvailable = true,
                        cashbackAmount = BigDecimal.TEN,
                        priceFrom = BigDecimal.TEN
                    ),
                    AdvertisingCampaignGarson(measuredImageReferenceTestInstance()),
                    emptyList<CmsAdvertisingCampaignItem>()
                ),
                //4
                arrayOf(
                    false,
                    false,
                    WelcomeCashbackInfo(
                        isWelcomeCashbackEmitOrderAvailable = false,
                        cashbackAmount = BigDecimal.TEN,
                        priceFrom = BigDecimal.TEN
                    ),
                    AdvertisingCampaignGarson(measuredImageReferenceTestInstance()),
                    emptyList<CmsAdvertisingCampaignItem>()
                ),
                //5
                arrayOf(
                    false,
                    false,
                    WelcomeCashbackInfo(
                        isWelcomeCashbackEmitOrderAvailable = false,
                        cashbackAmount = BigDecimal.TEN,
                        priceFrom = BigDecimal.TEN
                    ),
                    PlusBenefitsGarson(),
                    emptyList<CmsAdvertisingCampaignItem>()
                )
            )
        }
    }

    class ReloadValuesOnUpdateTest {

        private val authStatusSubject: PublishSubject<Boolean> = PublishSubject.create()
        private val advertisingCampaignWashInteractedSubject: PublishSubject<Boolean> = PublishSubject.create()

        private val getAuthStatusStreamUseCase = mock<GetAuthStatusStreamUseCase> {
            on { getAuthStatusStream() } doReturn authStatusSubject
        }
        private val getWelcomeCashbackInfoUseCase = mock<GetWelcomeCashbackInfoUseCase> {
            on { execute() } doReturn Single.just(
                WelcomeCashbackInfo(true, BigDecimal.TEN, BigDecimal.TEN)
            )
        }
        private val welcomeCashbackRepository = mock<WelcomeCashbackRepository> {
            on { getAdvertisingCampaignWashInteracted() } doReturn advertisingCampaignWashInteractedSubject
        }

        private val useCase = CmsAdvertisingCampaignUseCase(
            welcomeCashbackRepository,
            getAuthStatusStreamUseCase,
            getWelcomeCashbackInfoUseCase
        )

        @Test
        fun `Test reload values on updates`() {
            val testObservable = useCase.execute(AdvertisingCampaignGarson(measuredImageReferenceTestInstance())).test()

            //эмит первого значения
            authStatusSubject.onNext(true)
            advertisingCampaignWashInteractedSubject.onNext(false)
            //эмит второго значения
            authStatusSubject.onNext(false)
            //эмит третьего значения
            advertisingCampaignWashInteractedSubject.onNext(true)


            testObservable
                .assertNoErrors()
                .assertNotComplete()
                .assertValueCount(3)
        }
    }
}