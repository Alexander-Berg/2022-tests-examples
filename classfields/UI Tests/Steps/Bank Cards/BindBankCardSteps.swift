//
//  Created by Alexey Aleshkov on 30/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.BindBankCardAccessibilityIdentifiers

final class BindBankCardSteps {
    lazy var bindProcess = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.viewIdentifier,
        type: .other
    )

    lazy var bindLoader = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.processLoaderIdentifier,
        type: .other,
        in: self.bindProcess
    )

    lazy var bindWaitForMessageResult = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.waitForMessageResultViewIdentifier,
        type: .other,
        in: self.bindProcess
    )

    lazy var bindNoMessageButton = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.noMessageButtonIdentifier,
        type: .button,
        in: self.bindWaitForMessageResult
    )

    lazy var bindNoMessageResult = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.noMessageArrivedResultViewIdentifier,
        type: .other,
        in: self.bindProcess
    )

    lazy var bindProgressResult = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.progressResultViewIdentifier,
        type: .other,
        in: self.bindProcess
    )

    lazy var bindProgressProceed = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.progressProceedButtonIdentifier,
        type: .button,
        in: self.bindProgressResult
    )

    lazy var bindSuccessResult = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.successResultViewIdentifier,
        type: .other,
        in: self.bindProcess
    )
    
    lazy var bindSuccessProceed = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.successProceedButtonIdentifier,
        type: .button,
        in: self.bindSuccessResult
    )

    lazy var bindFailResult = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.failureResultViewIdentifier,
        type: .other,
        in: self.bindProcess
    )

    lazy var bindFailRetry = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.failureRetryButtonIdentifier,
        type: .button,
        in: self.bindFailResult
    )

    lazy var bindCloseButton = ElementsProvider.obtainElement(
        identifier: BindBankCardIdentifiers.closeButtonIdentifier,
        type: .button,
        in: self.bindProcess
    )

    // MARK: - Private

    private typealias BindBankCardIdentifiers = BindBankCardAccessibilityIdentifiers
}

extension BindBankCardSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        self.bindProcess.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func screenIsDismissed(timeout: TimeInterval = Constants.timeout) -> Self {
        self.bindProcess.yreEnsureNotExistsWithTimeout(timeout: timeout)
        return self
    }

    @discardableResult
    func loaderIsPresented() -> Self {
        self.bindLoader.yreEnsureExistsWithTimeout()
        self.bindCloseButton.yreEnsureNotExists()
        return self
    }

    @discardableResult
    func waitForMessageIsPresented() -> Self {
        self.bindWaitForMessageResult.yreEnsureExistsWithTimeout()
        self.bindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func tapOnNoMessageProceedButton() -> Self {
        self.bindNoMessageButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func noMessageIsPresented() -> Self {
        self.bindNoMessageResult.yreEnsureExistsWithTimeout()
        self.bindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func progressResultIsPresented(timeout: TimeInterval) -> Self {
        self.bindProgressResult.yreEnsureExistsWithTimeout(timeout: timeout)
        self.bindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func tapOnProgressProceedButton() -> Self {
        self.bindProgressProceed
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func successResultIsPresented(timeout: TimeInterval) -> Self {
        self.bindSuccessResult.yreEnsureExistsWithTimeout(timeout: timeout)
        self.bindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func tapOnSuccessProceedButton() -> Self {
        self.bindSuccessProceed
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func failResultIsPresented(timeout: TimeInterval) -> Self {
        self.bindFailResult.yreEnsureExistsWithTimeout(timeout: timeout)
        self.bindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func failRetryButtonIsVisible() -> Self {
        self.bindFailRetry
            .yreEnsureVisible()
        return self
    }

    @discardableResult
    func failRetryButtonIsHidden() -> Self {
        self.bindFailRetry
            .yreEnsureNotVisible()
        return self
    }

    @discardableResult
    func tapOnFailRetryButton() -> Self {
        self.bindFailRetry
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        self.bindCloseButton.yreEnsureExists().yreTap()
        return self
    }
}
