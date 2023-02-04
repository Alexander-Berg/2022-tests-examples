//
//  YaRentInventoryPreviewListSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryPreviewListSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана превью описи") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureManagerCommentHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие комментария менеджера") { _ -> Void in
            self.managerComment
                .yreEnsureNotExistsWithTimeout()
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

    @discardableResult
    func tapObject(section: Int, index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на объект в комнате \(section) по индексу \(index)") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: RoomsID.objectCell(section: section, index: index))
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func tapDefect(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на дефект по индексу \(index)") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: DefectID.defectCell(index: index))
                .yreEnsureExistsWithTimeout()
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

    private typealias RoomsID = YaRentInventoryAccessibilityIdentifiers.RoomsList
    private typealias DefectID = YaRentInventoryAccessibilityIdentifiers.DefectList
    private typealias PreviewID = YaRentInventoryAccessibilityIdentifiers.PreviewList

    private lazy var screenView = ElementsProvider.obtainElement(identifier: PreviewID.view)
    private lazy var managerComment = ElementsProvider.obtainElement(identifier: RoomsID.managerComment)
    private lazy var continueButton = ElementsProvider.obtainElement(identifier: PreviewID.continueButton)
}
