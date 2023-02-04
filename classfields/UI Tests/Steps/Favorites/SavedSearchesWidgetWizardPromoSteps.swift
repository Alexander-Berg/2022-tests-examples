//
//  SavedSearchesWidgetWizardPromoSteps.swift
//  UI Tests
//
//  Created by Anfisa Klisho on 06.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.SavedSearchesWidgetWizardAccessibilityIdentifiers

final class SavedSearchesWidgetWizardPromoSteps {
    @discardableResult
    func wizardPromoIsShown() -> Self {
        XCTContext.runActivity(named: "Проверяем, что визард промо показывается") { _ -> Void in
            self.wizardView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func wizardPromoClosed() -> Self {
        XCTContext.runActivity(named: "Проверяем, что визард закрылся") { _ -> Void in
            self.wizardView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapWizardActionButton1() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Далее на 1 шаге в визарде") { _ -> Void in
            self.wizardActionButton1
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapWizardActionButton2() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Далее на 2 шаге в визарде") { _ -> Void in
            self.wizardActionButton2
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapWizardActionButton3() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Понятно на 3 шаге в визарде") { _ -> Void in
            self.wizardActionButton3
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func compareWizardStepWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем визард со снапшотом") { _ in
            self.wizardView
                .yreEnsureExistsWithTimeout()
                .yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = SavedSearchesWidgetWizardAccessibilityIdentifiers

    private lazy var wizardView = ElementsProvider.obtainElement(identifier: Identifiers.wizardView)
    private lazy var wizardActionButton1 = ElementsProvider.obtainButton(identifier: Identifiers.wizardActionButton1)
    private lazy var wizardActionButton2 = ElementsProvider.obtainButton(identifier: Identifiers.wizardActionButton2)
    private lazy var wizardActionButton3 = ElementsProvider.obtainButton(identifier: Identifiers.wizardActionButton3)
}
