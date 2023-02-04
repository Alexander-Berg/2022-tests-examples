//
//  SnippetGallerySteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 31.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class SnippetGallerySteps {
    init(element: XCUIElement) {
        self.element = element
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что галерея показана") { _ -> Void in
            self.carouselView.yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func swipeLeft(times: UInt) -> Self {
        XCTContext.runActivity(named: "Скроллим галерею влево") { _ -> Void in
            for _ in 0..<times {
                self.carouselView.swipe(direction: .left)
            }
        }
        return self
    }

    @discardableResult
    func swipeRight(times: UInt) -> Self {
        XCTContext.runActivity(named: "Скроллим галерею вправо") { _ -> Void in
            for _ in 0..<times {
                self.carouselView.swipe(direction: .right)
            }
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
    func tap() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ячейку") { _ -> Void in
            self.carouselView.yreTap()
        }
        return self
    }

    // MARK: - Private

    private let element: XCUIElement

    private var virtualTour: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.virtualTour)
    }

    private var youtubeVideo: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.youtubeVideo)
    }

    private var carouselView: XCUIElement {
        return self.element
    }

    private typealias AccessibilityIdentifiers = SnippetGalleryAccessibilityIdentifiers
}
