//
//  BestPriceRequestFromGalleryTests.swift
//  UITests
//
//  Created by Ibrakhim Nikishin on 11/17/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

class BestPriceRequestFromGalleryTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    private var offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_openBestPriceRequestForm() {
        mock()
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"),
                                                   overrideAppSettings: ["enableGalleryBestPriceRequest": true,
                                                                         "webHosts": "http://127.0.0.1:\(port)",
                                                                         "currentHosts": [
                                                                            "PublicAPI": "http://127.0.0.1:\(port)/"
                                                                        ]]))
            .openGallery()
            .should(provider: .galleryScreen, .exist)
            .focus { screen in
                screen
                    .swipeToPreviousPhoto()
                    .focus(on: .bestPriceSlide, ofType: .bestPriceGallerySlide) { slide in
                        slide
                            .tap(.bestPriceRequestButton)
                    }
            }
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
        server.addHandler("GET /carfax/report/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_report_raw_GET_ok", userAuthorized: forceLogin)
        }
        server.addHandler("GET /carfax/bought-reports/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_bought-reports_GET_ok", userAuthorized: forceLogin)
        }
        server.addHandler("GET /carfax/offer/cars/\(offerId)/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_offer_cars_1090794514-915f196d_raw_GET_ok", userAuthorized: forceLogin)
        }
    }
}
