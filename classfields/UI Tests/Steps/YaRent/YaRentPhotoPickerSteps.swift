//
//  YaRentPhotoPickerSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 18.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.YaRentPhotoPickerAccessibilityIdentifiers

final class YaRentPhotoPickerSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана пикера фотографий") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureScreenDismissed() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана пикера фотографий") { _ -> Void in
            self.screenView.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func ensureContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие контента на экране") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAddPhoto() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Добавить фото'") { _ -> Void in
            self.addPhotoButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnRetry(at index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на повторную загрузку фотографии `\(index + 1)`") { _ -> Void in
            let identifier = Identifiers.ImageUploader.itemCell(for: index)
            let photoElement = ElementsProvider.obtainElement(identifier: identifier)
            let retryButton = ElementsProvider.obtainElement(identifier: Identifiers.ImageUploader.Action.retry,
                                                             in: photoElement)
            retryButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnRemove(at index: Int) -> Self {
        XCTContext.runActivity(named: "Удаляем фотографию `\(index + 1)`") { _ -> Void in
            let identifier = Identifiers.ImageUploader.itemCell(for: index)
            let photoElement = ElementsProvider.obtainElement(identifier: identifier)
            let removeButton = ElementsProvider.obtainElement(identifier: Identifiers.ImageUploader.Action.delete,
                                                              in: photoElement)
            removeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSubmitButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Отправить'") { _ -> Void in
            self.submitButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapCloseButton() -> Self {
        XCTContext.runActivity(named: "Закрываем пикер фотографий") { _ -> Void in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()
            navigationContainer.yreEnsureExistsWithTimeout()
            
            let closeButton = ElementsProvider.obtainBackButton(in: navigationContainer)
            closeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isCloseConfirmationNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что диалог `Вы уверены, что хотите выйти?` не показывается") { _ -> Void in
            YaRentPhotoPickerDialogSteps()
                .screenIsDismissed()
        }
        return self
    }

    @discardableResult
    func confirmCloseConfirmation() -> Self {
        XCTContext.runActivity(named: "Нажимаем `Да` в диалоге `Вы уверены, что хотите выйти?`") { _ -> Void in
            YaRentPhotoPickerDialogSteps()
                .screenIsPresented()
                .pressYesButton()
                .screenIsDismissed()
        }
        return self
    }

    @discardableResult
    func declineCloseConfirmation() -> Self {
        XCTContext.runActivity(named: "Нажимаем `Нет` в диалоге `Вы уверены, что хотите выйти?`") { _ -> Void in
            YaRentPhotoPickerDialogSteps()
                .screenIsPresented()
                .pressNoButton()
                .screenIsDismissed()
        }
        return self
    }

    private typealias Identifiers = YaRentPhotoPickerAccessibilityIdentifiers

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView)
    private lazy var addPhotoButton = ElementsProvider.obtainElement(identifier: Identifiers.ImageUploader.addPhotoButton)
    private lazy var submitButton = ElementsProvider.obtainButton(
        identifier: Identifiers.submitButton,
        in: self.screenView
    )
}
