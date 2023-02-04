//
//  MortgageListSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 29.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.MortgageListAccessibilityIdentifiers

final class MortgageListSteps {
    @discardableResult
    func isListNonEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список ипотечных программ не пуст") { _ -> Void in
            self.mortgageProgramCell.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func scrollToMortgageProgramCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к первой ипотечной программе") { _ -> Void in
            self.mortgageProgramCell.yreEnsureExistsWithTimeout()
            self.collectionView.scroll(to: self.mortgageProgramCell)
        }
        return self
    }

    @discardableResult
    func tapOnSubmitButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на 'Оформить заявку'") { _ -> Void in
            self.submitButton.yreEnsureExistsWithTimeout()
            self.submitButton.yreTap()
        }
        return self
    }

    private lazy var screen = ElementsProvider.obtainElement(identifier: MortgageListAccessibilityIdentifiers.view)

    private lazy var collectionView = ElementsProvider.obtainElement(
        identifier: MortgageListAccessibilityIdentifiers.collectionView,
        in: self.screen
    )

    private lazy var mortgageProgramCell = ElementsProvider.obtainElement(
        identifier: MortgageListAccessibilityIdentifiers.mortgageProgramCell,
        in: self.collectionView
    )

    private lazy var submitButton = ElementsProvider.obtainElement(
        identifier: MortgageListAccessibilityIdentifiers.mortgageProgramButton,
        in: self.mortgageProgramCell
    )

    private lazy var disclaimerCell = ElementsProvider.obtainElement(
        identifier: MortgageListAccessibilityIdentifiers.disclaimerCell,
        in: self.collectionView
    )
}
