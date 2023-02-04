//
//  BestOffersTests.swift
//  UITests
//
//  Created by Alexander Malnev on 7/17/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuBestOffers
class BestOffersTests: BaseTest {
    private static let requestTimeout = 10.0
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    lazy var mainSteps = MainSteps(context: self)
    override var appSettings: [String: Any] {
        var value = super.appSettings
        value["otherDealerOffersCount"] = 1
        return value
    }

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: - Helpers

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (request, _) -> Response? in
            return Response.okResponse(fileName: "best_offers_search_cars")
        }

        server.addHandler("POST /search/cars/mark-model-filters?geo_radius=0&&search_tag=match_applications", { _, _ in
            return Response.okResponse(fileName: "best_offers_mark_model_filters")
        })

        server.addHandler("POST /search/cars?context=default&group_by=CONFIGURATION&page=1&page_size=1&sort=fresh_relevance_1-desc", { _, _  in
            return Response.okResponse(fileName: "best_offers_search_cars_configuration")
        })

        server.addHandler("POST /search/cars?context=default&group_by=COMPLECTATION_NAME&page=1&page_size=100&sort=fresh_relevance_1-desc", { _, _ in
            return Response.okResponse(fileName: "best_offers_search_complectations")
        })

        server.addHandler("POST /search/cars?context=group_card&page=1&page_size=20&sort=fresh_relevance_1-desc", { _, _  in
            return Response.okResponse(fileName: "best_offers_search_cars_listing")
        })

        server.addHandler("GET /reference/catalog/cars/configurations/subtree?configuration_id=21574746", { _, _ in
            return Response.okResponse(fileName: "best_offers_reference_catalog_cars_configurations_subtree")
        })

        server.addHandler("GET /reference/catalog/cars/all-options", { _, _ in
            return Response.okResponse(fileName: "best_offers_reference_catalog_cars_all-options")
        })

        server.addHandler("POST /search/CARS/equipment-filters", { _, _ in
            return Response.okResponse(fileName: "best_offers_search_cars_equipment-filters")
        })

        server.addHandler("GET /offer/cars/1/phones", { _, _ in
            return Response.okResponse(fileName: "best_offers_phones")
        })

        try! server.start()
    }

    private func setupMatchApplicationSuccess() {
        server.addHandler("POST /match-applications", { _, _ in
            return Response.okResponse(fileName: "best_offers_match")
        })
    }

    private func setupMatchApplicationFailure() {
        server.addHandler("POST /match-applications", { _, _ in
            return Response.badResponse(fileName: "best_offers_match_error")
        })
    }

    private func setupUserProfileStub() {
        server.addHandler("GET /user?with_auth_types=true", { _, _ in
            return Response.okResponse(fileName: "best_offers_user_with_phone")
        })
    }

    private func setupMatchingStub(_ flag: Bool) {
        server.addHandler("POST /search/cars?context=bottom_sheet&group_by=DEALER_ID&page=1&page_size=4&sort=price-asc", { _, _ in
            return Response.okResponse(fileName: flag ? "best_offers_search_cars_matching" : "best_offers_search_cars_matching_false")
        })
    }

    @discardableResult
    private func routeToStockListing() -> StockCardSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToStockOffer(with: "1", position: .body)
            .openStockCardOffer(offersTitle: "88 предложений")
    }

    func expectationForRequest(phone: String, creditFlag: Bool, tradeInFlag: Bool) -> XCTestExpectation {
        return expectationForRequest { req -> Bool in
            let requestMatch = req.method == "POST" && req.uri.lowercased() == "/match-applications".lowercased()
            guard requestMatch,
                  let json = req.messageBodyString(),
                  let body = try? Ru_Auto_MatchMaker_MatchApplication(jsonString: json),
                  let catalogFilter = body.userProposal.searchParams.catalogFilter.first else {
                return false
            }

            return catalogFilter.mark == "BMW" &&
                catalogFilter.model == "X1" &&
                catalogFilter.generation == 21574693 &&
                catalogFilter.configuration == 21574746 &&
                body.userInfo.creditInfo.isPossible == creditFlag &&
                body.hasUserOffer == tradeInFlag &&
                body.userInfo.phone == phone &&
                body.userProposal.searchParams.rid == [10716] &&
                body.userInfo.userID == 1
        }
    }

    // MARK: - Tests

    func test_bestOfferModalRequest() {
        setupMatchingStub(true)
        setupMatchApplicationSuccess()

        server.addHandler("POST /user/phones", { _, _ in
            return Response.okResponse(fileName: "best_offers_user_phones")
        })

        server.addHandler("POST /user/phones/confirm", { _, _ in
            return Response.okResponse(fileName: "best_offers_user_phones_confirm")
        })

        server.addHandler("GET /user?with_auth_types=true", { _, _ in
            return Response.okResponse(fileName: "best_offers_user")
        })
        launch()

        var steps = routeToStockListing()
            .scrollToOffer(with: "1")
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)
            .as(BestOffersSteps.self)

        let titleText = app.staticTexts["Лучшие предложения BMW X1"].firstMatch
        Step("Шторка показалась") {
            titleText.shouldExist()
        }

        let descriptionText = app.staticTexts["Оставьте заявку, чтобы дилеры нашли для вас самые выгодные предложения, которых нет в общем доступе."].firstMatch
        descriptionText.shouldExist()

        let screenshot = app.descendants(matching: .any).matching(identifier: "best_offers_modal").firstMatch.waitAndScreenshot().image
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: #function)
        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, overallTolerance: 0.01)

        steps = steps
            .tapCredit()
            .tapTradeIn()
            .tapPhone()
            .wait(for: 4)

        Step("Добавляем телефон") {
            let key = app.keys["6"]
            for _ in 0 ..< 10 {
                key.tap()
            }
            steps.wait(for: 1)
        }
        Step("Подтверждаем добавление телефона по коду") {
            let key = app.keys["6"]
            for _ in 0 ..< 4 {
                key.tap()
            }
            steps.wait(for: 1)
        }
        let expectation = self.expectationForRequest(phone: "+76666666666", creditFlag: true, tradeInFlag: true)
        steps.tapSendRequest()

        self.wait(for: [expectation], timeout: Self.requestTimeout)

        steps.wait(for: 1)

        Step("Шторка должна закрыться") {
            titleText.shouldNotExist()
        }
    }

    func test_bestOfferModalRequestWithPresetPhone() {
        setupMatchingStub(true)
        setupUserProfileStub()
        setupMatchApplicationSuccess()
        launch()

        let bestOfferSteps = routeToStockListing()
            .scrollToOffer(with: "1")
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)
            .as(BestOffersSteps.self)

        let titleText = app.staticTexts["Лучшие предложения BMW X1"].firstMatch
        Step("Шторка показалась") {
            titleText.shouldExist()
        }

        let phoneText = app.staticTexts["+7 666 666-66-66"].firstMatch
        phoneText.shouldExist()

        let phonePickerSteps = bestOfferSteps.tapPhone()
            .wait(for: 1)
            .as(PhonePickerSteps.self)

        phonePickerSteps
            .tapPhone("+7 888 888-88-88")

        let expectation = self.expectationForRequest(phone: "+78888888888", creditFlag: false, tradeInFlag: false)

        bestOfferSteps.tapSendRequest()

        self.wait(for: [expectation], timeout: Self.requestTimeout)

        bestOfferSteps.wait(for: 1)

        Step("Шторка должна закрыться") {
            titleText.shouldNotExist()
        }
    }

    func test_noModalForNegativeFlag() {
        setupMatchingStub(false)
        setupMatchApplicationSuccess()
        launch()

        routeToStockListing()
            .scrollToOffer(with: "1")
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)

        let titleText = app.staticTexts["Лучшие предложения BMW X1"].firstMatch
        Step("Шторка не показывается") {
            titleText.shouldNotExist()
        }
    }

    func test_modalNotClosingOnError() {
        setupMatchingStub(true)
        setupUserProfileStub()
        setupMatchApplicationFailure()
        launch()

        let bestOfferSteps = routeToStockListing()
            .scrollToOffer(with: "1")
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)
            .as(BestOffersSteps.self)

        let titleText = app.staticTexts["Лучшие предложения BMW X1"].firstMatch
        Step("Шторка показалась") {
            titleText.shouldExist()
        }

        bestOfferSteps.tapSendRequest().wait(for: 3)

        Step("Шторка не должна скрываться") {
            titleText.shouldExist()
        }
    }

    func test_noModalForCountExceeded() {
        setupMatchingStub(true)
        setupMatchApplicationSuccess()
        launch()

        let steps = routeToStockListing()
            .scrollToOffer(with: "1")

        steps
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)

        let titleText = app.staticTexts["Лучшие предложения BMW X1"].firstMatch
        Step("Шторка показалась") {
            titleText.shouldExist()
        }

        steps
            .as(BestOffersSteps.self)
            .tapDismiss()

        steps
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)

        Step("Шторка должна закрыться") {
            titleText.shouldNotExist()
        }

        let rateTitle = app.staticTexts["Оцените объявление"].firstMatch
        rateTitle.shouldExist()
    }

    func test_bestOffersModalRequestWithCredit() {
        setupMatchingStub(true)
        setupUserProfileStub()
        setupMatchApplicationSuccess()
        launch()

        let bestOfferSteps = routeToStockListing()
            .scrollToOffer(with: "1")
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)
            .as(BestOffersSteps.self)
            .tapCredit()

        let expectation = self.expectationForRequest(phone: "+76666666666", creditFlag: true, tradeInFlag: false)

        bestOfferSteps.tapSendRequest()

        self.wait(for: [expectation], timeout: Self.requestTimeout)
    }

    func test_bestOffersModalRequestWithTradeIn() {
        setupMatchingStub(true)
        setupUserProfileStub()
        setupMatchApplicationSuccess()
        launch()

        let bestOfferSteps = routeToStockListing()
            .scrollToOffer(with: "1")
            .tap(.callButton)
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker.tap(.phoneOption)
            }
            .base
            .wait(for: 1)
            .as(BestOffersSteps.self)
            .tapTradeIn()

        let expectation = self.expectationForRequest(phone: "+76666666666", creditFlag: false, tradeInFlag: true)

        bestOfferSteps.tapSendRequest()

        self.wait(for: [expectation], timeout: Self.requestTimeout)
    }
}
