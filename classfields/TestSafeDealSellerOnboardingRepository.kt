package ru.auto.ara.core.mocks_and_stubbs

import ru.auto.data.repository.ISafeDealSellerOnboardingRepository
import java.util.*

class TestSafeDealSellerOnboardingRepository : ISafeDealSellerOnboardingRepository {

    private var offerViewsCount: Int = -1
    private var onboardingNextShowDate: Date? = null

    override fun clear() {
        offerViewsCount = -1
        onboardingNextShowDate = null
    }

    override fun getSellerOfferViewsCount(): Int = offerViewsCount

    override fun getOnboardingNextShowDate(): Date?  = onboardingNextShowDate

    override fun setSellerOfferViewsCount(offerViewsCount: Int) {
        this.offerViewsCount = offerViewsCount
    }

    override fun setOnboardingNextShowDate(lastOnboardingShowDate: Date) {
        onboardingNextShowDate = lastOnboardingShowDate
    }

}
