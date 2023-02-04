//
//  SaleCardReportPurchaseTests.swift
//  UITests
//
//  Created by Pavel Savchenkov on 05.08.2021.
//

import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuSaleCard AutoRuPayments AutoRuStandaloneCarHistory
final class SaleCardReportPurchaseTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)
    private lazy var carfaxReportSteps = CarfaxStandaloneCardBasicSteps(context: self)
    private lazy var saleCardSteps = SaleCardSteps(context: self)
    private lazy var paymentOptionsSteps = PaymentOptionsSteps(context: self, source: self)
    private let requestTimeout: TimeInterval = 2
    private let boughtReportTitle = "Volkswagen Golf Plus, 2010"
    private let freeReportTitle = "BMW X5, 2009"

    override func setUp() {
        super.setUp()
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_reportLayoutForReport(bought: false)
        mocker.mock_reportLayoutForOffer(bought: false)
            .mock_paymentInitWithAttachedCard()
            .mock_paymentProcess()
            .mock_paymentClosed()
            .startMock()
        server.api.offer.category(.cars).offerID("1101613244-b69e1290")
            .get
            .ok(mock: .file("PaymentHistoryAll_discount-precedence_offer"))
    }

    func test_shouldBuySingleReportFromCard() {
        let freeReportExpectation = expectationForRequest(
            method: "GET",
            uri: "/ios/makeXmlForOffer?offer_id=1101613244-b69e1290"
        )

        let boughtReportExpectation = expectationForRequest(
            method: "GET",
            uri: "/ios/makeXmlForOffer?decrement_quota=true&offer_id=1101613244-b69e1290"
        )

        launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))
        mocker.mock_reportLayoutForReport(bought: true)
        mocker.mock_reportLayoutForOffer(bought: true)


        wait(for: [freeReportExpectation], timeout: requestTimeout)

        saleCardSteps
            .scrollToReportBuySingleButton()
            .tapReportBuySingleButton()
            .tapOnPurchaseButton()

        carfaxReportSteps
            .checkReportTitle(withText: boughtReportTitle)
            .chechBuyFullReportButtonNotExist()
            .tapBack()

        wait(for: [boughtReportExpectation], timeout: requestTimeout)

        saleCardSteps
            .scroll(to: .carReportPreview, direction: .down)
            .should(provider: .saleCardScreen, .exist)
            .focus({ screen in
                screen
                    .checkShowFullReportbuttonIsDisplayed()
                    .checkReportBuySingleButtonNotExist()
            })
    }

    func test_shouldBuySingleReportFromFreeReport() {
        let boughtReportExpectation = expectationForRequest(
            method: "GET",
            uri: "/ios/makeXmlForReport?decrement_quota=true&offer_id=1101613244-b69e1290"
        )

        launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101613244-b69e1290")))

        saleCardSteps
            .scrollToReportPreview()
            .scrollToFreeReport()
            .openFreeReport()

        mocker.mock_reportLayoutForReport(bought: true)
        mocker.mock_reportLayoutForOffer(bought: true)
        carfaxReportSteps
            .wait(for: 1)
            .buyFullReport()

        paymentOptionsSteps
            .tapOnPurchaseButton()

        wait(for: [boughtReportExpectation], timeout: requestTimeout)

        carfaxReportSteps
            .checkReportTitle(withText: boughtReportTitle)
            .chechBuyFullReportButtonNotExist()
            .tapBack()
        saleCardSteps
            .scroll(to: .carReportPreview, direction: .down)
            .should(provider: .saleCardScreen, .exist)
            .focus({ screen in
                screen
                    .checkShowFullReportbuttonIsDisplayed()
                    .checkReportBuySingleButtonNotExist()
            })
    }

    func test_shouldBuySingleReportFromFreeReportAndAddFavorite() {
        server.addHandler("GET /offer/cars/1098230510-dd311329") { (_, _) -> Response? in
            var resp: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098230510-dd311329_ok")
            resp.offer.id = "1098230510-dd311329"
            resp.offer.status = .active
            return Response.okResponse(message: resp)
        }

        launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))

        SaleCardSteps(context: self)
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .freeReportButton)
                    .tap(.freeReportButton)
            }
            .should(provider: .carReportScreen, .exist)
            .focus { $0.tap(.purchaseReportButton) }
            .do {
                mocker.mock_reportLayoutForReport(bought: true)
                mocker.mock_reportLayoutForOffer(bought: true)
                server.addHandler("GET /carfax/offer/cars/1098230510-dd311329/raw") { (_, _) -> Response? in
                    var lastOffer = Auto_Api_Vin_OfferRecord()
                    lastOffer.offerID = "1098230510-dd311329"
                    lastOffer.offerStatus = .active

                    var resp: Auto_Api_RawVinReportResponse = .init(mockFile: "carfax_offer_cars_1090794514-915f196d_raw_GET_ok")
                    resp.report.autoruOffers.offers.append(lastOffer)

                    return Response.okResponse(message: resp)
                }
            }
            .should(provider: .paymentOptionsScreen, .exist)
            .focus { $0.tap(.purchaseButton) }
            .should(provider: .carReportScreen, .exist)
            .focus { screen in
                screen
                    .tap(.favoriteButton)
                    .wait(for: 1)
                    .focus(on: .favoriteButton) { $0.validateSnapshot() }
            }
    }
}
