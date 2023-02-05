package ru.yandex.market.clean.presentation.feature.cms.item.feed.formatter

import com.bumptech.glide.RequestManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.base.presentation.core.mvp.MvpDelegate
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.presentation.feature.cms.item.feed.item.FeedSnippetItem
import ru.yandex.market.clean.presentation.feature.cms.item.feed.model.FeedCmsProductVo
import ru.yandex.market.clean.presentation.feature.cms.item.feed.model.FeedSnippetDelegate
import ru.yandex.market.clean.presentation.feature.cms.item.feed.model.FeedSnippetParams
import ru.yandex.market.clean.presentation.feature.cms.model.CmsProductBuilderVo
import ru.yandex.market.clean.presentation.feature.cms.model.cmsProductVOTestInstance
import ru.yandex.market.clean.presentation.feature.product.stationSubscription.StationSubscriptionButtonPresenter
import ru.yandex.market.clean.presentation.feature.wishlist.wishitem.WishLikeItemPresenter
import ru.yandex.market.feature.constructorsnippetblocks.description.descriptionVoTestInstance
import ru.yandex.market.feature.constructorsnippetblocks.offer.OfferVo
import ru.yandex.market.feature.constructorsnippetblocks.photo.photoVoTestInstance
import ru.yandex.market.ui.view.mvp.cartcounterbutton.CartCounterPresenter
import javax.inject.Provider

class FeedSnippetFormatterTest {

    private val formatter = FeedSnippetFormatter(
        snippetEntityMapper = mock(),
        cartCounterPresenterFactory = mock(),
        wishLikePresenterFactory = mock(),
        stationSubscriptionPresenterProvider = mock()
    )

    private val mvpDelegate = mock<MvpDelegate<*>>()
    private val requestManager = mock<RequestManager>()
    private val cartCounterPresenterProvider = mock<Provider<CartCounterPresenter>>()
    private val wishLikePresenterProvider = mock<Provider<WishLikeItemPresenter>>()
    private val feedSnippetDelegate = mock<FeedSnippetDelegate>()
    private val stationSubscriptionButtonPresenter = mock<Provider<StationSubscriptionButtonPresenter>>()

    private val inputItems = listOf(
        FeedCmsProductVo(
            productVo = cmsProductVOTestInstance(),
            productBuilderVo = CmsProductBuilderVo(
                photoVo = photoVoTestInstance().copy(badges = emptyList()),
                offerVo = OfferVo.FOR_SKELETON_WITH_DISCOUNT,
                descriptionVo = descriptionVoTestInstance(),
                stationSubscriptionOfferItemVo = null,
            ),
            isVisual = false,
        )
    )

    private val expectedResult = listOf(
        FeedSnippetItem(
            parentDelegate = mvpDelegate,
            params = FeedSnippetParams(
                productVo = inputItems.first().productVo,
                productBuilderVo = inputItems.first().productBuilderVo,
                delegate = feedSnippetDelegate,
                requestManager = requestManager,
                cartCounterPresenterProvider = cartCounterPresenterProvider,
                wishLikePresenterProvider = wishLikePresenterProvider,
                stationSubscriptionButtonPresenterProvider = stationSubscriptionButtonPresenter
            ),
            isVisual = false,
        )
    )

    @Test
    fun `check correct formatting`() {
        val actualResult = formatter.format(
            items = inputItems,
            widget = CmsWidget.testInstance(),
            mvpDelegate = mvpDelegate,
            requestManager = requestManager,
            listener = mock(),
        )
        assertThat(actualResult.first().productVo).isEqualTo(expectedResult.first().productVo)
    }
}
