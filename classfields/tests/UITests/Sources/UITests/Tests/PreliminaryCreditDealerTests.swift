//
//  PreliminaryCreditTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 29.05.2020.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleCard AutoRuPreliminaryCreditClaim
class PreliminaryDealerCreditTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]

    private var offerId = "1098252972-99d8c274"
    private var dealerId = "35426"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    struct Draft: Codable {
        let name: String
        let email: String
        let phone: String
    }

    private var emptyDraft: Bool = false
    override var launchEnvironment: [String: String] {
        let data: Data
        if !emptyDraft {
            let draft = Draft(name: "Петрулио Буэнди Сальвадор", email: "pet@sa.ru", phone: "9875643212")
            let encoder = JSONEncoder()
            encoder.dataEncodingStrategy = .base64
            data = (try? JSONEncoder().encode(draft)) ?? Data()
        } else {
            data = Data()
        }
        var value = super.launchEnvironment
        let userDefaults: [String: Any] = [
            "preliminaryClaimDraft": String(data: data, encoding: .utf8) ?? ""
        ]
        let userDefaultsJsonData = try! JSONSerialization.data(withJSONObject: userDefaults, options: [])
        value["STANDARD_USER_DEFAULTS"] = userDefaultsJsonData.base64EncodedString()
        return value
    }

    // MARK: - Helpers

    private func setupServer() {
        try! server.start()
    }

    private func mock(isEmpty: Bool, forceLogin: Bool = true) {
        server.forceLoginMode = forceLogin ? .forceLoggedIn : .preservingResponseState

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: forceLogin)
        }

        server.addHandler("GET /offer/CARS/1098252972-99d8c274 *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1098252972-99d8c274_dealer_ok", userAuthorized: forceLogin)
        }

        let userProfile: Auto_Api_UserResponse = {
            var profile = Auto_Api_UserResponse()
            profile.user.id = "1"
            profile.user.profile.autoru.about = ""
            return profile
        }()

        server.addHandler("GET /user *") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! userProfile.jsonUTF8Data(), userAuthorized: forceLogin)
        }
    }

    private func launchAndGoToCredits(provider: String, offerId: String) -> PreliminaryCreditSteps {
        launch(on: .saleCardScreen,
               options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"),
                              overrideAppSettings: [
                                "currentBank": "tinkoff",
                                "fallbackBank": "alphabank",
                                "webHosts": "http://127.0.0.1:\(port)"]))
        .scrollTo("credit_calculator_header", windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0))
        .validateSnapShot(accessibilityId: "credit_calculator_header", snapshotId: "\(provider)Header", message: "Snapshot Заголовка калькулятора с \(provider)") {  step in
            step.swipeUp()
        }
        .scrollTo("credit_calculator", windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0))
        .validateSnapShot(accessibilityId: "credit_calculator", snapshotId: "\(provider)calculator", message: "Snapshot Кредитного калькулятора") {  step in
            step.swipeUp()
        }
        .openPreliminarySteps()
    }

    private func validateInlinePreliminaryScreen(step: PreliminaryCreditSteps, bank: String, isEmpty: Bool, amount: String, term: String, fee: String, monthPay: String = "", dealerId: String, offerId: String, login: Bool = false) {
        let expectation = [
            expectationForRequest { request -> Bool in
                if request.method == "POST", request.uri == "/products" {
                    if let data = request.messageBody, let messageBody = try? Auto_Application_Application(jsonUTF8Data: data) {
                        if messageBody.credit.amount == Int32(amount)!,
                           messageBody.credit.period == Int(term)!,
                           messageBody.credit.monthPayment == Int32(monthPay)!,
                           messageBody.credit.initPayment == Int32(fee)!,
                           messageBody.dealerID == Int32(dealerId)!,
                           messageBody.offerID == offerId,
                           messageBody.offerLink == "https://auto.ru/cars/used/sale/\(offerId)" {
                            return true
                        }
                    }
                }
                return false
            }
        ]

        if isEmpty {
            step
                .enterFio("Петрулио Буэнди Сальвадор")
                .enterEmail("pet@sa.ru")
                .enterPhone("9875643212")
                .tapDone()
                .as(SaleCardSteps.self)
                .scrollTo("preliminary_credit_claim_agreeement", windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0))
                .as(PreliminaryCreditSteps.self)
                .validateSnapShot(from: "preliminary_credit_claim_fio", to: "preliminary_credit_claim_agreeement", snapshotId: "FilledInlineCreditScreenSelectable\(amount)\(fee)\(term)\(monthPay)\(bank)", message: "Snapshot Заполненной анкеты")

                .tapAgreement()
                .validateSnapShot(accessibilityId: "TextAnnotationViewController", snapshotId: "Agreement\(amount)\(fee)\(term)\(bank)", message: "Snapshot соглашения")
                .close()
                .tapSubmit()
            if login {
                step.enterLoginCode("1234")
            }
        } else {
            step
                .as(SaleCardSteps.self)
                .scrollTo("preliminary_credit_claim_agreeement", windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0))
                .as(PreliminaryCreditSteps.self)
                .validateSnapShot(from: "preliminary_credit_claim_fio", to: "preliminary_credit_claim_agreeement", snapshotId: "FilledInlineCreditScreenSelectable\(amount)\(fee)\(term)\(monthPay)\(bank)", message: "Snapshot Заполненной анкеты")
                .tapSubmit()
            if login {
                step.enterLoginCode("1234")
            }
        }
        wait(for: expectation, timeout: 10)
    }

    func test_dealer_empty() {
        emptyDraft = true
        mock(isEmpty: true)

        let step = launchAndGoToCredits(provider: "dealer", offerId: offerId)

        validateInlinePreliminaryScreen(step: step, bank: "dealer", isEmpty: true, amount: "715500", term: "60", fee: "79500", monthPay: "14650", dealerId: dealerId, offerId: offerId)
    }

    func test_dealer_filled() {
        emptyDraft = false
        mock(isEmpty: false)

        let step = launchAndGoToCredits(provider: "dealer", offerId: offerId)

        validateInlinePreliminaryScreen(step: step, bank: "dealer", isEmpty: false, amount: "715500", term: "60", fee: "79500", monthPay: "14650", dealerId: dealerId, offerId: offerId)
    }

    func test_dealer_unauthorized() {
        emptyDraft = false
        mock(isEmpty: false, forceLogin: false)

        server.addHandler("POST /auth/login-or-register") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_auth_login-or-register", userAuthorized: false)
        }
        server.addHandler("POST /user/confirm") { (_, _) -> Response? in
            return Response.okResponse(fileName: "credits_user_confirm", userAuthorized: false)
        }

        let step = launchAndGoToCredits(provider: "dealer", offerId: offerId)

        validateInlinePreliminaryScreen(step: step, bank: "dealer", isEmpty: false, amount: "715500", term: "60", fee: "79500", monthPay: "14650", dealerId: dealerId, offerId: offerId, login: true)
    }
}
