//
//  ExcerptPaymentMethodsTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 26.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREExcerptPaymentModule
@testable import YREPaymentComponents
@testable import YREModel
@testable import YREModelObjc

final class ExcerptPaymentMethodsTests: XCTestCase {
    func testPaymentMethodsLayout() {
        let availablePrice = ExcerptPurchasePrice(
            base: 41000,
            effective: 12300,
            modifiers: [.discount(percent: 70, amount: 28700)]
        )
        let purchase = ExcerptPurchase(
            id: "195a0fbfe44e4bc0bba47b95ecabf974",
            status: .new,
            availablePrice: availablePrice
        )
        let headerViewType = ExcerptPaymentMethodsViewModelGenerator.makeHeaderViewType(purchase: purchase)
        let tableViewModel = ExcerptPaymentMethodsViewModelGenerator.makeTableViewModel(paymentMethods: [
            .service(.bankCard),
            .service(.applePay),
            .service(.sberbank),
            .service(.yooMoney),
        ])
        let footerViewModel = ExcerptPaymentMethodsViewModelGenerator.makeFooterViewModel(purchase: purchase)
        let viewController = PaymentMethodsViewController(
            headerViewType: headerViewType,
            tableViewModel: tableViewModel,
            footerViewModel: footerViewModel
        )
        self.assertSnapshot(viewController.view)
    }
}
