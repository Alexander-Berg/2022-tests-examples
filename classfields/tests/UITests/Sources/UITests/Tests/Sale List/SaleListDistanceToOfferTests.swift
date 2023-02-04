import AutoRuProtoModels
import SwiftProtobuf
import Snapshots
import XCTest

/// @depends_on AutoRuSaleList AutoRuCellHelpers
final class SaleListDistanceToOfferTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    private static let searchURI = "POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc"
    private static let offerID = "1101101721-a355a648"

    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        mockListingBase()
        mocker.startMock()
    }

    func test_exactDistance() {
        Step("Точное расстояние до оффера") {}
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                let sellerLocation = Auto_Api_DistanceToTarget.with { (distance: inout Auto_Api_DistanceToTarget) in
                    distance.distance = 12
                    distance.regionInfo.genitive = "Зеленогорска"
                }

                offer.seller.location.distanceToSelectedGeo.append(sellerLocation)
            }
        }
        launch()

        openFilters()
            .tapRegionField()
            .toggleExpandRegion(index: 1)
            .toggleSelectCity(regionIndex: 1, index: 1)
            .wait(for: 1)
            .confirmFilters()
            .showResultsTap()
            .wait(for: 1)
            .checkSnippetBodyHasLabel(offer: Self.offerID, text: "Москва (12 км от Зеленогорска)")
    }

    func test_roundDistance() {
        Step("Округляем расстояние до оффера") {}
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                let sellerLocation = Auto_Api_DistanceToTarget.with { (distance: inout Auto_Api_DistanceToTarget) in
                    distance.distance = 112
                    distance.regionInfo.genitive = "Зеленогорска"
                }

                offer.seller.location.distanceToSelectedGeo.append(sellerLocation)
            }
        }
        launch()

        openFilters()
            .tapRegionField()
            .toggleExpandRegion(index: 1)
            .toggleSelectCity(regionIndex: 1, index: 1)
            .wait(for: 1)
            .confirmFilters()
            .showResultsTap()
            .wait(for: 1)
            .checkSnippetBodyHasLabel(offer: Self.offerID, text: "Москва (120 км от Зеленогорска)")
    }

    func test_moreThanRadiusDistance() {
        Step("Расстояние больше радиуса") {}
        server.addHandler(Self.searchURI) { (_, _) -> Response? in
            return Self.listingOkResponse { (offer: inout Auto_Api_Offer) in
                let sellerLocation = Auto_Api_DistanceToTarget.with { (distance: inout Auto_Api_DistanceToTarget) in
                    distance.distance = 333
                    distance.regionInfo.genitive = "Зеленогорска"
                }

                offer.seller.location.distanceToSelectedGeo.append(sellerLocation)
            }
        }
        launch()

        openFilters()
            .tapRegionField()
            .toggleExpandRegion(index: 1)
            .toggleSelectCity(regionIndex: 1, index: 1)
            .wait(for: 1)
            .confirmFilters()
            .showResultsTap()
            .wait(for: 1)
            .checkSnippetBodyHasLabel(offer: Self.offerID, text: "Москва (более 200 км от Зеленогорска)")
    }

    func test_otherCityOffer() {
        Step("Офферы из других городов") {}
        let sellerLocation = Auto_Api_DistanceToTarget.with { (distance: inout Auto_Api_DistanceToTarget) in
            distance.distance = 333
            distance.regionInfo.genitive = "Зеленогорска"
        }
        mocker
            .mock_searchCars(newCount: 0, usedCount: 0, distances: [.init(radius: 400, count: 1, distanceToOffer: sellerLocation)])
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1, distanceToOffer: sellerLocation)])
        launch()

        openFilters()
            .tapRegionField()
            .toggleExpandRegion(index: 1)
            .toggleSelectCity(regionIndex: 1, index: 1)
            .wait(for: 1)
            .confirmFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToOffer(with: "400_0", position: .footer)
            .checkSnippetBodyHasLabel(offer: "400_0", text: "Тотьма (333 км от Зеленогорска)")
    }

    // MARK: -

    private func openFilters() -> FiltersSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
    }

    private static func listingOkResponse(mutation: (inout Auto_Api_Offer) -> Void) -> Response {
        var model: Auto_Api_OfferListingResponse = .init(mockFile: "SaleListHeaderTests_single-offer")
        mutation(&model.offers[0])
        return Response.okResponse(message: model)
    }

    private func mockListingBase() {
        mocker
            .mock_base()
            .mock_user()
            .mock_searchCarsSpecials()
            .mock_searchCarsContextRecommendNewInStock()
            .mock_searchMoto()
            .mock_searchTrucks()
            .mock_searchCarsRelated()
            .mock_referenceCatalogCarConfigurationsSubtree()
            .mock_referenceCatalogCarsAllOptions()
            .mock_reviewsAutoListing()
            .mock_videoSearchCars()
    }
}
