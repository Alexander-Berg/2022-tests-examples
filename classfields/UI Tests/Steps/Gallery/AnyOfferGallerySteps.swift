//
//  AnyOfferGallerySteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 31.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class AnyOfferGallerySteps {
    @discardableResult
    func swipeLeft(times: UInt) -> Self {
        XCTContext.runActivity(named: "Скроллим галерею влево") { _ -> Void in
            for _ in 0..<times {
                self.galleryView.swipe(direction: .left)
            }
        }
        return self
    }

    @discardableResult
    func swipeRight(times: UInt) -> Self {
        XCTContext.runActivity(named: "Скроллим галерею вправо") { _ -> Void in
            for _ in 0..<times {
                self.galleryView.swipe(direction: .right)
            }
        }
        return self
    }

    @discardableResult
    func tap() -> Self {
        XCTContext.runActivity(named: "Тап по элементу коллекции") { _ -> Void in
            self.galleryView.yreTap()
        }
        return self
    }

    @discardableResult
    func isVirtualTourVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с 3D туром показана") { _ -> Void in
            self.virtualTour.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isVirtualTourNotVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с 3D туром скрыта") { _ -> Void in
            self.virtualTour.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isYoutubeVideoVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с Youtube показана") { _ -> Void in
            self.youtubeVideo.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isYoutubeVideoNotVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с Youtube скрыта") { _ -> Void in
            self.youtubeVideo.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isAnchorButtonPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка 'Планировка' показана") { _ -> Void in
            self.anchorButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isAnchorButtonNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка 'Планировка' скрыта") { _ -> Void in
            self.anchorButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isCallButtonPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка 'Позвонить' показана") { _ -> Void in
            self.callButton.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isAnchorButtonVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка-якорь видна") { _ -> Void in
            self.anchorButton.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isAnchorButtonNotVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка-якорь скрыта") { _ -> Void in
            self.anchorButton.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAnchorButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку-якорь") { _ -> Void in
            self.anchorButton.yreTap()
        }
        return self
    }

    @discardableResult
    func checkAnchorTitle(_ title: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что текст на кнопке-якоре равен \(title)") { _ in
            XCTAssertEqual(self.anchorButton.label, title)
        }
        return self
    }

    @discardableResult
    func isPhotoVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с фотографией показана") { _ -> Void in
            self.photo.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isFlatPlanVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с планировкой показана") { _ -> Void in
            self.flatPlan.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isFloorPlanVisible() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с планом этажа показана") { _ -> Void in
            self.floorPlan.yreEnsureVisibleWithTimeout()
        }
        return self
    }


    // MARK: - Private

    private var photo: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.photo, in: self.carousel)
    }

    private var carousel: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.carousel)
    }
    
    private var virtualTour: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.virtualTour, in: self.carousel)
    }

    private var youtubeVideo: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.youtubeVideo, in: self.carousel)
    }

    private var flatPlan: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.flatPlan, in: self.carousel)
    }

    private var floorPlan: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.floorPlan, in: self.carousel)
    }

    private var anchorButton: XCUIElement {
        ElementsProvider.obtainButton(identifier: AccessibilityIdentifiers.anchorButton)
    }

    private var callButton: XCUIElement {
        ElementsProvider.obtainButton(identifier: AccessibilityIdentifiers.callButton)
    }

    private lazy var galleryView = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.view
    )

    private typealias AccessibilityIdentifiers = AnyOfferGalleryAccessibilityIdentifiers
}

extension AnyOfferGallerySteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Позвонить'") { _ -> Void in
            self.callButton.yreTap()
        }
        return self
    }
}
