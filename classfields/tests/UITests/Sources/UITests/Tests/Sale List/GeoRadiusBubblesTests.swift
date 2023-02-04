import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuServices AutoRuSaleList
final class GeoRadiusBubblesTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    private let distances: [SearchMockerLocatorCounterDistance] = [
        .init(radius: 0, count: 1),
        .init(radius: 100, count: 1),
        .init(radius: 400, count: 4),
        .init(radius: 800, count: 10),
        .init(radius: 1000, count: 10),
        .init(radius: 1100, count: 20)
    ]

    override func setUp() {
        super.setUp()

        mockListingBase()
        mocker.startMock()
    }

    func test_saleList_geoRadiusExists_100() {
        mocker
            .mock_searchHistory(geoRadius: 100)
            .mock_searchCars(newCount: 0, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                checkGeoRadiusBubblesForRadius100(cell: cell)
            }
    }

    func test_saleList_geoRadiusExists_changeGeoRadiusOnTap() {
        mocker
            .mock_searchHistory(geoRadius: 1000)
            .mock_searchCars(newCount: 0, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        let request0Radius = expectationForRequest { request -> Bool in
            request.method == "POST"
                && request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc"
                && Self.checkSearchParamsForGeoRadius(request.messageBody!)
        }

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell
                    .focus(on: .radiusBubble(1000), ofType: .geoRadiusBubbleItem) { bubble in
                        bubble
                            .shouldBeActive(true)
                            .should(.title, .contain("+ 1000"))
                    }
                    .scroll(to: .currentRegionBubble, direction: .right, maxSwipes: 2)
                    .focus(on: .currentRegionBubble, ofType: .geoRadiusBubbleItem) { bubble in
                        bubble
                            .shouldBeActive(false)
                            .should(.title, .match("Нерехта"))
                            .tap()
                    }
            }

        wait(for: [request0Radius], timeout: 5)
    }

    func test_saleList_geoRadiusExistsForMoto_changeGeoRadiusOnTap() {
        mocker
            .mock_searchHistory(geoRadius: 1000, isCar: false)
            .mock_searchCars(newCount: 0, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        let request0Radius = expectationForRequest { request -> Bool in
            request.method == "POST"
                && request.uri == "/search/moto?context=listing&page=1&page_size=20&sort=cr_date-desc"
                && Self.checkSearchParamsForGeoRadius(request.messageBody!)
        }

        openListingFromHistory(for: "ABM Pegas 200")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell
                    .scroll(to: .currentRegionBubble, direction: .right, maxSwipes: 2)
                    .focus(on: .currentRegionBubble, ofType: .geoRadiusBubbleItem) { bubble in
                        bubble.shouldBeActive(false).tap()
                    }
            }

        wait(for: [request0Radius], timeout: 5)
    }

    func test_saleList_geoRadiusExistsForCommercial_changeGeoRadiusOnTap() {
        mocker
            .mock_searchHistory(geoRadius: 1000, isCar: false)
            .mock_searchCars(newCount: 0, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        let request0Radius = expectationForRequest { request -> Bool in
            return request.method == "POST"
                && request.uri == "/search/trucks?context=listing&page=1&page_size=20&sort=cr_date-desc"
                && Self.checkSearchParamsForGeoRadius(request.messageBody!)
        }

        openListingFromHistory(for: "Chevrolet 3000-Series")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell
                    .scroll(to: .currentRegionBubble, direction: .right, maxSwipes: 2)
                    .focus(on: .currentRegionBubble, ofType: .geoRadiusBubbleItem) { bubble in
                        bubble.shouldBeActive(false).tap()
                    }
            }

        wait(for: [request0Radius], timeout: 5)
    }

    func test_saleList_scrollToActiveOnOpen() {
        mocker
            .mock_searchHistory(geoRadius: 500)
            .mock_searchCars(newCount: 0, usedCount: 10)
            .mock_searchOfferLocatorCounters(type: .all)

        launch()

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell.should(.radiusBubble(500), .exist)
            }
            .swipeUp()
            .swipeUp()
            .swipeDown()
            .swipeDown()
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell
                    .should(.radiusBubble(500), .exist)
                    .tap(.radiusBubble(1000))
                    .should(.radiusBubble(500), .exist)
            }
    }

    func test_saleList_geoRadiusDoesNotExists_russia() {
        let distances = self.distances.filter { $0.radius != 1100 } // без чипса "Россия"

        mocker
            .mock_searchHistory(geoRadius: 100)
            .mock_searchCars(newCount: 0, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell.should(.wholeRussiaBubble, .be(.hidden))
            }
    }

    func test_saleList_geoRadiusExists_changeGeoToRussia() {
        mocker
            .mock_searchHistory(geoRadius: 100)
            .mock_searchCars(newCount: 0, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        let requestRussia = expectationForRequest { request -> Bool in
            if request.method == "POST"
                && request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                return req.geoRadius == 0 && req.rid == [225]
            }
            return false
        }

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell.focus(on: .wholeRussiaBubble, ofType: .geoRadiusBubbleItem) { bubble in
                    bubble
                        .shouldBeActive(false)
                        .should(.title, .match("Россия"))
                        .tap()
                }
            }

        wait(for: [requestRussia], timeout: 5)
    }

    func test_stockCard_geoRadiusExists_100() {
        mocker
            .mock_searchHistory(geoRadius: 100, state: .new)
            .mock_searchCars(newCount: 2, usedCount: 0, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                checkGeoRadiusBubblesForRadius100(cell: cell)
            }
    }

    func test_stockCard_geoRadiusExists_changeGeoRadiusOnTap() {
        mocker
            .mock_searchHistory(geoRadius: 1000, state: .new)
            .mock_searchCars(newCount: 10, usedCount: 10, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        let request0Radius = expectationForRequest { request -> Bool in
            request.method == "POST"
                && request.uri == "/search/cars?context=group_card&page=1&page_size=20&sort=fresh_relevance_1-desc"
                && Self.checkSearchParamsForGeoRadius(request.messageBody!)
        }

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell
                    .focus(on: .radiusBubble(1000), ofType: .geoRadiusBubbleItem) { bubble in
                        bubble.shouldBeActive(true)
                    }
                    .scroll(to: .currentRegionBubble, direction: .right, maxSwipes: 2)
                    .focus(on: .currentRegionBubble, ofType: .geoRadiusBubbleItem) { bubble in
                        bubble.shouldBeActive(false).tap()
                    }
            }

        wait(for: [request0Radius], timeout: 5)
    }

    func test_stockCard_scrollToActiveOnOpen() {
        mocker
            .mock_searchHistory(geoRadius: 500, state: .new)
            .mock_searchCars(newCount: 10, usedCount: 10)
            .mock_searchOfferLocatorCounters(type: .all)

        launch()

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell.should(.radiusBubble(500), .exist)
            }
            .swipeUp()
            .swipeUp()
            .swipeDown()
            .swipeDown()
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell
                    .should(.radiusBubble(500), .exist)
                    .tap(.radiusBubble(1000))
                    .should(.radiusBubble(500), .exist)
            }
    }

    func test_stockCard_geoRadiusExists_changeGeoToRussia() {
        mocker
            .mock_searchHistory(geoRadius: 100, state: .new)
            .mock_searchCars(newCount: 2, usedCount: 0, distances: distances)
            .mock_searchOfferLocatorCounters(type: .all, distances: distances)

        launch()

        let requestRussia = expectationForRequest { request -> Bool in
            if request.method == "POST"
                && request.uri == "/search/cars?context=group_card&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                return req.geoRadius == 0 && req.rid == [225]
            }
            return false
        }

        openListingFromHistory(for: "LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .focus(on: .geoRadiusBubbles, ofType: .geoRadiusBubblesCell) { cell in
                cell.focus(on: .wholeRussiaBubble, ofType: .geoRadiusBubbleItem) { bubble in
                    bubble
                        .shouldBeActive(false)
                        .should(.title, .match("Россия"))
                        .tap()
                }
            }

        wait(for: [requestRussia], timeout: 5)
    }

    // MARK: - Private

    private func checkGeoRadiusBubblesForRadius100(cell: GeoRadiusBubblesCell) {
        cell.should(.currentRegionBubble, .exist)

        cell.focus(on: .radiusBubble(100), ofType: .geoRadiusBubbleItem) { bubble in
            bubble.shouldBeActive(true)
        }

        cell
            .should(.wholeRussiaBubble, .exist)
            .should(.radiusBubble(200), .be(.hidden))
    }

    private func openListingFromHistory(for title: String) -> SaleCardListSteps {
        Step("Открываем листинг из истории для '\(title)'") {
            mainSteps.openSearchHistory(title)
        }
        return mainSteps.as(SaleCardListSteps.self)
    }

    private static func checkSearchParamsForGeoRadius(_ data: Data, radius: Int = 0) -> Bool {
        let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: data)
        return req.geoRadius == radius
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
