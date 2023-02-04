//
//  BestPriceRequestFromSaleCardTests.swift
//  UITests
//
//  Created by Ibrakhim Nikishin on 12/2/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuSaleCard
final class BestPriceRequestFromSaleCardTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    private var offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_openBestPriceRequestForm() {
        mock()
        launch(on: .saleCardScreen,
               options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"),
                              overrideAppSettings: ["enableFooterBestPriceRequest" : true]))
        .openBestPriceRequestForm()
    }

    // MARK: - Helpers

    private func setupServer() {
        try! server.start()
    }

    private func mock() {
        let forceLogin = true
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: forceLogin)
        }

        server.addHandler("GET /history/last/all?page=1&page_size=20") { (_, _) -> Response? in
            return Response.okResponse(fileName: "history_last_all_credit_ok", userAuthorized: forceLogin)
        }

        server.addHandler("GET /offer/CARS/1098252972-99d8c274 *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "offer_CARS_1098252972-99d8c274_ok", userAuthorized: forceLogin)
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
}
