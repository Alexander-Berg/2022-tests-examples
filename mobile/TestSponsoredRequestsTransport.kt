package ru.yandex.market.mocks.local.fapi

class TestSponsoredRequestsTransport {

    private val resolveSkuLock = Any()
    private val resolveSkuCpc = mutableSetOf<String>()

    fun registerResolveSkuRequest(
        cpc: String?
    ) {
        if (cpc != null) {
            synchronized(resolveSkuLock) {
                resolveSkuCpc.add(cpc)
            }
        }
    }

    fun checkResolveSkuCpcPassed(
        cpc: String?
    ): Boolean {
        val result = synchronized(resolveSkuLock) {
            resolveSkuCpc.contains(cpc)
        }
        return result
    }

    fun clearResolveSkuRequests() {
        synchronized(resolveSkuLock) {
            resolveSkuCpc.clear()
        }
    }

    private data class AddItemsToCardSponsoredParams(
        val cpaUrl: String?,
        val feeShow: String?,
    )

    private val addItemsToCartLock = Any()
    private val addItemsToCartRequests = mutableSetOf<AddItemsToCardSponsoredParams>()

    fun registerAddItemsToCartRequest(
        cpaUrl: String?,
        feeShow: String?,
    ) {
        val params = AddItemsToCardSponsoredParams(cpaUrl, feeShow)
        synchronized(addItemsToCartLock) {
            addItemsToCartRequests.add(params)
        }
    }

    fun checkAddItemsToCartParamsPassed(
        cpaUrl: String?,
        feeShow: String?,
    ): Boolean {
        val params = AddItemsToCardSponsoredParams(cpaUrl, feeShow)
        val result = synchronized(addItemsToCartLock) {
            addItemsToCartRequests.contains(params)
        }
        return result
    }

    fun clearAddItemsToCartRequests() {
        synchronized(addItemsToCartRequests) {
            addItemsToCartRequests.clear()
        }
    }

}