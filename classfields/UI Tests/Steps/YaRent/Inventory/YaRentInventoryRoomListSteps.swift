//
//  YaRentInventoryRoomListSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 06.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryRoomListSteps {
    @discardableResult
    func ensureListContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие списка комнат на экране") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAddRoomButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'Добавить комнату'") { _ -> Void in
            self.addRoomButton
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapEditRoomButton(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'Редактировать комнату'") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.editRoomButton(index: index))
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func tapAddObjectButton(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'Добавить объект'") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.addObjectButton(index: index))
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func tapObject(section: Int, index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на объект в комнате \(section) по индексу \(index)") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.objectCell(section: section, index: index))
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func tapContinueButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'Продолжить'") { _ -> Void in
            self.continueButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func tapManagerComment() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'комментарий менеджера'") { _ -> Void in
            self.managerComment
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.RoomsList

    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView)
    private lazy var addRoomButton = ElementsProvider.obtainButton(
        identifier: Identifiers.addRoomButton,
        in: self.contentView
    )
    private lazy var continueButton = ElementsProvider.obtainElement(identifier: Identifiers.continueButton)
    private lazy var managerComment = ElementsProvider.obtainElement(identifier: Identifiers.managerComment)
}
