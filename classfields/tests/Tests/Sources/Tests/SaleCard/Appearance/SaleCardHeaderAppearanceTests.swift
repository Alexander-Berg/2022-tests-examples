import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import AutoRuUtils
import AutoRuFetchableImage
import AutoRuSaleCardSharedUI
import Snapshots
@testable import AutoRuYogaLayout
import AutoRuColorSchema
import Foundation
@testable import AutoRuSaleCardSharedModels

final class SaleCardHeaderAppearanceTests: BaseUnitTest {
    override func setUp() {
        super.setUp()
        self.setReplaceImagesWithStub(nil)
        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()
        self.setReplaceImagesDefaultBehavior()
        FetchableImage.blockThreadUntilFinished = false
    }

    private static let model = SaleCardHeaderModel(
        rubPrice: 100_500,
        creditParam: nil,
        previousRubPrice: nil,
        priceBadge: nil,
        title: "Land Rover Range Rover Evoque I, 2017",
        canDisplayPriceChange: false,
        status: .common,
        maxDiscount: 0,
        userSaleCardMode: false,
        isUsedSaleFromDealer: false,
        withNds: false,
        bookingInfo: nil,
        isMyOffer: false,
        safeDealRequestStatus: nil
    )

    func test_onlyPrice() {
        Self.compareWithSnapshot(model: Self.model, identifier: "only_price")
    }

    func test_priceBadge() {
        var model = Self.model

        model.priceBadge = .excellentPrice
        Self.compareWithSnapshot(model: model, identifier: "price_badge_excellent")

        model.priceBadge = .goodPrice
        Self.compareWithSnapshot(model: model, identifier: "price_badge_good")
    }

    func test_status() {
        var model = Self.model

        model.status = .sold
        Self.compareWithSnapshot(model: model, identifier: "status_sold")
    }

    func test_creditBadge() {
        var model = Self.model
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098252972-99d8c274_dealer_ok")
        var offer = offerResponse.offer
        let product = Vertis_Shark_CreditProduct()
        let bank = Vertis_Shark_Bank.with {
            $0.logo56X56RoundURL = "https://www.placeholder.com/image.png" // placeholder url
        }
        offer.dealerCreditConfig = Auto_Api_CreditConfiguration.with({ ( creditInfo) in
            creditInfo.creditAmountSliderStep = 100
            creditInfo.creditDefaultTerm = 5
            creditInfo.creditMaxAmount = 1_000_000
            creditInfo.creditMinAmount = 100_000
            creditInfo.creditMinRate = 0.08
            creditInfo.creditOfferInitialPaymentRate = 0.1
            creditInfo.creditStep = 100
            creditInfo.creditTermValues = [1, 2, 3, 4, 5]
        })
        model.creditParam = offer.dealerCreditInfo?.possibleCreditParam
        model.onCreditTap = { }
        Self.compareWithSnapshot(model: model, identifier: "credit_badge")
    }

    func test_changeDownPrice() {
        var model = Self.model

        model.previousRubPrice = 200_000
        model.canDisplayPriceChange = true
        Self.compareWithSnapshot(model: model, identifier: "price_change_down")

        model.isUsedSaleFromDealer = true
        Self.compareWithSnapshot(model: model, identifier: "price_change_down_arrow")
    }

    func test_changeUpPrice() {
        var model = Self.model

        model.previousRubPrice = 90_000
        model.canDisplayPriceChange = true
        Self.compareWithSnapshot(model: model, identifier: "price_change_up")

        model.isUsedSaleFromDealer = true
        Self.compareWithSnapshot(model: model, identifier: "price_change_up_arrow")
    }

    func test_dealerOffer() {
        var model = Self.model

        model.maxDiscount = 10_000

        model.status = .new
        Self.compareWithSnapshot(model: model, identifier: "dealer_offer_discount_new")

        model.isUsedSaleFromDealer = true
        Self.compareWithSnapshot(model: model, identifier: "dealer_offer_discount")

        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098252972-99d8c274_dealer_ok")
        var offer = offerResponse.offer
        let product = Vertis_Shark_CreditProduct()
        let bank = Vertis_Shark_Bank.with {
            $0.logo56X56RoundURL = "https://www.placeholder.com/image.png" // placeholder url
        }
        offer.dealerCreditConfig = Auto_Api_CreditConfiguration.with({ ( creditInfo) in
            creditInfo.creditAmountSliderStep = 100
            creditInfo.creditDefaultTerm = 5
            creditInfo.creditMaxAmount = 1_000_000
            creditInfo.creditMinAmount = 100_000
            creditInfo.creditMinRate = 0.08
            creditInfo.creditOfferInitialPaymentRate = 0.1
            creditInfo.creditStep = 100
            creditInfo.creditTermValues = [1, 2, 3, 4, 5]
        })
        model.creditParam = offer.dealerCreditInfo?.possibleCreditParam

        model.onCreditTap = { }
        Self.compareWithSnapshot(model: model, identifier: "dealer_offer_credit")
    }

    func test_withNDS() {
        var model = Self.model

        model.withNds = true
        Self.compareWithSnapshot(model: model, identifier: "with_nds")
    }

    func test_booking() {
        var model = Self.model

        let endDate = Date(timeIntervalSinceReferenceDate: 60 * 60 * 24 * 10000)
        model.bookingInfo = .init(byMe: false, endDate: endDate)
        Self.compareWithSnapshot(model: model, identifier: "booking")

        model.bookingInfo = .init(byMe: true, endDate: endDate)
        Self.compareWithSnapshot(model: model, identifier: "booking_by_me")

        let todayEndDate = Date(timeIntervalSinceNow: 100)
        model.bookingInfo = .init(byMe: true, endDate: todayEndDate)
        Self.compareWithSnapshot(model: model, identifier: "booking_last_day")
    }

    func test_safeDeal() {
        var model = Self.model

        model.onSafeDealCancelTap = { }

        model.safeDealRequestStatus = .accepted
        Self.compareWithSnapshot(model: model, identifier: "safe_deal_accepted")

        model.safeDealRequestStatus = .rejected
        Self.compareWithSnapshot(model: model, identifier: "safe_deal_rejected")

        model.safeDealRequestStatus = .awaiting
        Self.compareWithSnapshot(model: model, identifier: "safe_deal_awaiting")
    }

    // MARK: - Private

    private static func compareWithSnapshot(model: SaleCardHeaderModel, identifier: String) {
        Snapshot.compareWithSnapshot(
            layout: SaleCardHeaderLayout(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: identifier
        )
    }
}
