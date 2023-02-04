//
//  UserOfferProductInfoTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 11.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREUserOfferProductInfoModule

final class UserOfferProductInfoTests: XCTestCase {
    func testPromotionLayout() {
        let viewModel = UserOfferProductInfoViewModel(
            title: "Продвижение",
            duration: "на 7 дней",
            description: "Ваше объявление оказывается выше любых бесплатных на странице поиска в течение 7-ми дней.",
            type: .promotion(calls: "× 2 звонков"),
            activateControlViewModel: .init(isEnabled: true, title: "Подключить за 117 ₽", oldPrice: "270 ₽")
        )
        let viewController = UserOfferProductInfoViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    func testPremiumLayout() {
        let viewModel = UserOfferProductInfoViewModel(
            title: "Премиум",
            duration: "на 7 дней",
            description: """
            Премиум-объявления показываются на первых трёх позициях каждой страницы выдачи и \
            отмечаются специальным значком. Плюс показываются на главной странице.
            """,
            type: .premium(views: "× 5 просмотров"),
            activateControlViewModel: .init(isEnabled: true, title: "Подключить за 499 ₽", oldPrice: "1200 ₽")
        )
        let viewController = UserOfferProductInfoViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    func testRaisingLayout() {
        let viewModel = UserOfferProductInfoViewModel(
            title: "Поднятие",
            duration: "на 7 дней",
            description: "Ваше объявление 24 часа будет показываться выше других после блока Премиум",
            type: .raising(calls: "x 2 звонков"),
            activateControlViewModel: .init(isEnabled: true, title: "Подключить за 57 ₽", oldPrice: "100 ₽")
        )
        let viewController = UserOfferProductInfoViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    func testTurboLayout() {
        let viewModel = UserOfferProductInfoViewModel(
            title: "Пакет «Турбо»",
            duration: "на 7 дней",
            description: """
            Включает в себя опции Премиум, Продвижение, ежедневное Поднятие в течение недели. \
            Получите в 7 раз больше просмотров и в 3 раза больше звонков!
            """,
            type: .turbo(views: "× 7 просмотров", calls: "× 3 звонков"),
            activateControlViewModel: .init(isEnabled: true, title: "Подключить за 699 ₽", oldPrice: nil)
        )
        let viewController = UserOfferProductInfoViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
}
