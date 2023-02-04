//
//  SuperMenuNologinTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 16.02.2022.
//

import XCTest
import Snapshots

/// @depends_on AutoRuAppRouting AutoRuSuperMenu
final class SuperMenuNologinTests: BackendStatefulTests {

    func test_showAllWebScreens() {
        state.user.authorized = false
        openSuperMenu()
            .tap(.insurance)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
            .tap(.catalog)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
            .tap(.youtube)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
            .tap(.journal)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
            .scroll(to: .confidentialPolitics, direction: .up)
            .tap(.help)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
            .tap(.licenseAgreement)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
            .tap(.confidentialPolitics)
            .should(provider: .webViewPicker, .exist)
            .focus { $0.tap(.closeButton) }
    }

    func test_showNologinInterface() {
        state.user.authorized = false
        openSuperMenu()
            .step("Проверяем хедер таблицы") { $0
                .should(.logInTitle, .exist(timeout: 7))
                .focus(on: .header, ofType: .superMenuHeaderCell) { cell in
                    cell.validateSnapshot(snapshotId: "test_showNologinInterface_Header")
                }
            }
            .step("Проверяем профиль") { $0
                .tap(.auth)
                .should(provider: .loginScreen, .exist)
                .focus { $0.tap(.closeButton) }
            }
            .step("Проверяем кошелёк") { $0
                .tap(.wallet)
                .should(provider: .loginScreen, .exist)
                .focus { $0.tap(.closeButton) }
            }
            .step("Проверяем мои отзывы") { $0
                .tap(.myReviews)
                .should(provider: .loginScreen, .exist)
                .focus { $0.tap(.closeButton) }
                .should(provider: .userReviewsScreen, .exist)
                .focus { $0.tap(.closeButton) }
            }
            .step("Проверяем безопасную сделку") { $0
                .tap(.safeDeal)
                .should(provider: .safeDealListScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
            .step("Проверяем стоимость") { $0
                .tap(.price)
                .should(provider: .estimateFormScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
            .scroll(to: .about, direction: .up)
            .step("Проверяем уведомления") { $0
                .tap(.notifications)
                .should(provider: .notificationSettingsScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
            .step("Проверяем о приложении") { $0
                .tap(.about)
                .should(provider: .aboutScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.close) }
            }
            .step("Проверяем отчёты про авто") { $0
                .scroll(to: .reports, direction: .down)
                .focus(on: .reports, ofType: .superMenuPromoCell) { cell in
                    cell.validateSnapshot(snapshotId: "test_showNologinInterface_Promo")
                }
                .tap(.reports)
                .should(provider: .carReportStandAloneScreen, .exist)
            }
    }
    
    func test_openMenuButtonExist() {
        state.user.authorized = false
        let screen = launchMain()
        screen
            .should(provider: .mainScreen, .exist)
            .focus { $0.should(.superMenuButton, .exist) }
        screen.toggle(to: \.favorites)
            .should(provider: .navBar, .exist)
            .focus { $0.should(.superMenuButton, .exist) }
        screen.toggle(to: \.chats)
            .should(provider: .loginScreen, .exist)
            .focus { $0.tap(.closeButton) }
            .should(provider: .navBar, .exist)
            .focus { $0.should(.superMenuButton, .exist) }
        screen.toggle(to: \.garage)
            .should(provider: .navBar, .exist)
            .focus { $0.should(.superMenuButton, .exist) }
        screen.toggle(to: \.offersAttentions)
            .should(provider: .navBar, .exist)
            .focus { $0.should(.superMenuButton, .exist) }
    }
    
    func test_successfulLoginNormalUserFromAuth() {
        state.user.authorized = false

        openSuperMenu()
            .tap(.auth)
            .should(provider: .loginScreen, .exist)
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .userProfileScreen, .exist)
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.close) }
            .step("Проверяем хедер таблицы") { $0
                .focus(on: .header, ofType: .superMenuHeaderCell) { cell in
                    cell.validateSnapshot(snapshotId: "test_successfulLoginNormalUser_Header")
                }
            }
        XCTAssertTrue(state.user.authorized)
    }
    
    func test_successfulLoginNormalUserFromWallet() {
        state.user.authorized = false

        openSuperMenu()
            .tap(.wallet)
            .should(provider: .loginScreen, .exist)
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .walletScreen, .exist)
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.close) }
            .step("Проверяем хедер таблицы") { $0
                .focus(on: .header, ofType: .superMenuHeaderCell) { cell in
                    cell.validateSnapshot(snapshotId: "test_successfulLoginNormalUser_Header")
                }
            }
        XCTAssertTrue(state.user.authorized)
    }
    
    func test_successfulLoginDealerUserFromAuth() {
        state.user.isDealer = true
        state.user.authorized = false

        openSuperMenu()
            .tap(.auth)
            .should(provider: .loginScreen, .exist)
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .navBar, .exist)
            .focus { $0.should(.superMenuButton, .be(.hidden)) }
        XCTAssertTrue(state.user.authorized)
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
