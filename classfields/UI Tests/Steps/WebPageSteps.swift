//
//  Created by Alexey Aleshkov on 30/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

class WebPageSteps {
    lazy var rootView = ElementsProvider.obtainElement(
        identifier: WebPageIdentifiers.viewIdentifier,
        type: .other
    )

    lazy var webView = ElementsProvider.obtainElement(
        identifier: WebPageIdentifiers.webViewIdentifier,
        type: .webView,
        in: self.rootView
    )

    lazy var closeButton = ElementsProvider.obtainElement(
        identifier: WebPageIdentifiers.closeButtonIdentifier,
        type: .other
    )

    lazy var shareButton = ElementsProvider.obtainElement(
        identifier: WebPageIdentifiers.shareButtonIdentifier,
        type: .other
    )

    lazy var activityVC = ElementsProvider.obtainElement(
        identifier: WebPageIdentifiers.activityVCIdentifier
    )

    // MARK: - Private

    private typealias WebPageIdentifiers = WebPageAccessibilityIdentifiers
}

extension WebPageSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что открылось WebView") { _ -> Void in
            self.webView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func screenIsDismissed() -> Self {
        XCTContext.runActivity(named: "Проверяем, WebView закрыт") { _ -> Void in
            self.webView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Закрыть'") { _ -> Void in
            self.closeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnShareButton() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие кнопки шаринга") { _ -> Void in
            self.shareButton
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isActivityVCPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем появление экрана шарилки") { (_) -> Void in
            self.activityVC
                .yreEnsureExistsWithTimeout()
        }
        return self
    }
}
