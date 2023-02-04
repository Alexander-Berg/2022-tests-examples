//
//  SaleCardDealerButtonsTests.swift
//  UITests
//
//  Created by Alexander Malnev on 1/28/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuSaleCard
final class SaleCardDealerButtonsTests: BaseTest, KeyboardManaging {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        app.launchArguments.append("--recreateDB")
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }
        advancedMockReproducer.setup(server: self.server, mockFolderName: "SaleCardDealerButtons")

        try! server.start()
    }

    func test_shouldClickCallBackButtonAndConfirmPhoneNumber() {
        mocker
            .setForceLoginMode(.forceLoggedOut)

        api.user.phones
            .post
            .ok(mock: .file("user_email_change_ok"))

        api.user.phones.confirm
            .post
            .ok(mock: .file("best_offers_user_phones_confirm"))

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101769771-7e7d4c10")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .sellerInfo)
                    .tap(.sellerInfo)
            }
            .should(provider: .dealerСontactsModal, .exist)
            .focus({ $0.tap(.callBackButton) })
            .wait(for: 1)
            .do { typeFromKeyboard("9998887777") }
            .do { typeFromKeyboard("1") }
            .should(provider: .saleCardScreen, .exist)
    }

    func test_dealerButtonsFromSaleCard() {
        launch()

        var steps = mainSteps.openFilters()
            .showResultsTap()
            .openStockCardOffer(offersTitle: "156 предложений")
            .openOffer(with: "1101769771-7e7d4c10")
            .scrollToDealerSubsriptionButton()
            .validateDealerSubscriptionButton(hasSubscription: false)

        server.addHandler("GET /user/favorites/all/subscriptions") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_favorites_all_subscriptions_with_dealer", userAuthorized: true)
        }

        steps = steps.tapDealerSubsriptionButton()
            .wait(for: 1)
            .validateDealerSubscriptionButton(hasSubscription: true)

        server.addHandler("GET /user/favorites/all/subscriptions") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_favorites_all_subscriptions_empty", userAuthorized: true)
        }

        steps.tapDealerSubsriptionButton()
            .wait(for: 1)
            .validateDealerSubscriptionButton(hasSubscription: false)
            .validateDealerListingButtonTitle(count: 36)
            .tapDealerListingButton()
            .descriptionTitleExists()
    }

    func test_shouldVisibleContactsFromDealerCardWithOpenNoPopUp() {
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101769771-7e7d4c10")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .dealerListingButton)
                    .tap(.dealerListingButton)
            }
            .should(provider: .dealerСardScreen, .exist)
            .focus({ $0.should(.address, .exist) })
    }
}
