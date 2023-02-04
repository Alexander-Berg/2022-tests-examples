//
//  AnyCardCallbackTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
@testable import YRECardComponents
@testable import YREDesignKit

final class AnyCardCallbackTests: XCTestCase {
    func testCallbackNode() {
        let viewModel: CardCallbackView.ViewModel = .init(phone: PhoneNumber.empty, state: .initial)
        self.assertCallbackNodeSnapshot(viewModel)
    }

    func testSuccessSentCallbackNode() {
        let viewModel: CardCallbackView.ViewModel = .init(phone: PhoneNumber.valid, state: .success(.sent))
        self.assertCallbackNodeSnapshot(viewModel)
    }

    func testSuccessInQueueCallbackNode() {
        let viewModel: CardCallbackView.ViewModel = .init(phone: PhoneNumber.valid, state: .success(.inQueue))
        self.assertCallbackNodeSnapshot(viewModel)
    }

    func testErrorEmptyPhoneCallbackNode() {
        let viewModel: CardCallbackView.ViewModel = .init(phone: PhoneNumber.empty, state: .error(.emptyPhone))
        self.assertCallbackNodeSnapshot(viewModel)
    }

    func testErrorInvalidPhoneCallbackNode() {
        let viewModel: CardCallbackView.ViewModel = .init(phone: PhoneNumber.invalid, state: .error(.invalidPhone))
        self.assertCallbackNodeSnapshot(viewModel)
    }

    func testCallbackDisclaimer() {
        let factory = CardCallbackDisclaimerFactory()
        let disclaimerController = factory.makeCardCallbackDisclaimer()
        self.assertSnapshot(disclaimerController.view)
    }

    private enum PhoneNumber {
        static let empty = ""
        static let valid = "79999999999"
        static let invalid = "1234567890"
    }

    private func assertCallbackNodeSnapshot(_ viewModel: CardCallbackView.ViewModel, function: String = #function) {
        let view = CardCallbackView()
        view.configure(viewModel: viewModel)
        view.backgroundColor = ColorScheme.Background.primary
        view.frame = Self.frame { CardCallbackView.height(width: $0, viewModel: viewModel) }
        self.assertSnapshot(view, function: function)
    }
}
