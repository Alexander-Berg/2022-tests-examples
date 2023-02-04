//
//  PromocodeProfileTests.swift
//  UITests
//
//  Created by Alexander Malnev on 3/22/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuUserProfile AutoRuPayments
final class PromocodeProfileTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {

        server.addHandler("POST /device/hello") { _, _ in
            Response.okResponse(fileName: "hello_ok", userAuthorized: false)
        }

        try! server.start()
    }

    func test_promocodeEntryButtonNotShownForUnauthorized() {
        server.forceLoginMode = .forceLoggedOut

        launch()

        mainSteps
            .openOffersTab()
            .as(LoginSteps.self)
            .dismissLoginIfNeeded()
            .as(OffersSteps.self)
            .validateNoEnterPromocodeButton()
    }

    func test_promocodeEntryButtonInEmptyListShowsBottomsheetToSendPromocode() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("GET /user *") { [weak self] _, _ in
            guard let strongSelf = self else {
                return Response.badResponse(code: Auto_Api_ErrorCode.badRequest)
            }
            return Response.responseWithStatus(body: try! strongSelf.userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        let expectation = expectationForRequest { req -> Bool in
            req.method == "POST" && req.uri.lowercased() == "/promocode/promocode"
        }

        launch()

        mainSteps
            .openOffersTab()
            .tapEnterPromocode()
            .as(PromocodeBottomsheetSteps.self)
            .wait(for: 1)
            .enterPromocodeText("promocode")
            .clearPromocodeText()
            .validatePromocodeInputIs("")
            .enterPromocodeText("promocode")
            .activatePromocode()

        self.wait(for: [expectation], timeout: 3)
    }

    func test_noPromocodeEntryButtonInEmptyListForDealer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("GET /user *") { _, _ in
            Response.okResponse(fileName: "dealer_profile_all_grants", userAuthorized: true)
        }

        launch()

        mainSteps
            .openOffersTab()
            .validateNoEnterPromocodeButton()
    }

    func test_promocodeEntryButtonBelowListShowsBottomsheetToSendPromocode() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("GET /user *") { [weak self] (_, _) -> Response? in
            guard let strongSelf = self else {
                return Response.badResponse(code: Auto_Api_ErrorCode.badRequest)
            }
            return Response.responseWithStatus(body: try! strongSelf.userProfile.jsonUTF8Data(), userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all *") { _, _ in
            Response.okResponse(fileName: "enable_push_user_sales", userAuthorized: true)
        }

        let expectation = expectationForRequest { req -> Bool in
            if(req.method == "POST" && req.uri.lowercased() == "/promocode/promocode") {
                if let data = req.messageBody,
                   let request = try? Auto_Api_PromocodeActivationRequest(jsonUTF8Data: data) {
                    return request.validation.products == []
                }
            }
            return false
        }

        launch()

        mainSteps
            .openOffersTab()
            .scrollToEnterPromocode()
            .validateSnapShot(accessibilityId: "enter_promocode")
            .tapEnterPromocode()
            .as(PromocodeBottomsheetSteps.self)
            .wait(for: 1)
            .enterPromocodeText("promocode")
            .clearPromocodeText()
            .validatePromocodeInputIs("")
            .enterPromocodeText("promocode")
            .activatePromocode()

        self.wait(for: [expectation], timeout: 3)
    }

    func test_noPromocodeEntryButtonBelowListForDealer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("GET /user *") { _, _ in
            Response.okResponse(fileName: "dealer_profile_all_grants", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/all?page=1&page_size=20&sort=cr_date-desc&status=ACTIVE&with_daily_counters=true") { _, _ in
            Response.okResponse(fileName: "dealer_offers_page_1", userAuthorized: true)
        }

        launch()

        mainSteps
            .openOffersTab()
            .validateNoEnterPromocodeButton()
    }
}
