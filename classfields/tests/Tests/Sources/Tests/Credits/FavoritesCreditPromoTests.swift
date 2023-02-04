//
//  FavoritesPromoCreditLayoutTest.swift
//  Tests
//
//  Created by Roman Bevza on 2/2/21.
//

import Foundation
import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import Snapshots
@testable import AutoRuFavoriteSaleList
import AutoRuColorSchema

final class FavoritesCreditPromoTests: BaseUnitTest {
    func test_promo() {
        XCTContext.runActivity(named: "Проверка лейаута промо кредитов во вкладке сохраненных поисков") { _ in }
        let model = FavoritesCreditPromoViewModel(
            title: "Кредит на спецусловиях от 9.9%",
            monthPayment: 10700,
            maxSum: 500000,
            firstPayment: 0,
            buttonTitle: "Заполнить заявку",
            isDraftExist: false,
            actions: .init(onTap: {},
                           onClose: {}
            )
        )
        Snapshot.compareWithSnapshot(
            layout: FavoritesCreditPromoLayout(model: model),
            backgroundColor: ColorSchema.Background.surface,
            interfaceStyle: [.light, .dark],
            overallTolerance: 0.001
        )
    }

    func test_draftPromo() {
        XCTContext.runActivity(named: "Проверка лейаута промо кредитов во вкладке сохраненных поиско") { _ in }
        let model = FavoritesCreditPromoViewModel(
            title: "Кредит на спецусловиях от 9.9%",
            monthPayment: 10700,
            maxSum: 500000,
            firstPayment: 0,
            buttonTitle: "Дозаполнить заявку",
            isDraftExist: false,
            actions: .init(onTap: {},
                           onClose: {}
            )
        )
        Snapshot.compareWithSnapshot(
            layout: FavoritesCreditPromoLayout(model: model),
            backgroundColor: ColorSchema.Background.surface,
            interfaceStyle: [.light, .dark],
            overallTolerance: 0.001
        )
    }
}
