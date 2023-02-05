package ru.yandex.yandexbus.inhouse.service.taxi.uber

import com.uber.sdk.rides.client.model.PriceEstimate
import com.uber.sdk.rides.client.model.PriceEstimatesResponse
import com.uber.sdk.rides.client.model.TimeEstimate
import com.uber.sdk.rides.client.model.TimeEstimatesResponse

class TestPriceEstimatesResponse(private val _prices: List<PriceEstimate>) : PriceEstimatesResponse() {

    constructor(estimate: PriceEstimate) : this(listOf(estimate))

    override fun getPrices(): MutableList<PriceEstimate> {
        return _prices.toMutableList()
    }
}

class TestTimeEstimatesResponse(private val _estimations: List<TimeEstimate>) : TimeEstimatesResponse() {

    constructor(estimate: TimeEstimate) : this(listOf(estimate))

    override fun getTimes(): MutableList<TimeEstimate> {
        return _estimations.toMutableList()
    }
}

class TestPriceEstimate(
    private val lowEstimate: Int? = null,
    private val highEstimate: Int? = null,
    private val estimate: String? = null,
    private val currencyCode: String? = null,
    private val productId: String? = null,
    private val duration: Int? = null,
    private val displayName: String? = null,
    private val distance: Float? = null,
    private val surgeMultiplier: Float = 0F
) : PriceEstimate() {

    constructor(lowEstimate: Int, highEstimate: Int, estimate: String, currencyCode: String)
            : this(lowEstimate, highEstimate, estimate, currencyCode, null)

    override fun getProductId() = productId

    override fun getLowEstimate() = lowEstimate

    override fun getHighEstimate() = highEstimate

    override fun getEstimate() = estimate

    override fun getCurrencyCode() = currencyCode

    override fun getDuration() = duration

    override fun getDisplayName() = displayName

    override fun getDistance() = distance

    override fun getSurgeMultiplier() = surgeMultiplier
}

class TestTimeEstimate(
    private val estimate: Int? = null,
    private val productId: String? = null,
    private val displayName: String? = null
) : TimeEstimate() {
    override fun getProductId() = productId

    override fun getEstimate() = estimate

    override fun getDisplayName() = displayName
}