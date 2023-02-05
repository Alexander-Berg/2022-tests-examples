package ru.yandex.market.di

import android.os.Bundle
import ru.yandex.market.analytics.MetricTransport
import ru.yandex.market.analytics.logger.TransportLogger

class TestTransportLogger : TransportLogger {

    private val events: MutableList<String> = mutableListOf()
    private val lock = Object()

    fun checkEvent(eventName: String) {
        synchronized(lock) {
            events.firstOrNull { it == eventName }.let(events::remove)
        }
    }

    fun assertAllImportantEventsChecked() {
        val notCheckedEvents = events.filter { MUST_BE_CHECKED_EVENT_NAMES.contains(it) }

        require(notCheckedEvents.isEmpty()) {
            "Failed to finish test cause events that should be checked are not checked. Override analyticsChecker in " +
                    "test for it. List of unchecked events: $notCheckedEvents"
        }

    }

    override fun log(
        transport: MetricTransport,
        message: String,
        vararg args: String
    ) {
        synchronized(lock) {
            if (transport != MetricTransport.HEALTH) {
                //пока не сделаем универсального транопорта берем первое значение, которое сейчас = имени метрики
                events.add(args.first())
            }
        }
    }

    override fun toJsonString(bundle: Bundle): String = ""

    override fun toJsonString(map: Map<String, Any?>): String = ""

    override fun saveDebugLogEvent(transport: MetricTransport, message: String, args: Array<out String>) = Unit

    companion object {
        //https://st.yandex-team.ru/BLUEMARKETAPPS-29897

        const val FIREBASE_ADD_TO_CART = "add_to_cart"
        const val FIREBASE_VIEW_ITEM = "view_item"
        const val FIREBASE_PURCHASE_WITHOUT_CREDIT_MULTIORDER = "purchase_without_credit_multiorder"
        const val FIREBASE_PURCHASE = "purchase"
        const val FIREBASE_VIEW_CART = "view_cart"
        const val FIREBASE_VIEW_ITEM_LIST = "view_item_list"
        const val FIREBASE_ADD_TO_WISH_LIST = "add_to_wishlist"
        const val FIREBASE_ECOMMERCE_PURCHASE = "ecommerce_purchase"

        const val ADJUST_ADD_TO_CART = "2yabky"
        const val ADJUST_VIEW_ITEM = "coh39b"
        const val ADJUST_PURCHASE_WITHOUT_CREDIT_MULTIORDER = "jk30lx"
        const val ADJUST_VIEW_SEARCH_RESULT = "4cmnwd"
        const val ADJUST_NEW_CUSTOMER = "v1jhpk"

        private val MUST_BE_CHECKED_EVENT_NAMES = setOf(
            FIREBASE_ADD_TO_CART,
            FIREBASE_VIEW_ITEM,
            FIREBASE_PURCHASE_WITHOUT_CREDIT_MULTIORDER,
            FIREBASE_PURCHASE,
            FIREBASE_VIEW_CART,
            FIREBASE_VIEW_ITEM_LIST,
            FIREBASE_ADD_TO_WISH_LIST,
            FIREBASE_ECOMMERCE_PURCHASE,

            ADJUST_ADD_TO_CART,
            ADJUST_VIEW_ITEM,
            ADJUST_PURCHASE_WITHOUT_CREDIT_MULTIORDER,
            ADJUST_VIEW_SEARCH_RESULT,
            ADJUST_NEW_CUSTOMER
        )
    }
}