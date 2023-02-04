//
//  ImagePickerSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 12.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import class YREAccessibilityIdentifiers.ImagePickerAccessibilityIdentifiers
import XCTest

final class ImagePickerSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем открытие пикера фото") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func selectPhoto(at index: Int) -> Self {
        XCTContext.runActivity(named: "Выбираем фото по индексу \(index)") { _ -> Void in
            let predicate = NSPredicate(format: "label CONTAINS[c] 'Фотография'")
            self.app.images
                .matching(predicate)
                .element(boundBy: index)
                .yreTap()
        }
        return self
    }

    @discardableResult
    func submitPhotos() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Выбрать фотографии'") { _ -> Void in
            let button = self.app.buttons["Add"]
            button.tap()
        }
        return self
    }

    private typealias Identifiers = ImagePickerAccessibilityIdentifiers

    private lazy var app = XCUIApplication()
    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var submitButton = ElementsProvider.obtainElement(
        identifier: Identifiers.submitButton,
        in: self.view
    )
}
