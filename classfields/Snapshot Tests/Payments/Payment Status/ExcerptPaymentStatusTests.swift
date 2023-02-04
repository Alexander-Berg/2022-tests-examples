//
//  ExcerptPaymentStatusTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 21.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREExcerptPaymentModule
@testable import YREPaymentComponents

final class ExcerptPaymentStatusTests: XCTestCase {
    func testExcerptPaymentStatusNoMessageArrivedLayout() {
        let viewModel = PaymentStatusViewModel.smsConfirmation(.noMessageArrived)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusWaitingForMessageLayout() {
        let viewModel = PaymentStatusViewModel.smsConfirmation(.waitingForMessage)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusOverallTimeoutOccuredLayout() {
        let viewModel = ExcerptPaymentStatusViewModelGenerator.generateOverallTimeoutOccuredViewModel()
        let viewController = PaymentStatusViewController(viewModel: .overallTimeoutOccured(viewModel))
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusCardExpiredLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .cardExpired)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusInvalidCardCSCLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .invalidCardCSC)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusInvalidCardCredentialsLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .invalidCardCredentials)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusInvalidCardNumberLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .invalidCardNumber)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusNoInternetConnectionLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .noInternetConnection)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusNotEnoughFundsLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .notEnoughFunds)
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
    
    func testExcerptPaymentStatusUnspecifiedErrorLayout() {
        let error = ExcerptPaymentStatusViewModelGenerator.generateErrorViewModel(error: .unspecified(secondsBeforeRetry: 0))
        let viewModel = PaymentStatusViewModel.failure(error: error)
        let viewController = PaymentStatusViewController(viewModel: viewModel)
        self.assertSnapshot(viewController.view)
    }
}
