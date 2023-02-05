package ru.yandex.market.clean.data.store

import org.junit.Test
import ru.yandex.market.clean.data.model.DetailedSkuSnapshot
import ru.yandex.market.clean.domain.model.offerAffectingInformationTestInstance
import ru.yandex.market.domain.product.model.SkuId
import ru.yandex.market.clean.domain.model.sku.detailedSkuTestInstance

class DetailedSkuCacheDataStoreTest {

    private val dataStore = DetailedSkuCacheDataStore()

    @Test
    fun `Subscriber got new emit if there was no entity before`() {
        val affectingInfo = offerAffectingInformationTestInstance()
        val skuId = SkuId("skuId", null, null)
        val sku = detailedSkuTestInstance(skuId = skuId)

        val testObserver = dataStore.getCachedSku(affectingInfo, skuId).test()

        dataStore.putSku(affectingInfo, sku, false)

        testObserver
            .assertValue(DetailedSkuSnapshot(sku, false))
            .assertNotTerminated()
    }

    @Test
    fun `Check BehaviourSubject`() {
        val affectingInfo = offerAffectingInformationTestInstance()
        val skuId = SkuId("skuId", null, null)
        val sku = detailedSkuTestInstance(skuId = skuId)

        dataStore.putSku(affectingInfo, sku, false)

        dataStore.getCachedSku(affectingInfo, skuId).test()
            .assertValue(DetailedSkuSnapshot(sku, false))
            .assertNotTerminated()
    }

    @Test
    fun `Check distinctUntilChanged`() {
        val affectingInfo = offerAffectingInformationTestInstance()
        val skuId = SkuId("skuId", null, null)
        val sku = detailedSkuTestInstance(skuId = skuId)

        val testObserver = dataStore.getCachedSku(affectingInfo, skuId).test()

        dataStore.putSku(affectingInfo, sku, false)
        dataStore.putSku(affectingInfo, sku, false)

        testObserver
            .assertValueCount(1)
            .assertNotTerminated()
    }

    @Test
    fun `Subscriber got prev emit if current is not actualized but prev was actualized`() {
        val affectingInfo = offerAffectingInformationTestInstance()
        val skuId = SkuId("skuId", null, null)
        val sku = detailedSkuTestInstance(skuId = skuId, rating = 9f)
        val sku2 = detailedSkuTestInstance(skuId = skuId, rating = 10f)

        val testObserver = dataStore.getCachedSku(affectingInfo, skuId).test()

        dataStore.putSku(affectingInfo, sku, true)
        dataStore.putSku(affectingInfo, sku2, false)

        testObserver
            .assertValue(DetailedSkuSnapshot(sku, true))
            .assertNotTerminated()
    }

    @Test
    fun `Subscriber got prev and current emit if current is actualized and prev was actualized`() {
        val affectingInfo = offerAffectingInformationTestInstance()
        val skuId = SkuId("skuId", null, null)
        val sku = detailedSkuTestInstance(skuId = skuId, rating = 9f)
        val sku2 = detailedSkuTestInstance(skuId = skuId, rating = 10f)

        val testObserver = dataStore.getCachedSku(affectingInfo, skuId).test()

        dataStore.putSku(affectingInfo, sku, true)
        dataStore.putSku(affectingInfo, sku2, true)

        testObserver
            .assertValues(
                DetailedSkuSnapshot(sku, true),
                DetailedSkuSnapshot(sku2, true)
            )
            .assertNotTerminated()
    }

    @Test
    fun `Subscriber got prev and current emit if current is not actualized and prev was not actualized`() {
        val affectingInfo = offerAffectingInformationTestInstance()
        val skuId = SkuId("skuId", null, null)
        val sku = detailedSkuTestInstance(skuId = skuId, rating = 9f)
        val sku2 = detailedSkuTestInstance(skuId = skuId, rating = 10f)

        val testObserver = dataStore.getCachedSku(affectingInfo, skuId).test()

        dataStore.putSku(affectingInfo, sku, false)
        dataStore.putSku(affectingInfo, sku2, false)

        testObserver
            .assertValues(
                DetailedSkuSnapshot(sku, false),
                DetailedSkuSnapshot(sku2, false)
            )
            .assertNotTerminated()
    }
}