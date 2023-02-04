//
//  SharkCreditTest_FilterTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 09.09.2021.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleCard AutoRuCredit AutoRuPreliminaryCreditClaim
class SharkCreditTest_FilterTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]
    lazy var sharkMocker: SharkMocker = SharkMocker(server: server)
    override var appSettings: [String: Any] {
        return settings
    }

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["reportUseJsTemplate"] = true
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
    }

    func test_creditPriceFilter() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        launch()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        let searchRequestHasCreditParam = expectationForRequest(requestChecker: ({
            if $0.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                if let req = try? Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: $0.messageBody!),
                   req.creditGroup.paymentTo == 41000,
                   req.creditGroup.loanTerm == 60,
                   req.creditGroup.initialFee == 0,
                   req.searchTag.contains("allowed_for_credit") {
                    return true
                }
            }
            return false
        }))

        mainSteps
            .wait(for: 1)
            .openFilters()
            .tapOnField(.creditPrice)
            .validateSnapshot(of: "SharkCreditPriceFilterVC", snapshotId: "SharkCreditPriceFilterVC-creditOff")
            .tap("В кредит")
            .wait(for: 1)
            .validateSnapshot(of: "SharkCreditPriceFilterVC", snapshotId: "SharkCreditPriceFilterVC-creditOn")
            .tap("Готово")
            .as(FiltersSteps.self)
            .showResultsTap()

        wait(for: [searchRequestHasCreditParam], timeout: 10)
    }

    func test_creditPriceFilter_changeOnNewAfterSetCreditParam() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        launch()

        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        let searchRequestHasNotCreditParam = expectationForRequest(requestChecker: ({
            if $0.uri == "/search/cars?context=listing&group_by=CONFIGURATION&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                if let req = try? Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: $0.messageBody!),
                   !req.hasCreditGroup,
                   !req.searchTag.contains("allowed_for_credit") {
                    return true
                }
            }
            return false
        }))

        mainSteps
            .wait(for: 1)
            .openFilters()
            .tapOnField(.creditPrice)
            .validateSnapshot(of: "SharkCreditPriceFilterVC", snapshotId: "SharkCreditPriceFilterVC-creditOff")
            .tap("В кредит")
            .wait(for: 1)
            .validateSnapshot(of: "SharkCreditPriceFilterVC", snapshotId: "SharkCreditPriceFilterVC-creditOn")
            .tap("Готово")
            .tap("Новые")
            .as(FiltersSteps.self)
            .showResultsTap()

        wait(for: [searchRequestHasNotCreditParam], timeout: 10)
    }

    func test_creditPriceFilter_openCredit_new() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        launch()

        mainSteps
            .wait(for: 1)
            .openFilters()
            .tapOnField(.creditPrice)
            .tap("В кредит")
            .tap("Узнайте ваш лимит по кредиту")
            .exist(selector: "field_name")
    }

    func test_creditPriceFilter_openCredit_draft() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        launch()

        mainSteps
            .wait(for: 1)
            .openFilters()
            .tapOnField(.creditPrice)
            .tap("В кредит")
            .tap("Узнайте ваш лимит по кредиту")
            .exist(selector: "CreditFormViewController")
    }

    func test_creditPriceFilter_openCredit_active() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        launch()

        mainSteps
            .wait(for: 1)
            .openFilters()
            .tapOnField(.creditPrice)
            .tap("В кредит")
            .tap("Узнайте ваш лимит по кредиту")
            .exist(selector: "CreditLKViewController")
    }

    func test_creditPriceFilter_allowedForCredit() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .active, claims: [.init(.draft, .tinkoff_1)], offerType: .none)
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true
        launch()

        let searchRequestHasAllowedForCrdit = expectationForRequest(requestChecker: ({
            if $0.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                if let req = try? Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: $0.messageBody!),
                   req.searchTag.contains("allowed_for_credit") {
                    return true
                }
            }
            return false
        }))

        mainSteps
            .wait(for: 1)
            .openFilters()
            .scrollToField(.allowedForCredit)
            .tapOnField(.allowedForCredit)
            .showResultsTap()
        wait(for: [searchRequestHasAllowedForCrdit], timeout: 10)
    }

    func test_creditPriceFilter_openFromMainPage() {
        sharkMocker
            .baseMock(offerId: "")
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockNoApplication()
            .start()
        settings["skipPreliminaryClaimDraft"] = true
        settings["skipCreditAlert"] = true

        launch(on: .transportScreen)
            .wait(for: 1)
            .focus(on: .creditPriceFilter) { cell in
                cell.tap()
            }
            .wait(for: 1)
            .validateSnapshot(of: "SharkCreditPriceFilterVC", snapshotId: "SharkCreditPriceFilterVC-creditOn")
    }
}
