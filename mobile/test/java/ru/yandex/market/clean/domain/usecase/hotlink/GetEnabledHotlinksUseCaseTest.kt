package ru.yandex.market.clean.domain.usecase.hotlink

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.cms.CmsHotLink
import ru.yandex.market.clean.domain.model.express.EntryPoints
import ru.yandex.market.clean.domain.model.lavka2.LavkaEnabledInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaStartupInfo
import ru.yandex.market.clean.domain.usecase.express.GetExpressEntryPointsInfoUseCase
import ru.yandex.market.clean.domain.usecase.express.ObserveIsExpressEntryPointsUseCase
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.EXPRESS_DISABLED_ENTRYPOINT
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.EXPRESS_ENABLED_ENTRYPOINT
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.HOTLINKS
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.LAVKA_STARTUP_INFO_AVAILABLE
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.LAVKA_STARTUP_INFO_UNAVAILABLE
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.WITHOUT_EXPRESS_GROCERIES_SUPERMARKET_HOT_LINKS_PREDICATE
import ru.yandex.market.clean.domain.usecase.hotlink.GetEnabledHotlinksUseCaseTestEntity.WITHOUT_SUPERMARKET_HOT_LINKS_PREDICATE
import ru.yandex.market.clean.domain.usecase.lavka2.GetLavkaStartupInfoUseCase
import ru.yandex.market.clean.domain.usecase.lavka2.LavkaEnabledStatusUseCase
import ru.yandex.market.mockResult
import ru.yandex.market.optional.Optional

@RunWith(Parameterized::class)
class GetEnabledHotlinksUseCaseTest(
    private val lavkaStartupInfo: LavkaStartupInfo,
    private val expressEntryPoints: EntryPoints,
    private val predicate: (CmsHotLink) -> Boolean
) {

    private val lavkaEnabledStatusUseCase = mock<LavkaEnabledStatusUseCase>()
    private val lavkaStartupInfoUseCase = mock<GetLavkaStartupInfoUseCase>()
    private val observeExpressEntryPointsInfoUseCase = mock<ObserveIsExpressEntryPointsUseCase>()
    private val getExpressEntryPointsInfoUseCase = mock<GetExpressEntryPointsInfoUseCase>()
    private val useCase = GetEnabledHotlinksUseCase(
        lavkaEnabledStatusUseCase,
        lavkaStartupInfoUseCase,
        observeExpressEntryPointsInfoUseCase,
        getExpressEntryPointsInfoUseCase
    )

    @Test
    fun check() {
        lavkaEnabledStatusUseCase.isLavkaNativeEnabled().mockResult(Observable.just(LAVKA_ENABLED_INFO))

        lavkaStartupInfoUseCase.execute().mockResult(Single.just(Optional.of(lavkaStartupInfo)))

        observeExpressEntryPointsInfoUseCase.execute().mockResult(
            Observable.just(expressEntryPoints.expressDelivery.isEnabled)
        )

        getExpressEntryPointsInfoUseCase.execute().mockResult(Single.just(expressEntryPoints))

        val expectedResult = HOTLINKS.filterNot(predicate)

        useCase.getEnabledHotlinks(HOTLINKS)
            .test()
            .assertNoErrors()
            .assertResult(expectedResult)

        verify(lavkaEnabledStatusUseCase).isLavkaNativeEnabled()
        verify(lavkaStartupInfoUseCase).execute()
        verify(observeExpressEntryPointsInfoUseCase).execute()
        verify(getExpressEntryPointsInfoUseCase).execute()
    }

    private companion object {

        val LAVKA_ENABLED_INFO = LavkaEnabledInfo(isEnabled = true, isLavkaInMarketEnabled = true)

        @Parameterized.Parameters
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(
                LAVKA_STARTUP_INFO_AVAILABLE,
                EXPRESS_ENABLED_ENTRYPOINT,
                WITHOUT_SUPERMARKET_HOT_LINKS_PREDICATE
            ),

            arrayOf(
                LAVKA_STARTUP_INFO_UNAVAILABLE,
                EXPRESS_DISABLED_ENTRYPOINT,
                WITHOUT_EXPRESS_GROCERIES_SUPERMARKET_HOT_LINKS_PREDICATE
            ),
        )
    }
}