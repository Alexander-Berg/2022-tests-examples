//
//  YaRentPaymentMethodsTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 27.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

// swiftlint:disable force_unwrapping

import XCTest
@testable import YREYandexRentPaymentModule
@testable import YREPaymentComponents
@testable import YREModel

final class YaRentPaymentMethodsTests: XCTestCase {
    func testPaymentMethodsLayout() {
        let startDate = Self.dateFormatter.date(from: "2021-09-28")!
        let endDate = Self.dateFormatter.date(from: "2021-10-28")!
        let paymentInfo = YandexRentPayments.PaymentInfo(
            flatAddress: "Очень-очень-очень-очень-очень длинный текст",
            payment: .rent(
                .init(id: "", amount: 41000, dateInterval: .init(start: startDate, end: endDate))
            ),
            contractTermsURL: URL(string: "https://yandex.ru")
        )
        let headerViewType = YaRentPaymentMethodsViewModelGenerator.makeHeaderViewType(paymentInfo: paymentInfo)
        let tableViewModel = YaRentPaymentMethodsViewModelGenerator.makeTableViewModel(
            paymentMethods: [
                PaymentInitiatedPurchase.PaymentMethod.sbp,
                .bankCard
            ],
            utilizedMethod: .sbp
        )
        let footerViewModel = YaRentPaymentMethodsViewModelGenerator.makeFooterViewModel(paymentInfo: paymentInfo)
        let viewController = PaymentMethodsViewController(
            headerViewType: headerViewType,
            tableViewModel: tableViewModel,
            footerViewModel: footerViewModel
        )
        self.assertSnapshot(viewController.view)
    }
    
    private static let dateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        return dateFormatter
    }()
}
