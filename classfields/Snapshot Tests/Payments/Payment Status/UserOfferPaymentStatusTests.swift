//
//  UserOfferPaymentStatusTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 26.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREPaymentComponents
@testable import YREUserOfferPaymentModule

final class UserOfferPaymentStatusTests: XCTestCase {
    func testSmsConfirmationWaitingForMessage() {
        let viewController = PaymentStatusViewController(viewModel: .smsConfirmation(.waitingForMessage))
        self.assertSnapshot(viewController.view)
    }

    func testSmsConfirmationNoMessageArrived() {
        let viewController = PaymentStatusViewController(viewModel: .smsConfirmation(.noMessageArrived))
        self.assertSnapshot(viewController.view)
    }

    func testOverallTimeoutOccuredShouldDisplayConfirmButton() {
        let overallTimeoutOccuredViewModel = Generator.generateOverallTimeoutOccuredViewModel(
            shouldDisplayConfirmButton: true
        )
        let viewController = PaymentStatusViewController(viewModel: .overallTimeoutOccured(overallTimeoutOccuredViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testOverallTimeoutOccuredShouldNotDisplayConfirmButton() {
        let overallTimeoutOccuredViewModel = Generator.generateOverallTimeoutOccuredViewModel(
            shouldDisplayConfirmButton: false
        )
        let viewController = PaymentStatusViewController(viewModel: .overallTimeoutOccured(overallTimeoutOccuredViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorUnspecified() {
        let errorViewModel = Generator.generate(error: .unspecified(secondsBeforeRetry: nil))
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorUnspecifiedWithRetry() {
        let errorViewModel = Generator.generate(error: .unspecified(secondsBeforeRetry: 5))
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorNoInternetConnection() {
        let errorViewModel = Generator.generate(error: .noInternetConnection)
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorCardExpired() {
        let errorViewModel = Generator.generate(error: .cardExpired)
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorNotEnoughFunds() {
        let errorViewModel = Generator.generate(error: .notEnoughFunds)
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorInvalidCardCredentials() {
        let errorViewModel = Generator.generate(error: .invalidCardCredentials)
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorInvalidCardNumber() {
        let errorViewModel = Generator.generate(error: .invalidCardNumber)
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    func testErrorInvalidCardCSC() {
        let errorViewModel = Generator.generate(error: .invalidCardCSC)
        let viewController = PaymentStatusViewController(viewModel: .failure(error: errorViewModel))
        self.assertSnapshot(viewController.view)
    }

    private typealias Generator = UserOfferErrorPaymentStatusViewModelGenerator
}
