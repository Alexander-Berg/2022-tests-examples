package ru.yandex.metro.preferences.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.reactivex.Completable
import io.reactivex.Observable
import ru.yandex.metro.ClassSpek
import ru.yandex.metro.scheme.domain.PreferredSchemeComponentProvider
import ru.yandex.metro.scheme.domain.SchemeInvalidationNotifier
import ru.yandex.metro.scheme.domain.SchemeRepository
import ru.yandex.metro.scheme.domain.SchemeSelectionUseCase
import ru.yandex.metro.scheme.domain.di.SchemeComponent
import ru.yandex.metro.scheme.domain.model.CountryCode
import ru.yandex.metro.scheme.domain.model.CountryGroupByData
import ru.yandex.metro.scheme.domain.model.Scheme
import ru.yandex.metro.scheme.domain.model.SchemeId
import ru.yandex.metro.scheme.domain.model.SchemeList
import ru.yandex.metro.scheme.domain.model.SchemeListByData
import ru.yandex.metro.scheme.domain.model.SchemeSummary
import ru.yandex.metro.scheme.domain.model.SchemeSummaryByData
import ru.yandex.metro.utils.deeplink.usecase.ChangeSchemeUseCase

private val RETURNED_SCHEME_SUMMARY: SchemeSummary = SchemeSummaryByData(
        id = SchemeId("2"),
        name = mock { },
        version = "13.0",
        aliases = listOf("spb"),
        countryCode = CountryCode("ru"),
        geoRegion = mock { },
        logoUrl = null,
        updateInfo = null,
        isLocal = true,
        defaultAlias = "saint-petersburg"
)

private val RETURNED_SCHEMES: SchemeList = SchemeListByData(listOf(
        CountryGroupByData(
                CountryCode("ru"),
                listOf(RETURNED_SCHEME_SUMMARY)
        )
))

private val RETURNED_CURRENT_SCHEME = Scheme(SchemeId("1"), null)

class PreferredSchemeByAliasSpec : ClassSpek(ChangeSchemeUseCase::class.java, {
    val schemeSelectionUseCase by memoized {
        mock<SchemeSelectionUseCase> {
            on { setPreferredSchemeIdAndCheckForUpdate(any()) } doReturn Completable.complete()
        }
    }

    val schemeRepository by memoized {
        mock<SchemeRepository> {
            on { schemeList() } doReturn Observable.just(RETURNED_SCHEMES)
        }
    }

    val preferredSchemeComponentProvider by memoized {
        val preferredSchemeComponent = mock<SchemeComponent> {
            on { scheme() } doReturn RETURNED_CURRENT_SCHEME
        }
        mock<PreferredSchemeComponentProvider> {
            on { preferredSchemeComponent() } doReturn Observable.just(preferredSchemeComponent)
        }
    }

    val schemeInvalidationNotifier by memoized {
        mock<SchemeInvalidationNotifier> {
            on { schemeInvalidationEvents() } doReturn Observable.just(Unit)
        }
    }

    context("scheme repository has scheme for requested alias") {
        val usecase = ChangeSchemeUseCase(
                preferredSchemeComponentProvider,
                schemeRepository,
                schemeSelectionUseCase,
                schemeInvalidationNotifier
        )
        val tested = usecase.selectScheme("spb").test()

        it("should complete") {
            tested.assertComplete()
        }
    }

    context("scheme repository doesn't have scheme for requested alias") {
        val usecase = ChangeSchemeUseCase(
                preferredSchemeComponentProvider,
                schemeRepository,
                schemeSelectionUseCase,
                schemeInvalidationNotifier
        )
        val tested = usecase.selectScheme("moscow").test()

        it("shouldn't complete") {
            tested.assertNotComplete()
            tested.assertNoErrors()
        }
    }
})
