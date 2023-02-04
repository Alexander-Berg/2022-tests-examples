//
//  CallHistorySteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 3/11/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class CallHistorySteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана Истории звонков") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    func withOfferList() -> OfferListSteps {
        return OfferListSteps(screen: self.viewController)
    }

    func withSiteList() -> SiteListSteps {
        return SiteListSteps(screen: self.viewController)
    }

    func withVillageList() -> VillageListSteps {
        return VillageListSteps(screen: self.viewController)
    }

    private lazy var viewController = ElementsProvider.obtainElement(identifier: CallHistoryAccessibilityIdentifiers.view)

    private func listStepsProvider(cellID: String) -> AnyOfferListStepsProvider {
        return AnyOfferListStepsProvider(
            container: self.viewController,
            cellID: cellID
        )
    }

    private enum Identifiers {
        static let offerCell = OfferSnippetCellAccessibilityIdentifiers.cell
        static let siteCell = SiteSnippetCellAccessibilityIdentifiers.cellIdentifier
        static let villageCell = VillageSnippetCellAccessibilityIdentifiers.cellIdentifier
    }
}
