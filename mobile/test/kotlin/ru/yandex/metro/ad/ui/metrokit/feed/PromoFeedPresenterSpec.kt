package ru.yandex.metro.ad.ui.metrokit.feed

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import ru.yandex.metro.ClassSpek
import ru.yandex.metro.ad.domain.metrokit.AdsFeedSession
import ru.yandex.metro.ad.domain.metrokit.PromoFeedSessionProvider
import ru.yandex.metro.ad.domain.metrokit.model.promo.AdsFeedItem
import ru.yandex.metro.ad.domain.metrokit.model.promo.PromoId
import ru.yandex.metro.ad.domain.metrokit.model.promo.PromoSummary
import ru.yandex.metro.ad.ui.metrokit.feed.list.PromoFeedItem
import ru.yandex.metro.common.domain.ConfigurationManager
import ru.yandex.metro.dialog.ui.openers.RateAppScreenOpener
import ru.yandex.metro.scheme.domain.focusrect.SchemeFocusRectUseCase
import ru.yandex.metro.utils.android.recyclerview.ItemWithPosition
import ru.yandex.metro.utils.android.rx.CustomRxPlugins

private val PROMO_ID = PromoId("1")
private const val PROMO_POSITION = 0

private val PROMO_ITEM = ItemWithPosition(
        position = PROMO_POSITION,
        item = PromoFeedItem(
                AdsFeedItem(
                        PromoSummary(PROMO_ID, "Burger King"),
                        mock { },
                        mock { },
                        true
                )
        )
)

class PromoFeedPresenterSpec : ClassSpek(PromoFeedPresenter::class.java, {
    RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    CustomRxPlugins.setImmediateMainThreadSchedulerHandler(Function { Schedulers.trampoline() })

    val navigator by memoized { mock<PromoFeedNavigator> { } }

    context("ad was shown") {
        val session = mock<AdsFeedSession> {
            on { getItems() } doReturn Maybe.just(emptyList())
            on { reportShow(any()) } doReturn Completable.complete()
        }
        val promoFeedSessionProvider by memoized {
            mock<PromoFeedSessionProvider> {
                on { session() } doReturn Observable.just(session)
            }
        }

        val schemeFocusRectUseCase by memoized {
            mock<SchemeFocusRectUseCase> {
                on { pushNewSubtrahend(any(), any()) } doReturn Disposables.empty()
            }
        }

        val configurationManager by memoized {
            mock<ConfigurationManager> {
                on { isTwoPanes() } doReturn false
            }
        }

        val rateAppScreenOpener by memoized {
            mock<RateAppScreenOpener>()
        }

        val presenter by memoized {
            PromoFeedPresenter(
                    promoFeedSessionProvider,
                    schemeFocusRectUseCase,
                    navigator,
                    rateAppScreenOpener,
                    configurationManager,
                    mock()
            )
        }

        context("view sent intent `promo shown`") {
            val view by memoized {
                mock<PromoFeedView> {
                    on { shownPromo() } doReturn Observable.just(PROMO_ITEM)
                    on { closeIntent() } doReturn Observable.never<Unit>()
                    on { openPromoDetails() } doReturn Observable.never()
                    on { collapseIntent() } doReturn Observable.never<Unit>()
                    on { cardOffsets() } doReturn Observable.empty()
                    on { subtractFromSchemeFocusRectIntent()} doReturn  Observable.never()
                }
            }

            beforeEachTest {
                presenter.attachView(view)
            }

            afterEachTest {
                presenter.detachView()
            }

            it("should report to session") {
                verify(session, times(1)).reportShow(eq(PROMO_ID))
            }
        }
    }
})
