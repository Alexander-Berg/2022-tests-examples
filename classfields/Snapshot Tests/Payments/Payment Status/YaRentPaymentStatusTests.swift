//
//  YaRentPaymentStatusTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 10.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

// swiftlint:disable force_unwrapping

import XCTest
@testable import YREYandexRentPaymentModule
@testable import YREPaymentComponents
@testable import YREModuleFactory
@testable import YREModel

final class YaRentPaymentStatusTests: XCTestCase {
    func testPaymentStatusPaid() {
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .rent(.init(id: "", amount: 1000, dateInterval: .init())),
            nextPaymentDate: nil,
            pushNotificationStatus: .enabled
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusWithEnabledPushNotifications() {
        let nextPaymentDate = Self.dateFormatter.date(from: "2021-09-28")!
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .rent(.init(id: "", amount: 1000, dateInterval: .init())),
            nextPaymentDate: nextPaymentDate,
            pushNotificationStatus: .enabled
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusWithDisablePushNotifications() {
        let nextPaymentDate = Self.dateFormatter.date(from: "2021-09-28")!
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .rent(.init(id: "", amount: 1000, dateInterval: .init())),
            nextPaymentDate: nextPaymentDate,
            pushNotificationStatus: .disabled
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusWithNotRequestedPushNotifications() {
        let nextPaymentDate = Self.dateFormatter.date(from: "2021-09-28")!
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .rent(.init(id: "", amount: 1000, dateInterval: .init())),
            nextPaymentDate: nextPaymentDate,
            pushNotificationStatus: .notRequested
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testUtilitiesPaymentStatusWithEnabledPushNotifications() {
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .utilities(.init(id: "", amount: 1000)),
            nextPaymentDate: nil,
            pushNotificationStatus: .enabled
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testUtilitiesPaymentStatusWithDisablePushNotifications() {
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .utilities(.init(id: "", amount: 1000)),
            nextPaymentDate: nil,
            pushNotificationStatus: .disabled
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testUtilitiesPaymentStatusWithNotRequestedPushNotifications() {
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateSuccessViewModel(
            payment: .utilities(.init(id: "", amount: 1000)),
            nextPaymentDate: nil,
            pushNotificationStatus: .notRequested
        )
        let viewController = PaymentStatusViewController(viewModel: .success(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusOverallTimeoutOccuredLayout() {
        let payment = YandexRentPayments.Payment.rent(.init(id: "", amount: 1000, dateInterval: .init()))
        let viewModel = YaRentPaymentStatusViewModelGenerator.generateOverallTimeoutOccuredViewModel(payment: payment)
        let viewController = PaymentStatusViewController(viewModel: .overallTimeoutOccured(viewModel))
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusNoInternetConnectionLayout() {
        let error = YaRentPaymentStatusViewModelGenerator.generateErrorViewModel(error: .noInternetConnection)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusUnspecifiedErrorLayout() {
        let error = YaRentPaymentStatusViewModelGenerator.generateErrorViewModel(error: .unspecified)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusUnknownPaymentError() {
        let error = YaRentPaymentStatusViewModelGenerator.generateErrorViewModel(error: .paymentError(.unknown))
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    func testPaymentStatusPaymentError() {
        let error = YaRentPaymentStatusViewModelGenerator.generateErrorViewModel(
            error: .paymentError(.userShowing(.init(message: "Ошибка")))
        )
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }

    private static let dateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        return dateFormatter
    }()
}
