//
//  SearchExtendingSteps.swift
//  UI Tests
//
//  Created by Timur Guliamov on 08.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class SearchExtendingSteps {
    @discardableResult
    func makeScreenshot(suffix: String = "") -> Self {
        self.toast.yreWaitAndCompareScreenshot(identifier: "searchExtendingToast" + suffix)
        return self
    }
    
    @discardableResult
    func makeBottomSheetScreenshot(suffix: String = "") -> Self {
        self.bottomSheet.yreWaitAndCompareScreenshot(identifier: "bottomSheet" + suffix)
        return self
    }
    
    @discardableResult
    func makeBottomSheetApplyButtonScreenshot(suffix: String = "") -> Self {
        let button = ElementsProvider.obtainElement(
            identifier: SearchExtendingAccessibilityIdentifiers.bottomSheetApplyButton,
            in: self.bottomSheet
        )
        
        button.yreWaitAndCompareScreenshot(identifier: "bottomSheetApplyButton" + suffix)
        
        return self
    }
    

    @discardableResult
    func isToastPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие плашки расширения фильтров") { _ -> Void in
            self.toast.yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func isToastNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие плашки расширения фильтров") { _ -> Void in
            self.toast.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isBottomSheetPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие Боттом шита с расширениями фильтров") { _ -> Void in
            self.bottomSheet.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
     func waitForTopNotificationViewExistence() -> Self {
         XCTContext.runActivity(named: "Ожидаем появление верхней инфо-плашки") { _ -> Void in
             _ = self.topNotificationView.waitForExistence(timeout: Constants.timeout)
         }
         return self
     }
    
    @discardableResult
    func tapOnToastButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Изменить\" на плашке расширения фильтров") { _ in
            let button = ElementsProvider.obtainButton(
                identifier: SearchExtendingAccessibilityIdentifiers.toastButton,
                in: self.toast
            )
            
            button
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }
    
    @discardableResult
    func tapOnBottomSheetApplyButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Показать\" на боттомшите расширения фильтров") { _ in
            let button = ElementsProvider.obtainButton(
                identifier: SearchExtendingAccessibilityIdentifiers.bottomSheetApplyButton,
                in: self.bottomSheet
            )
            
            button
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }
    
    @discardableResult
    func tapOnBottomSheetCellSwitch(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на свитч у плашки с индексом \(index)") { _ in
            let cell = ElementsProvider.obtainElement(
                identifier: SearchExtendingAccessibilityIdentifiers.bottomSheetCell(index: index),
                type: .cell,
                in: self.bottomSheet
            )
            let cellSwitch = ElementsProvider.obtainElement(
                identifier: SearchExtendingAccessibilityIdentifiers.bottomSheetCellSwitch,
                type: .any,
                in: cell
            )
            
            cellSwitch.yreTap()
        }
        return self
    }
    
    @discardableResult
    func swipeToast(
        direction: XCUIElement.Direction,
        velocity: XCUIGestureVelocity = .default
    ) -> Self {
        XCTContext.runActivity(
            named: "Свайпаем тост \(self.stringForDirection(direction)). Скорость: \(self.stringForVelocity(velocity))"
        ) { _ in
            switch direction {
                case .left:
                    self.toast.swipeLeft(velocity: velocity)
                case .right:
                    self.toast.swipeRight(velocity: velocity)
                case .up:
                    self.toast.swipeUp(velocity: velocity)
                case .down:
                    self.toast.swipeDown(velocity: velocity)
            }
        }
        return self
    }
    
    @discardableResult
    func isBottomSheetCellsCountEqual(count: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем, что количество фильтров для расширения равно \(count)") { _ in
            let cell = ElementsProvider.obtainElement(
                identifier: SearchExtendingAccessibilityIdentifiers.bottomSheetCell(index: count - 1),
                type: .cell,
                in: self.bottomSheet
            )
            let nextCell = ElementsProvider.obtainElement(
                identifier: SearchExtendingAccessibilityIdentifiers.bottomSheetCell(index: count),
                type: .cell,
                in: self.bottomSheet
            )

            cell.yreEnsureExists()
            nextCell.yreEnsureNotExists()
        }
        
        return self
    }
    
    // MARK: - Private
    
    private lazy var toast: XCUIElement = ElementsProvider.obtainElement(
        identifier: SearchExtendingAccessibilityIdentifiers.toast
    )
    private lazy var bottomSheet: XCUIElement = ElementsProvider.obtainElement(
        identifier: SearchExtendingAccessibilityIdentifiers.bottomSheet
    )
    private lazy var topNotificationView = ElementsProvider.obtainElement(
        identifier: "top.notification.view"
    )
    
    private func stringForDirection(_ direction: XCUIElement.Direction) -> String {
        switch direction {
            case .down: return "вниз"
            case .left: return "влево"
            case .right: return "вправо"
            case .up: return "вверх"
        }
    }
    
    private func stringForVelocity(_ velocity: XCUIGestureVelocity) -> String {
        switch velocity {
            case .default: return "средняя"
            case .fast: return "быстрая"
            case .slow: return "медленная"
            default: return ""
        }
    }
}
