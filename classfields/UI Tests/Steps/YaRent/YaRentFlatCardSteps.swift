//
//  YaRentFlatCardSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 29.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class YaRentFlatCardSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана квартиры") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenDismissed() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран квартиры закрыт") { _ -> Void in
            self.screen.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isContentLoaded() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие контента на экране квартиры") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureFlatStatus(_ status: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что состояние квартиры - \(status)") { _ -> Void in
            let element = ElementsProvider.obtainElement(
                identifier: Identifiers.Flat.statusLabel,
                type: .staticText,
                in: self.flatView
            )

            XCTAssertEqual(element.label, status)
        }
        return self
    }

    @discardableResult
    func scroll(swipe direction: XCUIElement.Direction) -> Self {
        XCTContext.runActivity(named: "Скроллим список нотификаций (Свайп - \(direction))") { _ -> Void in
            self.notificationsCollection
                .yreEnsureExistsWithTimeout()
                .swipe(direction: direction)

            yreSleep(1, message: "Ожидаем окончания свайпа")
        }
        return self
    }

    @discardableResult
    func ensureNavigationTitle(isVisible: Bool) -> Self {
        XCTContext.runActivity(named: "Проверяем, что информация о квартире \(isVisible ? "" : "не ")отображается в навбаре") { _ -> Void in
            let element = self.navbarTitleView

            if isVisible {
                element.yreEnsureVisibleWithTimeout()
            }
            else {
                element.yreEnsureNotVisible()
            }
        }
        return self
    }

    @discardableResult
    func ensureFlatHeader(isVisible: Bool) -> Self {
        XCTContext.runActivity(named: "Проверяем, что информация о квартире \(isVisible ? "" : "не ")отображается внутри списка") { _ -> Void in
            let element = self.flatView

            if isVisible {
                element.yreEnsureExistsWithTimeout()
            }
            else {
                element.yreEnsureNotVisible()
            }
        }
        return self
    }

    @discardableResult
    func ensureInsuranceBadgeExists() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие бэйджа 'застрахована'") { _ -> Void in
            self.insuranceBadge
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapInsuranceBadge() -> Self {
        XCTContext.runActivity(named: "Нажимаем на бэйдж 'застрахована'") { _ -> Void in
            self.insuranceBadge
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSuccessPopupActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку действия на попапе") { _ -> Void in
            self.successPopupActionButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapReceiptsDeclinedPopupActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку действия на попапе квитанции отклонены") { _ -> Void in
            self.receiptsDeclinedPopupButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapBillSentPopupActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку действия на попапе счёт отправлен") { _ -> Void in
            self.billSentPopupButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func ensureInsurancePopupShowing() -> Self {
        XCTContext.runActivity(named: "Проверяем, что попап о страховке показан") { _ -> Void in
            self.insurancePopup
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapInsurancePopupActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку действия на попапе о страховке") { _ -> Void in
            self.insurancePopupButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func closePopup() -> Self {
        XCTContext.runActivity(named: "Закрываем попап") { _ -> Void in
            ElementsProvider
                .obtainBackButton()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = YaRentFlatCardAccessibilityIdentifiers

    private lazy var screen = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView, in: self.screen)
    private lazy var errorView = ElementsProvider.obtainElement(identifier: Identifiers.errorView, in: self.screen)
    private lazy var loadingView = ElementsProvider.obtainElement(identifier: Identifiers.loadingView, in: self.screen)

    private lazy var navbarTitleView = ElementsProvider.obtainElement(identifier: Identifiers.navigationTitle)
    private lazy var flatView = ElementsProvider.obtainElement(identifier: Identifiers.Flat.view, in: self.contentView)

    private lazy var notificationsCollection = ElementsProvider.obtainElement(
        identifier: Identifiers.notificationsCollection,
        in: self.contentView
    )

    private lazy var successPopup = ElementsProvider.obtainElement(identifier: Identifiers.Popup.Success.view)
    private lazy var successPopupActionButton = ElementsProvider.obtainButton(
        identifier: Identifiers.Popup.Success.actionButton,
        in: self.successPopup
    )

    private lazy var receiptsDeclinedPopup = ElementsProvider.obtainElement(identifier: Identifiers.Popup.ReceiptsDeclined.view)
    private lazy var receiptsDeclinedPopupButton = ElementsProvider.obtainButton(
        identifier: Identifiers.Popup.ReceiptsDeclined.actionButton,
        in: self.receiptsDeclinedPopup
    )

    private lazy var billSentPopup = ElementsProvider.obtainElement(identifier: Identifiers.Popup.BillSent.view)
    private lazy var billSentPopupButton = ElementsProvider.obtainButton(
        identifier: Identifiers.Popup.BillSent.actionButton,
        in: self.billSentPopup
    )

    private lazy var insuranceBadge = ElementsProvider.obtainElement(identifier: Identifiers.insuranceBadge)
    private lazy var insurancePopup = ElementsProvider.obtainElement(identifier: Identifiers.Popup.Insurance.view)
    private lazy var insurancePopupButton = ElementsProvider.obtainButton(
        identifier: Identifiers.Popup.Insurance.actionButton,
        in: self.insurancePopup
    )
}
