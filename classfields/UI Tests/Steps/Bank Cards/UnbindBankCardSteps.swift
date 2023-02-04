//
//  Created by Alexey Aleshkov on 30/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.UnbindBankCardAccessibilityIdentifiers

final class UnbindBankCardSteps {
    lazy var unbindProcess = ElementsProvider.obtainElement(
        identifier: UnbindBankCardIdentifiers.viewIdentifier,
        type: .other
    )

    lazy var unbindLoader = ElementsProvider.obtainElement(
        identifier: UnbindBankCardIdentifiers.processLoaderIdentifier,
        type: .other,
        in: self.unbindProcess
    )

    lazy var unbindSuccessResult = ElementsProvider.obtainElement(
        identifier: UnbindBankCardIdentifiers.successResultViewIdentifier,
        type: .other,
        in: self.unbindProcess
    )

    lazy var unbindFailResult = ElementsProvider.obtainElement(
        identifier: UnbindBankCardIdentifiers.failureResultViewIdentifier,
        type: .other,
        in: self.unbindProcess
    )

    lazy var unbindRetryButton = ElementsProvider.obtainElement(
        identifier: UnbindBankCardIdentifiers.failureRetryButtonIdentifier,
        type: .button,
        in: self.unbindProcess
    )

    lazy var unbindCloseButton = ElementsProvider.obtainElement(
        identifier: UnbindBankCardIdentifiers.closeButtonIdentifier,
        type: .button,
        in: self.unbindProcess
    )

    // MARK: - Private

    private typealias UnbindBankCardIdentifiers = UnbindBankCardAccessibilityIdentifiers
}

extension UnbindBankCardSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        self.unbindProcess.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func screenIsDismissed() -> Self {
        self.unbindProcess.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func loaderIsPresented() -> Self {
        self.unbindLoader.yreEnsureExistsWithTimeout()
        self.unbindCloseButton.yreEnsureNotExists()
        return self
    }

    @discardableResult
    func successResultIsPresented(timeout: TimeInterval) -> Self {
        self.unbindSuccessResult.yreEnsureExistsWithTimeout(timeout: timeout)
        self.unbindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func failResultIsPresented(timeout: TimeInterval) -> Self {
        self.unbindFailResult.yreEnsureExistsWithTimeout(timeout: timeout)
        self.unbindCloseButton.yreEnsureExists()
        return self
    }

    @discardableResult
    func tapOnRetryButton() -> Self {
        self.unbindRetryButton.yreEnsureExists().yreTap()
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        self.unbindCloseButton.yreEnsureExists().yreTap()
        return self
    }
}
