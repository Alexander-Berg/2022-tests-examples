package ru.yandex.market.test.util

import ru.yandex.market.di.TestTransportLogger

typealias AnalyticsChecker = () -> Unit

val noChecksRequired: AnalyticsChecker = { }

fun checkAddToCartEvent(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_ADD_TO_CART)
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.ADJUST_ADD_TO_CART)
    }
}

fun checkViewItem(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_VIEW_ITEM)
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.ADJUST_VIEW_ITEM)
    }
}

fun checkPurchaseWithoutCredit(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_PURCHASE_WITHOUT_CREDIT_MULTIORDER)
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.ADJUST_PURCHASE_WITHOUT_CREDIT_MULTIORDER)
    }
}

fun checkViewSearchResult(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.ADJUST_VIEW_SEARCH_RESULT)
    }
}

fun checkNewCustomer(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.ADJUST_NEW_CUSTOMER)
    }
}

fun checkFirebasePurchase(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_PURCHASE)
    }
}

fun checkFirebaseViewCart(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_VIEW_CART)
    }
}

fun checkFirebaseViewItemList(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_VIEW_ITEM_LIST)
    }
}

fun checkFirebaseAddToWithList(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_ADD_TO_WISH_LIST)
    }
}

fun checkFirebaseEcommercePurchase(times: Int = 1) {
    repeat(times) {
        AnalyticsHelper.checkImportantMetric(TestTransportLogger.FIREBASE_ECOMMERCE_PURCHASE)
    }
}