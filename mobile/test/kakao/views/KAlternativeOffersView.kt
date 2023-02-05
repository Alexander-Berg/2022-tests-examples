package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.screen.item.KAlternativeOfferItem

class KAlternativeOffersView : KBaseView<KTextView> {
    constructor(function: ViewBuilder.() -> Unit) : super(function)
    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    private val alternativeOffersRecyclerView = KRecyclerView({
        withId(R.id.alternativeOffersRecyclerView)
    }, itemTypeBuilder = {
        itemType(::KAlternativeOfferItem)
    })

    fun checkCheapestAlternativeOfferItem(
        supplier: String,
        deliveryDate: String,
        price: Int,
        hasFreeDelivery: Boolean
    ) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasSupplier(supplier)
            hasCheaperDescription(deliveryDate, hasFreeDelivery)
            hasPrice(price)
        }
    }

    fun checkAlternativeOfferFreeDelivery(deliveryDate: String, hasFreeDelivery: Boolean) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasCheaperDescription(deliveryDate, hasFreeDelivery)
        }
    }

    fun clickAlternativeOffer() {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            clickDescription()
        }
    }

    fun checkGiftAlternativeOfferItem(supplier: String, giftTitle: String, price: Int) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasSupplier(supplier)
            hasPrice(price)
            hasGiftDescription(giftTitle)
        }
    }

    fun alternativeOfferClickCartButton() {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            clickCartButton()
        }
    }

    fun alternativeOfferHasInCartButton() {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasCountInCartButton()
        }
    }

    fun checkFasterAlternativeOfferItem(
        supplier: String,
        deliveryDate: String,
        price: Int,
        hasFreeDelivery: Boolean
    ) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasSupplier(supplier)
            hasFasterDescription(deliveryDate, hasFreeDelivery)
            hasPrice(price)
        }
    }

    fun checkPostAlternativeOfferItem(
        supplier: String,
        deliveryDate: String,
        price: Int
    ) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasSupplier(supplier)
            hasPostDescription(deliveryDate)
            hasPrice(price)
        }
    }

    fun checkPickupAlternativeOfferItem(
        supplier: String,
        deliveryDate: String,
        price: Int
    ) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasSupplier(supplier)
            hasPickupDescription(deliveryDate)
            hasPrice(price)
        }
    }

    fun checkCourierAlternativeOfferItem(
        supplier: String,
        deliveryDate: String,
        price: Int
    ) {
        alternativeOffersRecyclerView.firstChild<KAlternativeOfferItem> {
            hasSupplier(supplier)
            hasCourierDescription(deliveryDate)
            hasPrice(price)
        }
    }
}