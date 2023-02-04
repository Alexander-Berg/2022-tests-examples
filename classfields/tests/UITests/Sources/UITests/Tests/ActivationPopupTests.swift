//
//  ActivationPopupTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/6/21.
//

import XCTest
import Snapshots

final class ActivationPopupTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()

        advancedMockReproducer.setup(server: self.server, mockFolderName: "ActivateOldSalePopupTests")
        server.forceLoginMode = .forceLoggedIn
        try! server.start()

        launch()
    }

    func test_activateOldSimilarOffer() {
        let steps = mainSteps
            .openOffersTab()
            .tapActivate("Активировать за 4 699 ₽ / 7 дней")
            .as(DuplicatingSaleVASDescriptionSteps.self)

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            return request.method == "POST" && request.uri == "/user/offers/CARS/1103072901-1f42f374/activate"
        }

        steps.tapRecover()

        self.wait(for: [requestExpectation], timeout: 5)
    }

    func test_tryActivateNewOffer() {
        let paymentRequestExpectation: XCTestExpectation = expectationForRequest(method: "POST", uri: "/billing/autoru/payment/init")
        let activationRequestExpectation: XCTestExpectation = expectationForRequest(method: "POST", uri: "/user/offers/CARS/1103072901-1f42f374/activate")
        activationRequestExpectation.isInverted = true

        mainSteps
            .openOffersTab()
            .tapActivate("Активировать за 4 699 ₽ / 7 дней")
            .wait(for: 1)
            .as(DuplicatingSaleVASDescriptionSteps.self)
            .tapActivate("Подключить за 4 699 ₽")

        self.wait(for: [paymentRequestExpectation, activationRequestExpectation], timeout: 5)
    }
}
