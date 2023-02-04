//
//  SaveSearchPopupSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 4/28/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

final class SaveSearchPopupSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана с подтверждением сохранения поиска") { _ -> Self in
            self.viewController.yreEnsureExistsWithTimeout()
            return self
        }
    }

    func finalScreenIsPresented() -> Self {
        self.finalVC.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func fillForm(title: String) -> Self {
        self.titleTextView
            .yreEnsureExistsWithTimeout()
            .yreTap()
            .yreTypeText(title)
        return self
    }

    @discardableResult
    func tapSubmitButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Подписаться") { _ -> Self in
            self.saveButton
                .yreEnsureExists()
                .yreTap()
            return self
        }
    }

    @discardableResult
    func tapSubscribeButton() -> Self {
        self.subscribeButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func tapCloseButton() -> Self {
        self.closeButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func closeKeyboard() -> Self {
        self.viewController.yreTap()
        return self
    }

    @discardableResult
    func tapPostponeButtonIfExists() -> Self {
        XCTContext.runActivity(
            named: #"Нажимаем на кнопку "Позже" на экране запроса разрешения на уведомления, если он был показан"#
        ) { _ -> Self in
            guard self.postponeButton.yreWaitForExistence() else { return self }
            self.postponeButton.yreTap()
            return self
        }
    }

    @discardableResult
    func tapDoneButtonIfExists() -> Self {
        XCTContext.runActivity(
            named: #"Нажимаем на кнопку "Позже" на экране запроса разрешения на уведомления, если он был показан"#
        ) { _ -> Self in
            guard self.doneButton.yreWaitForExistence() else { return self }
            self.doneButton.yreTap()
            return self
        }
    }

    @discardableResult
    func ensureTitleViewHasText(_ text: String? = nil) -> Self {
        XCTAssertNotNil(self.titleTextView.value as? String)
        XCTAssertNotEqual(self.titleTextView.value as? String, "")
        if let text = text {
            XCTAssertEqual(text, self.titleTextView.value as? String)
        }
        return self
    }

    @discardableResult
    func ensureGeoIntentShown(with text: String) -> Self {
        let paramsList = ElementsProvider.obtainElement(identifier: Self.paramsListID, in: viewController)
        let geoIntentRow = paramsList.staticTexts["Область поиска: " + text]
        paramsList.yreEnsureExists()
        geoIntentRow.yreEnsureExists()
        return self
    }

    @discardableResult
    func ensureCompositeGeoIntentShown(with text: String) -> Self {
        let paramsList = ElementsProvider.obtainElement(identifier: Self.paramsListID, in: viewController)
        let geoIntentRow = paramsList.staticTexts["Области поиска: " + text]
        paramsList.yreEnsureExists()
        geoIntentRow.yreEnsureExists()
        return self
    }

    @discardableResult
    func tapOnShowDrawnAreas() -> Self {
        let paramsList = ElementsProvider.obtainElement(identifier: Self.paramsListID, in: viewController)
        let showDrawnAreaButton = ElementsProvider.obtainElement(identifier: "Посмотреть нарисованные области", in: paramsList)
        showDrawnAreaButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnShowSearchArea() -> Self {
        let paramsList = ElementsProvider.obtainElement(identifier: Self.paramsListID, in: viewController)
        let showDrawnAreaButton = ElementsProvider.obtainElement(identifier: "Посмотреть область поиска", in: paramsList)
        showDrawnAreaButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        let screenshot = self.viewController.yreWaitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        return self
    }

    // MARK: Private

    private lazy var viewController = ElementsProvider.obtainElement(identifier: "savedSearch.params")
    private lazy var finalVC = ElementsProvider.obtainElement(identifier: "savedSearch.params.final")
    private lazy var navigationContainer = ElementsProvider.obtainNavigationContainer()
    private lazy var titleTextView = ElementsProvider.obtainElement(
        identifier: "savedSearch.params.textview.name",
        type: .textView,
        in: self.viewController
    )

    private lazy var saveButton = ElementsProvider.obtainElement(
        identifier: "savedSearch.params.button.save",
        type: .button,
        in: self.viewController
    )

    private lazy var closeButton: XCUIElement = ElementsProvider.obtainBackButton()

    private lazy var subscribeButton = ElementsProvider.obtainElement(
        identifier: "savedSearch.params.button.save",
        type: .button,
        in: self.navigationContainer
    )

    private lazy var postponeButton = ElementsProvider.obtainElement(
        identifier: "savedSearch.params.final.button.postpone",
        type: .button,
        in: self.finalVC
    )

    private lazy var doneButton = ElementsProvider.obtainElement(
        identifier: "savedSearch.params.final.button.done",
        type: .button,
        in: self.finalVC
    )

    private static let paramsListID = "savedSearch.params.list"
}
