//
//  DeepLinkProcessingSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 27.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class DeepLinkProcessingSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Экран с загрузкой диплинка показан") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Экран с загрузкой диплинка скрыт") { _ -> Void in
            self.screenView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func close() -> Self {
        XCTContext.runActivity(named: "Закрываем экран") { _ -> Void in
            self.closeButton.yreTap()
        }
        return self
    }
    
    @discardableResult
    func openInBrowser() -> Self {
        XCTContext.runActivity(named: "Тап по кнопке 'Открыть в браузере'") { _ -> Void in
            self.openInBrowserButton.yreTap()
        }
        return self
    }
    
    @discardableResult
    func compareSnapshot(snapshotID: String) -> Self {
        XCTContext.runActivity(named: "Делаем скриншот экрана") { _ -> Void in
            let ignoredEdges = XCUIApplication().yre_ignoredEdges()
            let snapshot = self.screenView.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: snapshot, identifier: snapshotID, ignoreEdges: ignoredEdges)
        }
        return self
    }
    

    // MARK: - Private
    
    private typealias AccessibilityIdentifiers = DeepLinkProcessingAccessibilityIdentifiers
    
    private var updateAppButton: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.updateAppButton,
            type: .button,
            in: self.screenView
        )
    }
    
    private var openInBrowserButton: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.openInBrowserButton,
            type: .button,
            in: self.screenView
        )
    }
    
    private var continueButton: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.continueButton,
            type: .button,
            in: self.screenView
        )
    }
    
    private var closeButton: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.closeButton,
            type: .button,
            in: self.screenView
        )
    }
    
    private var screenView: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
    }
}
