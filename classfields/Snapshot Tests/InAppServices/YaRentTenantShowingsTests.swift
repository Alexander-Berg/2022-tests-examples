//
//  YaRentTenantShowingsTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 06.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel
@testable import YREYaRentTenantShowingsModule

final class YaRentTenantShowingsTests: XCTestCase {
    func testShowingView() {
        let notification = YaRentShowing.Notification(
            htmlText: "Вы ранее смотрели эту квартиру, продолжите подачу заявки на просмотр",
            style: .accent,
            action: .init(text: "Заполнить заявку", kind: .appUpdate)
        )
        let showing = YaRentShowing(
            showingID: "",
            flatID: "",
            offerID: "",
            address: "Москва, Садовническая набережная 34",
            roommates: ["Александр", "Василина"],
            photos: [],
            rentAmount: 8_000_000,
            headerText: nil,
            notification: notification
        )
        self.assertShowingSnapshot(showing)
    }

    func testShowingViewSnippet() {
        let showing = YaRentShowing(
            showingID: "",
            flatID: "",
            offerID: "",
            address: "Москва, Садовническая набережная 34",
            roommates: [],
            photos: [],
            rentAmount: 5_000_000,
            headerText: nil,
            notification: nil
        )
        self.assertShowingSnapshot(showing)
    }

    func testShowingViewWithHeader() {
        let notification = YaRentShowing.Notification(
            htmlText: "Поздравляем! Собственник готов сдать вам квартиру. Перед подписанием необходимо заполнить ЖКХ",
            style: .success,
            action: .init(text: "Настроить", kind: .acceptHouseUtilities)
        )
        let showing = YaRentShowing(
            showingID: "",
            flatID: "",
            offerID: "",
            address: "Москва, Садовническая набережная 34",
            roommates: ["Александр", "Василина"],
            photos: [],
            rentAmount: 7_500_000,
            headerText: "Собственник готов сдать эту квартиру",
            notification: notification
        )
        self.assertShowingSnapshot(showing)
    }

    func testShowingViewWithInfoNotification() {
        let notification = YaRentShowing.Notification(
            htmlText: """
            Поздравляем! Собственник хочет сдать вам квартиру. \
            Следующий шаг — подписать договор аренды. \
            <a href="https://">Шаблон тут</a>. \
            Менеджер позвонит вам, чтобы уточнить детали.
            """,
            style: .info,
            action: nil
        )
        let showing = YaRentShowing(
            showingID: "",
            flatID: "",
            offerID: "",
            address: "Москва, Комендантский пр‑т, д. 60 корп. 1",
            roommates: [],
            photos: [],
            rentAmount: 12_000_000,
            headerText: nil,
            notification: notification
        )
        self.assertShowingSnapshot(showing)
    }

    func testShowingViewWithWarningNotification() {
        let notification = YaRentShowing.Notification(
            htmlText: "Не можем до вас дозвониться. Пожалуйста, перезвоните нам: +7 903 363 57 92. Будем ждать!",
            style: .warning,
            action: nil
        )
        let showing = YaRentShowing(
            showingID: "",
            flatID: "",
            offerID: "",
            address: "Москва, Садовническая набережная 34",
            roommates: ["Игорь"],
            photos: [],
            rentAmount: 9_000_000,
            headerText: nil,
            notification: notification
        )
        self.assertShowingSnapshot(showing)
    }

    func testShowingViewWithImportantNotification() {
        let notification = YaRentShowing.Notification(
            htmlText: "Поздравляем! Собственник хочет сдать вам квартиру. Перед подписанием необходимо принять условия по куммуналке",
            style: .important,
            action: .init(text: "Принять условия", kind: .acceptHouseUtilities)
        )
        let showing = YaRentShowing(
            showingID: "",
            flatID: "",
            offerID: "",
            address: "Москва, Садовническая набережная 34",
            roommates: [],
            photos: [],
            rentAmount: 9_000_000,
            headerText: nil,
            notification: notification
        )
        self.assertShowingSnapshot(showing)
    }
}

extension YaRentTenantShowingsTests {
    private func assertShowingSnapshot(_ showing: YaRentShowing, function: String = #function) {
        let model = YaRentTenantShowingsViewModelGenerator.makeShowing(
            from: showing,
            snippetActionHandler: {},
            roommatesActionHandler: {},
            notificationActionHandler: { _ in }
        )

        let view = YaRentTenantShowingView()
        view.viewModel = model
        view.frame = Self.frame { YaRentTenantShowingView.height(width: $0, viewModel: model) }
        self.assertSnapshot(view, function: function)
    }
}
