//
//  SaleCardChatPresetsTests.swift
//  UITests
//
//  Created by Alexander Ignatyev on 11/17/20.
//

import XCTest
import Snapshots

/// @depends_on AutoRuSaleCard
final class SaleCardChatPresetsTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    func test_chatPresets() {
        setupServer(mockFolderName: "SaleCardChatPresetsTests")

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101582267-a2e9275f")))
            .scrollTo("chat_presets", windowInsets: .init(top: 0, left: 0, bottom: 70, right: 0))
            .validateSnapshot(of: "chat_presets")
            .tapChatPreset("Ещё продаётся?")
            .should(provider: .chatScreen, .exist)
            .focus(on: .message(.byIndexFromTop(0)), ofType: .chatOutcomingMessageCell) { message in
                message.should(.text, .match("Здравствуйте, ещё продаётся?"))
            }
    }

    func test_chatPresets_chatNotEmpty() {
        setupServer(mockFolderName: "SaleCardChatPresetsTestsEmpty")

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101582267-a2e9275f")))
            .scrollTo("description")
            .notExist(selector: "chat_presets")
    }

    func test_chatPresets_sellerTypeDealer() {
        setupServer(mockFolderName: "SaleCardChatPresetsTestsDealer")
        
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1101582267-a2e9275f")))
            .scrollTo("description")
            .notExist(selector: "chat_presets")
    }

    private func setupServer(mockFolderName: String) {
        advancedMockReproducer.setup(server: self.server, mockFolderName: mockFolderName)
        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

}
