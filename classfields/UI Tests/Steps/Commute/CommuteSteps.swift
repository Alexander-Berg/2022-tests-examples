//
//  CommuteSteps.swift
//  UITests
//
//  Created by Leontyev Saveliy on 12/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class CommuteSteps {
    @discardableResult
    func isMapPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что карта отобржается") { _ -> Void in
            self.mapVC.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isCommutePanelVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что плашка \"Время на дорогу\" отображается") { _ -> Void in
            self.commutePanel
                .yreEnsureVisibleWithTimeout()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isCommutePanelInSelectionAddressState() -> Self {
        XCTContext.runActivity(named: "Проверяем, что текущее состояние - Выбор адреса") { _ -> Void in
            self.addressField.yreEnsureExistsWithTimeout()
            self.submitButton.yreEnsureExistsWithTimeout()
            self.cancelButton.yreEnsureExistsWithTimeout()

            self.transportTypeControl.yreEnsureNotExists()
            self.timeControl.yreEnsureNotExists()
            self.retryButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isCommutePanelInCommuteConfigurationState() -> Self {
        XCTContext.runActivity(named: "Проверяем, что текущее состояние - Настройка") { _ -> Void in
            self.addressField.yreEnsureExistsWithTimeout()
            self.transportTypeControl.yreEnsureExistsWithTimeout()
            self.timeControl.yreEnsureExistsWithTimeout()
            self.submitButton.yreEnsureExistsWithTimeout()
            self.cancelButton.yreEnsureExistsWithTimeout()

            self.retryButton.yreEnsureNotExists()
        }
        return self
    }

    /// This method checks the equality of the text in the address field with the value of `text`
    /// and it will retry checking the text the number of `attempts` times
    /// If there are still `attempts`, then after an unsuccessful check it will remain on the `delay` timeout
    ///
    /// In some cases `isAddressFieldContains(text:)` may throw assertion with message:
    /// XCTAssertEqual failed: ("Optional("Ищем адрес...")") is not equal to ("Optional(`text`)")
    @discardableResult
    func isAddressFieldContains(
        text: String,
        withRetryAttempts attempts: Int,
        withDelay delay: TimeInterval = 1.0
    ) -> Self {
        XCTContext.runActivity(named: "Проверяем, что в поле адреса введен текст - \"\(text)\"") { _ in
            let value: String = (self.addressField.value as? String) ?? ""
            let contains = (value == text)

            if !contains && attempts > 0 {
                XCTContext.runActivity(
                    named: "Текущий текст – \"\(value)\", осталось попыток: \(attempts)",
                    block: { _ -> Void in sleep(UInt32(delay)) }
                )
                return self.isAddressFieldContains(text: text, withRetryAttempts: attempts - 1)
            }

            XCTAssertTrue(contains)
            return self
        }
    }

    @discardableResult
    func isAddressFieldContains(text: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что в поле адреса введен текст - \"\(text)\"") { _ -> Void in
            let value = self.addressField.value as? String
            XCTAssertEqual(value, text)
        }
        return self
    }

    @discardableResult
    func pressSubmitButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку подтверждения") { _ -> Void in
            self.submitButton
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return self
    }

    @discardableResult
    func pressAddressField() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле ввода адреса") { _ -> Void in
            self.addressField
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return self
    }

    @discardableResult
    func pressBackButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Назад\"") { _ -> Void in
            self.backButton
                .yreEnsureExists()
                .tap()
        }
        return self
    }

    @discardableResult
    func selectTransportType(index: Int) -> Self {
        XCTContext.runActivity(named: "Выбираем тип транспорта – ячейка \(index)") { _ -> Void in
            let segment = ElementsProvider.obtainElement(identifier: "FluidSegment_\(index)",
                                                         in: self.transportTypeControl)
            segment
                .yreEnsureExists(message: "Segment with index \(index) doesn't exist")
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func selectTime(index: Int) -> Self {
        XCTContext.runActivity(named: "Выбираем время - ячейка \(index)") { _ -> Void in
            let segment = ElementsProvider.obtainElement(identifier: "FluidSegment_\(index)",
                                                         in: self.timeControl)
            segment
                .yreEnsureExists(message: "Segment with index \(index) doesn't exist")
                .yreForceTap()
        }
        return self
    }

    private lazy var containerVC = ElementsProvider.obtainElement(identifier: "CommuteContainerVC")
    private lazy var mapVC = ElementsProvider.obtainElement(
        identifier: "CommuteMapVC",
        in: self.containerVC
    )
    private lazy var commutePanel = ElementsProvider.obtainElement(
        identifier: "CommutePanelVC",
        in: self.containerVC
    )
    private lazy var addressField = ElementsProvider.obtainElement(
        identifier: "commutePanelView.addressField",
        in: self.commutePanel
    )
    private lazy var transportTypeControl = ElementsProvider.obtainElement(
        identifier: "commutePanelView.transportTypeControl",
        in: self.commutePanel
    )
    private lazy var timeControl = ElementsProvider.obtainElement(
        identifier: "commutePanelView.timeControl",
        in: self.commutePanel
    )
    private lazy var submitButton = ElementsProvider.obtainElement(
        identifier: "commutePanelView.submit",
        in: self.commutePanel
    )
    private lazy var retryButton = ElementsProvider.obtainElement(
        identifier: "commutePanelView.retry",
        in: self.commutePanel
    )
    private lazy var cancelButton = ElementsProvider.obtainElement(
        identifier: "commutePanelView.cancel",
        in: self.commutePanel
    )
    private lazy var backButton = ElementsProvider.obtainElement(
        identifier: "CommuteContainerVC.backButton",
        in: self.containerVC
    )
}
