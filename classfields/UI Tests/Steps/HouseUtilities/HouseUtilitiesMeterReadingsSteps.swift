//
//  HouseUtilitiesMeterReadingsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 11.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.HouseUtilitiesMetersAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesMeterReadingsSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана показаний счётчика") { _ -> Void in
            self.containerView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureDetailsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что открыт экран просмотра показаний") { _ -> Void in
            self.detailsView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensurePickerPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что открыт экран отправки показаний") { _ -> Void in
            self.pickerView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func typeValue(text: String, at index: Int) -> Self {
        XCTContext.runActivity(named: "Вводим показания счётчика по индексу \(index)") { _ -> Void in
            let valueFieldID = Identifiers.valueField(with: index)
            ElementsProvider
                .obtainElement(identifier: valueFieldID)
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText(text)
        }

        return self
    }

    @discardableResult
    func tapAddPhoto(at index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Добавить фото' по индексу \(index)") { _ -> Void in
            let cellID = Identifiers.addPhotoCell(with: index)
            ElementsProvider
                .obtainElement(identifier: cellID)
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapEditReadingsButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Изменить'") { _ -> Void in
            self.editReadingsButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSendReadingsButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Отправить'") { _ -> Void in
            self.sendReadingsButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapDeclineReadingsButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Отклонить'") { _ -> Void in
            self.declineReadingsButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesMetersAccessibilityIdentifiers.MeterReadings

    private lazy var containerView = ElementsProvider.obtainElement(identifier: Identifiers.containerView)
    private lazy var pickerView = ElementsProvider.obtainElement(
        identifier: Identifiers.pickerView,
        in: self.containerView
    )
    private lazy var detailsView = ElementsProvider.obtainElement(
        identifier: Identifiers.detailsView,
        in: self.containerView
    )

    private lazy var editReadingsButton = ElementsProvider.obtainButton(
        identifier: Identifiers.editReadingsButton,
        in: self.detailsView
    )
    private lazy var sendReadingsButton = ElementsProvider.obtainElement(
        identifier: Identifiers.sendReadingsButton,
        in: self.pickerView
    )

    private lazy var declineReadingsButton = ElementsProvider.obtainButton(
        identifier: Identifiers.declineReadingsButton,
        in: self.detailsView
    )
}
