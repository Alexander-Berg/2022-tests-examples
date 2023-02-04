//
//  OfferSnippetSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 08.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.OfferSnippetCellAccessibilityIdentifiers
import YRETestsUtils

final class OfferSnippetSteps {
    init(element: XCUIElement) {
        self.element = element
        self.actionsProvider = AnySnippetStepsProvider(cell: element)
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие сниппета оффера") { _ -> Void in
            self.element.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func callButtonLabelStarts(with prefix: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что название кнопки звонка имеет префикс \"\(prefix)\"") { _ -> Void in
            self.actionsProvider.callButton(self.callButton, labelStartsWith: prefix)
        }
        return self
    }

    @discardableResult
    func isCallButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Позвонить\"") { _ -> Void in
            self.actionsProvider.isCallButtonTappable(self.callButton)
        }
        return self
    }

    @discardableResult
    func isFavoritesButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Избранного\"") { _ -> Void in
            self.actionsProvider.isFavoritesButtonTappable(self.favoritesButton)
        }
        return self
    }


    @discardableResult
    func tap() -> Self {
        XCTContext.runActivity(named: "Нажимаем на сниппет оффера") { _ -> Void in
            self.element.yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnAdditionalActionsButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Меню действий\"") { _ -> Void in
            self.additionalActionsButton.yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом сниппета оффера") { _ -> Void in
            let screenshot = self.element.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func tapOnFavoritesButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Избранного\"") { _ -> Void in
            self.favoritesButton.tap()
        }
        return self
    }

    @discardableResult
    func isUserNotePresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что заметка отображается на сниппете") { _ -> Void in
            self.userNote
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func hasUserNote(withText text: String) -> Self {
        XCTContext.runActivity(named: "Проверяем что заметка отображается на сниппете") { _ -> Void in
            self.userNote
                .yreEnsureExistsWithTimeout()
            XCTAssertEqual(self.userNote.label, text)
        }
        return self
    }

    @discardableResult
    func isUserNoteNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что у сниппета нет заметки") { _ -> Void in
            self.userNote.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnUserNote() -> Self {
        XCTContext.runActivity(named: "Нажимаем на заметку") { _ -> Void in
            self.userNote.yreTap()
        }
        return self
    }

    @discardableResult
    func gallery() -> SnippetGallerySteps {
        return SnippetGallerySteps(element: self.element)
    }

    @discardableResult
    func compareUserNoteWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем скриншот блока заметки на сниппете") { _ -> Void in
            let screenshot = self.userNote.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: "userNote." + identifier)
        }
        return self
    }
    
    // MARK: - Private

    private let element: XCUIElement
    private let actionsProvider: AnySnippetStepsProvider

    private lazy var callButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.callButton,
        type: .button,
        in: self.element
    )

    private lazy var favoritesButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.favoritesButton,
        type: .button,
        in: self.element
    )

    private lazy var additionalActionsButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.additionalActionsButton,
        type: .button,
        in: self.element
    )

    private lazy var userNote = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.userNote,
        type: .any,
        in: self.element
    )

    private typealias AccessibilityIdentifiers = OfferSnippetCellAccessibilityIdentifiers
}

extension OfferSnippetSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Позвонить\"") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }
}
