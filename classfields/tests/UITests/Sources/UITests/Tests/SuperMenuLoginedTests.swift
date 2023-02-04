//
//  SuperMenuLoginedTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 1/31/22.
//

import AutoRuProtoModels
import XCTest
import Snapshots

/// @depends_on AutoRuAppRouting AutoRuSuperMenu
final class SuperMenuLoginedTests: BaseTest {
    var sharkMocker: SharkMocker!
    var settings: [String: Any] = [:]

    override var appSettings: [String: Any] {
        get {
            return self.settings
        }
        set { self.settings = newValue }
    }

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["offersHistoryEnabled"] = true
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
        sharkMocker = SharkMocker(server: server)
    }

    func test_estimateWebExp() {
        launchMain(options: .init(
            launchType: .default,
            overrideAppSettings: ["estimateInWebView": true]))
        .toggle(to: \.favorites)
        .should(provider: .navBar, .exist)
        .focus { $0.tap(.superMenuButton) }
        .should(provider: .superMenuScreen, .exist)
        .focus { $0.tap(.price)}
        .should(provider: .webViewPicker, .exist)
        .focus { picker in
            picker.step("Проверяем открытую ссылку") { picker in
                XCTAssert(
                    picker.currentURL.starts(with: "https://auto.ru/cars/evaluation/?exp_flags=CTBAUTORU-102"),
                    "Открыли неверную ссылку"
                )
            }
        }
    }

    func test_showLoginedInterface() {
        let products: [SharkMocker.Product] = [.tinkoff_auto, .tinkoff_cash, .tinkoff_creditCard, .tinkoff_refin]
        let offerId = "1098252972-99d8c274"
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: products)
            .mockApplication(status: .active,
                             claims: [],
                             offerType: .none)
            .start()
        settings["skipCreditAlert"] = true

        openSuperMenu()
            .step("Проверяем хедер таблицы") { $0
                .focus(on: .header, ofType: .superMenuHeaderCell) { cell in
                    cell.validateSnapshot(snapshotId: "test_showLoginedInterface_Header")
                }
            }
            .step("Проверяем профиль") { $0
                .tap(.auth)
                .should(provider: .userProfileScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
            .step("Проверяем кошелёк") { $0
                .tap(.wallet)
                .should(provider: .walletScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
            .step("Проверяем мои отзывы") { $0
                .tap(.myReviews)
                .should(provider: .userReviewsScreen, .exist)
                .focus { $0.tap(.closeButton) }
            }
            .step("Проверяем кредиты") { $0
                .tap(.credit)
                .should(provider: .creditLKScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
    }

    private func openSuperMenu() -> SuperMenuScreen {
        launchMain { screen in
            screen
                .toggle(to: \.favorites)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.superMenuButton) }
                .should(provider: .superMenuScreen, .exist)
        }
    }
}
