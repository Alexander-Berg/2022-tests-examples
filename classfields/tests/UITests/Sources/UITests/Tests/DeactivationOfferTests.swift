//
//  DeactivationOfferTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 20.05.2020.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDeactivateSale
class DeactivationOfferTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

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
        launch()
    }

    // MARK: - Helpers

    private func setupServer() {

        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=false") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /story/search") { (_, _) -> Response? in
            return Response.okResponse(fileName: "story_search_ok", userAuthorized: false)
        }

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_all_ok", userAuthorized: true)
        }

        server.addHandler("GET /user/offers/CARS/1097256960-ad2747f9/predict-buyers") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_CARS_1097256960-ad2747f9_predict-buyers_ok", userAuthorized: false)
        }

        server.addHandler("POST /user/offers/CARS/1097256960-ad2747f9/hide") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_CARS_1097256960-ad2747f9_hide_ok", userAuthorized: false)
        }

        server.addHandler("GET /user/offers/CARS/1097256960-ad2747f9") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_CARS_1097256960-ad2747f9_ok", userAuthorized: false)
        }

        try! server.start()
    }

    func test_second_deactivateAndSelectBuyerPhone() {
        let requestExpectationHide: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/offers/CARS/1097256960-ad2747f9/hide" {
                if let data = request.messageBody, let messageBody = try? Auto_Api_OfferHideRequest(jsonUTF8Data: data) {
                    if messageBody.buyerPhone == "+79291125595", messageBody.addToWhiteList == false {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .exist("Снять с продажи")
            .tapDeactivate()
            .selectReason(.soldOnAutoru)
            .validateSnapShot(accessibilityId: "ModalViewControllerHost", snapshotId: "PhonePopUpSecond")
            .selectBuyerPhone()
            .notExist("Снять с продажи")

        self.wait(for: [requestExpectationHide], timeout: 10)
    }

    func test_second_deactivateAndWriteBuyerPhone() {
        let requestExpectationHide: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/offers/CARS/1097256960-ad2747f9/hide" {
                if let data = request.messageBody, let messageBody = try? Auto_Api_OfferHideRequest(jsonUTF8Data: data) {
                    if messageBody.buyerPhone == "+79291125591", messageBody.addToWhiteList == false {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .exist("Снять с продажи")
            .tapDeactivate()
            .selectReason(.soldOnAutoru)
            .tapAnother()
            .validateSnapShot(accessibilityId: "ModalViewControllerHost", snapshotId: "OnlyPhoneDeactivaeAfterAnotherUp")
            .typeText("+7 929 112-55-91")
            .tapDeactivate()
            .notExist("Снять с продажи")

        self.wait(for: [requestExpectationHide], timeout: 10)
    }

    func test_second_deactivateWithNoRemember() {
        let requestExpectationHide: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/offers/CARS/1097256960-ad2747f9/hide" {
                if let data = request.messageBody, let messageBody = try? Auto_Api_OfferHideRequest(jsonUTF8Data: data) {
                    if messageBody.buyerPhone.isEmpty {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .exist("Снять с продажи")
            .tapDeactivate()
            .selectReason(.soldOnAutoru)
            .tapNoRemember()
            .notExist("Снять с продажи")

        self.wait(for: [requestExpectationHide], timeout: 10)
    }

    func test_second_deactivateWithNoLoadedPhoneButWrite() {
        server.addHandler("GET /user/offers/CARS/1097256960-ad2747f9/predict-buyers") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_CARS_1097256960-ad2747f9_predict-buyers_empty", userAuthorized: false)
        }

        let requestExpectationHide: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/user/offers/CARS/1097256960-ad2747f9/hide" {
                if let data = request.messageBody, let messageBody = try? Auto_Api_OfferHideRequest(jsonUTF8Data: data) {
                    if messageBody.buyerPhone == "+79291125591", messageBody.addToWhiteList == false {
                        return true
                    }
                }
            }
            return false
        }

        mainSteps
            .openOffersTab()
            .exist("Снять с продажи")
            .tapDeactivate()
            .selectReason(.soldOnAutoru)
            .validateSnapShot(accessibilityId: "ModalViewControllerHost", snapshotId: "PhonePopUpWithNoPhoneToSelectSecond")
            .typeText("79291125591")
            .tapDeactivate()
            .notExist("Снять с продажи")
            .notExist("+7 929 112-55-91")

        self.wait(for: [requestExpectationHide], timeout: 10)
    }
}
