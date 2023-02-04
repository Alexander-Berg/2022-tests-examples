//
//  InAppServicesGeneratorTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 27.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREInAppServicesModule

final class InAppServicesGeneratorTests: XCTestCase {
    func testOffersSortingWithoutRentPromo() {
        let initialModels: [InAppServicesOfferViewModelGenerator.RankedModel<String>] = [
            .init(status: .unknown, eligibleForYaRentPromo: false, model: "Кол-во просмотров"),
            .init(status: .activationRequired, eligibleForYaRentPromo: false, model: "Требует активации"),
            .init(status: .active(discount: 0), eligibleForYaRentPromo: false, model: "Кол-во просмотров"),
            .init(status: .noPhotos, eligibleForYaRentPromo: false, model: "Нет фотографий"),
            .init(status: .paymentInProcess, eligibleForYaRentPromo: false, model: "В процессе оплаты"),
            .init(status: .freeExpiresSoon(secondsUntilExpiry: 0), eligibleForYaRentPromo: false, model: "Дни до снятия"),
            .init(status: .moderation, eligibleForYaRentPromo: false, model: "На публикации"),
            .init(status: .inactive, eligibleForYaRentPromo: false, model: "Снято с публикации"),
            .init(status: .banned(editable: true), eligibleForYaRentPromo: false, model: "Заблокировано"),
            .init(status: .activationInProcess, eligibleForYaRentPromo: false, model: "В процессе активации"),
            .init(status: .productRenewalError, eligibleForYaRentPromo: false, model: "Ошибка при автопродлении"),
        ]

        let sortedModels = InAppServicesOfferViewModelGenerator.getSortedModels(from: initialModels)

        XCTAssertEqual(sortedModels[0], "Требует активации")
        XCTAssertEqual(sortedModels[1], "Ошибка при автопродлении")
        XCTAssertEqual(sortedModels[2], "Дни до снятия")
        XCTAssertEqual(sortedModels[3], "Кол-во просмотров")
        XCTAssertEqual(sortedModels[4], "Кол-во просмотров")
        XCTAssertEqual(sortedModels[5], "Снято с публикации")
        XCTAssertEqual(sortedModels[6], "Нет фотографий")
        XCTAssertEqual(sortedModels[7], "Заблокировано")
        XCTAssertEqual(sortedModels[8], "В процессе оплаты")
        XCTAssertEqual(sortedModels[9], "В процессе активации")
        XCTAssertEqual(sortedModels[10], "На публикации")
    }

    func testOffersSortingWithRentPromo() {
        let initialModels: [InAppServicesOfferViewModelGenerator.RankedModel<String>] = [
            .init(status: .moderation, eligibleForYaRentPromo: false, model: "На публикации"),
            .init(status: .activationRequired, eligibleForYaRentPromo: false, model: "Требует активации"),
            .init(status: .noPhotos, eligibleForYaRentPromo: true, model: "Промо Аренды 1"),
            .init(status: .productRenewalError, eligibleForYaRentPromo: false, model: "Ошибка при автопродлении"),
            .init(status: .freeExpiresSoon(secondsUntilExpiry: 0), eligibleForYaRentPromo: false, model: "Дни до снятия"),
            .init(status: .inactive, eligibleForYaRentPromo: true, model: "Промо Аренды 2"),
            .init(status: .banned(editable: true), eligibleForYaRentPromo: false, model: "Заблокировано"),
            .init(status: .active(discount: 0), eligibleForYaRentPromo: false, model: "Кол-во просмотров"),
        ]

        let sortedModels = InAppServicesOfferViewModelGenerator.getSortedModels(from: initialModels)

        XCTAssertEqual(sortedModels[0], "Промо Аренды 1")
        XCTAssertEqual(sortedModels[1], "Промо Аренды 2")
        XCTAssertEqual(sortedModels[2], "Требует активации")
        XCTAssertEqual(sortedModels[3], "Ошибка при автопродлении")
        XCTAssertEqual(sortedModels[4], "Дни до снятия")
        XCTAssertEqual(sortedModels[5], "Кол-во просмотров")
        XCTAssertEqual(sortedModels[6], "Заблокировано")
        XCTAssertEqual(sortedModels[7], "На публикации")
    }
}
