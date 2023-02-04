//
//  DocumentPreviewSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 31.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class DocumentPreviewSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        return XCTContext.runActivity(named: "Проверяем наличие экрана с просмотром документов") { _ -> Self in
            self.contentView.yreEnsureExistsWithTimeout()
            return self
        }
    }

    @discardableResult
    func tapCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку закрытия") { _ -> Void in
            self.closeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private lazy var contentView: XCUIElement = ElementsProvider.obtainElement(identifier: DocumentPreviewAccessibilityIdentifiers.view)
    private lazy var closeButton: XCUIElement = ElementsProvider
        .obtainElement(identifier: DocumentPreviewAccessibilityIdentifiers.closeButton)
}
