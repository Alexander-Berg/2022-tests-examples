//
//  SaleCardForcedLoginTests.swift
//  UITests
//
//  Created by Roman Bevza on 12/28/21.
//

import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels

final class SaleCardForcedLoginTests: BaseTest {
    func test_forcedLoginOnPhoneTap() {
        let offerId = "1098230510-dd311329"
        api.offer.category(.cars).offerID(offerId)
            .get
            .ok(mock: .file("offer_CARS_1098230510-dd311329_ok"))
        api.offer.category(.cars).offerID(offerId).phones
            .get
            .error(
                status: ._403,
                mock: .model(Auto_Api_ErrorResponse.init()) { model in
                    model.actionKind = .needAuth
                }
            )

        mocker
            .mock_base()
            .startMock()

        launch(on: .saleCardScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.callButton)
            }
            .should(provider: .loginScreen, .exist)
    }

    func test_forcedWebViewOnPhoneTap() {
        let offerId = "1098230510-dd311329"
        api.offer.category(.cars).offerID(offerId)
            .get
            .ok(mock: .file("offer_CARS_1098230510-dd311329_ok"))
        api.offer.category(.cars).offerID(offerId).phones
            .get
            .error(
                status: ._403,
                mock: .model(Auto_Api_ErrorResponse.init()) { model in
                    model.actionKind = .showURL
                    model.showURL = "http://example.com"
                }
            )

        mocker
            .mock_base()
            .startMock()

        launch(on: .saleCardScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                $0.tap(.callButton)
            }
            .should(provider: .webViewPicker, .exist)
    }
}
