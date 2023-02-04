//
//  AnyOfferCardSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.OfferCardAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.CardGalleryAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.AnyOfferGalleryAccessibilityIdentifiers

class AnyOfferCardSteps {
    @discardableResult
    func isCallButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие кнопки \"Позвонить\"") { _ -> Void in
            self.callButton.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isCallButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Позвонить\"") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapOnGallery() -> AnyOfferGallerySteps {
        XCTContext.runActivity(named: "Нажимаем на галерею") { _ -> Void in
            self.galleryView
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return AnyOfferGallerySteps()
    }

    @discardableResult
    func tapOnCallButtonFromGallery() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Позвонить\" в галерее") { _ -> Void in
            self.callButtonFromGallery
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButtonFromGallery() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Закрыть\" в галерее") { _ -> Void in
            self.closeButtonFromGallery
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    // MARK: Private

    private var galleryView: XCUIElement {
        ElementsProvider.obtainElement(identifier: CardGalleryAccessibilityIdentifiers.view)
    }
    var callButton: XCUIElement {
        ElementsProvider.obtainElement(identifier: "offer.any.card.callButton")
    }
    private var callButtonFromGallery: XCUIElement {
        ElementsProvider.obtainElement(identifier: AnyOfferGalleryAccessibilityIdentifiers.callButton)
    }
    private var closeButtonFromGallery: XCUIElement {
        ElementsProvider.obtainElement(identifier: AnyOfferGalleryAccessibilityIdentifiers.closeButton)
    }
    private var topNotificationView: XCUIElement {
        ElementsProvider.obtainElement(identifier: "top.notification.view")
    }
}

extension AnyOfferCardSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Позвонить\"") { _ -> Void in
            self.callButton.yreTap()
        }
        return self
    }
}
