import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuCellHelpers AutoRuSaleCard
final class SaleCardHeaderTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    private static let searchURI = "POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc"
    private static let offerID = "1101101721-a355a648-sale-card-header"
    private static let cardHeaderID = "header"

    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    // MARK: -

    func test_plainPriceNew() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                offer.section = .new
            }
        }

        let steps = openListing()
            .openCarOffer(with: Self.offerID)

        let screen = steps.onSaleCardScreen()
        Snapshot.compareWithSnapshot(
            image: Snapshot.screenshotCollectionView(
                fromCell: screen.headerView,
                toCell: screen.gallery
            )
        )
    }

    func test_plainPriceUsed() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                offer.section = .used
            }
        }

        let steps = openListing()
            .openCarOffer(with: Self.offerID)

        let screen = steps.onSaleCardScreen()
        Snapshot.compareWithSnapshot(
            image: Snapshot.screenshotCollectionView(
                fromCell: screen.headerView,
                toCell: screen.gallery
            )
        )
    }

    // MARK: -

    func test_plainPriceHasMaxDiscountAllowedForCreditNew() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                offer = offer
                    .addTags(.hasDiscount, .allowedForCredit)
                    .setDiscount(
                        tradein: 170_000,
                        insurance: 50000,
                        credit: 100_000,
                        max: 320_000
                    )
                offer.section = .new
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
            }
        }

        let steps = openListing()
            .openCarOffer(with: Self.offerID)

        let screen = steps.onSaleCardScreen()
        Snapshot.compareWithSnapshot(
            image: Snapshot.screenshotCollectionView(
                fromCell: screen.headerView,
                toCell: screen.gallery
            )
        )
    }

    func test_plainPriceHasMaxDiscountAllowedForCreditNewDealer() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                offer = offer
                    .addTags(.hasDiscount, .allowedForCredit)
                    .setDiscount(
                        tradein: 170_000,
                        insurance: 50000,
                        credit: 100_000,
                        max: 320_000
                    )
                offer.section = .new
                offer.sellerType = .commercial
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
            }
        }

        let steps = openListing()
            .openCarOffer(with: Self.offerID)

        let screen = steps.onSaleCardScreen()
        Snapshot.compareWithSnapshot(
            image: Snapshot.screenshotCollectionView(
                fromCell: screen.headerView,
                toCell: screen.gallery
            )
        )
    }

    func test_plainPriceHasMaxDiscountAllowedForCreditUsed() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                offer = offer
                    .addTags(.hasDiscount, .allowedForCredit)
                    .setDiscount(
                        tradein: 170_000,
                        insurance: 50000,
                        credit: 100_000,
                        max: 320_000
                    )
                offer.section = .used
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
            }
        }

        let steps = openListing()
            .openCarOffer(with: Self.offerID)

        let screen = steps.onSaleCardScreen()
        Snapshot.compareWithSnapshot(
            image: Snapshot.screenshotCollectionView(
                fromCell: screen.headerView,
                toCell: screen.gallery
            )
        )
    }

    func test_plainPriceHasMaxDiscountAllowedForCreditUsedDealer() {
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                offer = offer
                    .addTags(.hasDiscount, .allowedForCredit)
                    .setDiscount(
                        tradein: 170_000,
                        insurance: 50000,
                        credit: 100_000,
                        max: 320_000
                    )
                    .mutate {
                        $0.section = .used
                        $0.sellerType = .commercial
                        $0.dealerCreditConfig = Auto_Api_CreditConfiguration.with({ ( creditInfo) in
                            creditInfo.creditAmountSliderStep = 100
                            creditInfo.creditDefaultTerm = 5
                            creditInfo.creditMaxAmount = 1_000_000
                            creditInfo.creditMinAmount = 100_000
                            creditInfo.creditMinRate = 0.08
                            creditInfo.creditOfferInitialPaymentRate = 0.1
                            creditInfo.creditStep = 100
                            creditInfo.creditTermValues = [1, 2, 3, 4, 5]
                        })
                    }
            }
        }

        let steps = openListing()
            .openCarOffer(with: Self.offerID)

        let screen = steps.onSaleCardScreen()
        Snapshot.compareWithSnapshot(
            image: Snapshot.screenshotCollectionView(
                fromCell: screen.headerView,
                toCell: screen.gallery
            )
        )
    }

    // MARK: -

    private func setupServer() {

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        try! server.start()
    }

    private func openListing() -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
    }

    private static func listingOkResponse(mutation: (inout Auto_Api_Offer) -> Void) -> Response {
        var model: Auto_Api_OfferListingResponse = .init(mockFile: "SaleListHeaderTests_single-offer")
        model.offers[0].id = Self.offerID
        mutation(&model.offers[0])
        return Response.okResponse(message: model)
    }
}
