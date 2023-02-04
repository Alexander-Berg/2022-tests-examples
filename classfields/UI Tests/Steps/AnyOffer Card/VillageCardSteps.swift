//
//  VillageCardSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class VillageCardSteps: AnyOfferCardSteps {
    lazy var screen: XCUIElement = ElementsProvider.obtainElement(identifier: "Village Card View")

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана с карточкой КП") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isVillageNameLabelWithText(_ text: String) -> Self {
        let villageNameLabel = self.screen.staticTexts[self.villageNameLabelID]
        villageNameLabel.yreEnsureExistsWithTimeout()

        XCTAssertEqual(villageNameLabel.label, text)
        return self
    }

    @discardableResult
    func isCompletionInfoLabelWithText(_ text: String) -> Self {
        let completionInfoLabel = self.screen.staticTexts[self.completionInfoLabelID]
        completionInfoLabel.yreEnsureExistsWithTimeout()
        
        XCTAssertEqual(completionInfoLabel.label, text)
        return self
    }

    func tapStatisticsCell() -> VillageOfferListSteps {
        XCTContext.runActivity(named: "Нажимаем на ячейку со статистикой в КП") { _ -> Void in
            self.scrollToStatisticsCell()

            self.statisticsCell
                .yreTap()
        }
        return VillageOfferListSteps()
    }

    @discardableResult
    func scrollToStatisticsCell() -> Self {
        XCTContext.runActivity(named: "Скроллим к элементу со статистикой домов/участков") { _ -> Void in
            self.screen.scrollToElement(element: self.statisticsCell, direction: .up)
        }
        return self
    }

    @discardableResult
    func scrollToDeveloperVillages() -> Self {
        XCTContext.runActivity(named: "Скроллим к ячейке КП") { _ -> Void in
            self.screen.scrollToElement(element: self.developerVillagesBlock, direction: .up, swipeLimits: 30)
        }
        return self
    }

    func villageSnippet() -> VillageSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент с ячейкой КП") { _ -> VillageSnippetSteps in
            let snippetListNode = ElementsProvider.obtainElement(
                identifier: Identifiers.developerVillagesListNode,
                in: self.developerVillagesBlock
            )
            snippetListNode
                .yreEnsureExistsWithTimeout()

            let cell = ElementsProvider.obtainElement(
                identifier: VillageSnippetCellAccessibilityIdentifiers.viewIdentifier,
                in: snippetListNode
            )
            return VillageSnippetSteps(element: cell)
        }
    }

    // MARK: Private

    private typealias Identifiers = VillageCardAccessibilityIdentifiers

    private let villageNameLabelID = Identifiers.villageNameLabelID
    private let completionInfoLabelID = Identifiers.completionInfoLabelID
    private let statisticsCellID = Identifiers.statisticsCell

    private var statisticsCell: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: self.statisticsCellID,
            type: .any,
            in: self.screen
        )
    }

    private var developerVillagesBlock: XCUIElement {
        ElementsProvider.obtainElement(
            identifier: Identifiers.developerVillages,
            in: self.screen
        )
    }
}
