import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuViews AutoRuSaleCard AutoRuSaleList AutoRuFilters AutoRuUIComponents
final class AddToSavedSearchTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func test_addToSavedSearchFromAutoSaleList() {
        let addToSavedSearchRequestExpectation = expectationForRequest(
            method: "POST",
            uri: "/user/favorites/cars/subscriptions",
            requestChecker: { (req: Auto_Api_SavedSearchCreateParams) -> Bool in
                XCTAssertEqual(req.params.catalogFilter.first?.mark, "VAZ")
                XCTAssertEqual(req.params.catalogFilter.first?.model, "1111")
                XCTAssertEqual(req.params.carsParams.bodyTypeGroup.first, .anyBody)
                XCTAssertEqual(req.params.carsParams.seatsGroup, .anySeats)
                XCTAssertEqual(req.params.carsParams.engineGroup.first, .anyEngine)
                XCTAssertEqual(req.params.carsParams.engineGroup.first, .anyEngine)
                XCTAssertEqual(req.params.currency, .rur)
                XCTAssertEqual(req.params.hasImage_p, true)
                XCTAssertEqual(req.params.inStock, .anyStock)
                XCTAssertEqual(req.params.rid.first, 10702)
                XCTAssertEqual(req.params.geoRadius, 300)
                XCTAssertEqual(req.params.withDiscount, true)
                XCTAssertEqual(req.params.withDelivery, .both)
                XCTAssertEqual(req.params.onlyNds, false)
                XCTAssertEqual(req.params.excludeGeoRadius, 200)
                XCTAssertEqual(req.params.stateGroup, .all)
                XCTAssertEqual(req.params.exchangeGroup, .noExchange)
                XCTAssertEqual(req.params.sellerGroup.first, .anySeller)
                XCTAssertEqual(req.params.damageGroup, .notBeaten)
                XCTAssertEqual(req.params.ownersCountGroup, .anyCount)
                XCTAssertEqual(req.params.owningTimeGroup, .anyTime)
                XCTAssertEqual(req.params.customsStateGroup, .cleared)
                XCTAssertEqual(req.title, "")
                return true
            }
        )

        mocker
            .mock_searchHistory(state: .used)
            .mock_searchCars(newCount: 5, usedCount: 5)
            .mock_addToFavouriteSubscriptions()

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .tap(.addToSavedSearchIcon)
            .tap(.okButton)
            .wait(for: 1)
            .should(.savedSearchIcon, .exist)

        wait(for: [addToSavedSearchRequestExpectation], timeout: 1)
    }

    func test_savedSearchInFavourite() {
        let title = "BMW 5 серия IV (E39) Рестайлинг"
        let searchId = "71c47b723742bcb0b79056767fdf32f3bc39c068"

        let requestFavouriteSubscription = expectationForRequest(
            method: "GET",
            uri: "/user/favorites/all/subscriptions/\(searchId)"
        )

        let requestSavedSearchExpectation = expectationForRequest(
            method: "POST",
            uri: "/search/saved/\(searchId)?context=subscription&page=1&page_size=20&sort=cr_date-desc"
        )

        mocker
            .mock_favouriteSubscriptions()
            .mock_favouriteSubscriptionById(searchId)
            .mock_savedSearch(searchId)
            .mock_user(userEmailConfirmed: true)

        launch()

        let favouriteSteps = mainSteps
            .openFavoritesTab()
            .waitForLoading()
            .tapSegment(at: .searches)
            .should(.label(title), .exist)
            .should(.label("Новые и с пробегом, Москва"), .exist)
            .should(.subscribeSwitch(id: searchId), .be(.on))
            .tap(.label(title))

        wait(for: [requestFavouriteSubscription, requestSavedSearchExpectation], timeout: 1)

        favouriteSteps
            .should(provider: .saleListScreen, .exist)
            .focus { saleListScreen in
                saleListScreen.tap(.searchParams)
            }
            .should(provider: .filtersScreen, .exist)
            .focus { filtersSteps in
                checkSavedSearchParams(filtersSteps)
            }
    }

    private func checkSavedSearchParams(_ filtersSteps: FiltersSteps) {
        filtersSteps.should(.mark, .contain("BMW"))
        filtersSteps.should(.model, .contain("5 серии"))
        filtersSteps.should(.generation, .contain("c 2000 по 2004, IV (E39) Рестайлинг"))
        filtersSteps.should(.geo, .contain("Москва и Московская область, Санкт-Петербург"))
        filtersSteps.should(.year, .contain("от 2001 до 2003 г."))
        filtersSteps.should(.creditPrice, .contain("от 100,000 до 1,000,000 ₽"))
        filtersSteps.should(.transmission, .contain("Автоматическая"))
        filtersSteps.should(.bodytype, .contain("Седан"))
        filtersSteps.should(.engine, .contain("Дизель"))
        filtersSteps.should(.engineVolume, .contain("от 1.5 до 2.5 л"))
        filtersSteps.should(.gear, .contain("Передний"))
        filtersSteps.scroll(to: .power)
        filtersSteps.should(.power, .contain("от 100 до 250 л.с."))
        filtersSteps.should(.run, .contain("от 40,000 до 380,000 км"))
        filtersSteps.should(.acceleration, .contain("от 5 с"))
        filtersSteps.should(.fuelRate, .contain("до 30 л"))
        filtersSteps.should(.clearance, .contain("от 1 мм"))
        filtersSteps.should(.trunkVolume, .contain("от 1 л"))
        filtersSteps.should(.color(0), .contain("Черный"))
        filtersSteps.should(.wheel, .contain("Левый"))
        filtersSteps.scroll(to: .ownersCount)
        filtersSteps.should(.ownersCount, .contain("Не более двух"))
        filtersSteps.should(.owningTime, .contain("От 3 лет и более"))
        filtersSteps.should(.state, .contain("Кроме битых"))
        filtersSteps.should(.customs, .contain("Растаможен"))
        filtersSteps.should(.original_pts, .be(.on))
        filtersSteps.should(.manufacturerCheck, .be(.off))
        filtersSteps.should(.onlineView, .be(.off))
        filtersSteps.scroll(to: .exchange)
        filtersSteps.should(.warranty, .be(.off))
        filtersSteps.should(.exchange, .be(.off))
        filtersSteps.scroll(to: .photo)
        filtersSteps.should(.photo, .be(.off))
        filtersSteps.should(.video, .be(.off))
        filtersSteps.should(.panorama, .be(.off))
        filtersSteps.should(.delivery, .be(.off))
    }
}
