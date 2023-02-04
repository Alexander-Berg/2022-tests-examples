//
//  SavedSearchMapSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 5/28/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YRETestsUtils

final class SavedSearchMapSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        self.viewController.yreEnsureExistsWithTimeout()
        return self
    }
    
    @discardableResult
    func tapOnCloseButton() -> Self {
        let navigationContainer = ElementsProvider.obtainNavigationContainer()
        let closeButton = ElementsProvider.obtainBackButton(in: navigationContainer)
        closeButton
            .yreEnsureExistsWithTimeout()
            .yreTap()
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        let screenshot = self.viewController.yreWaitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier, overallTolerance: Constants.overallToleranceForMap)
        return self
    }
    
    private lazy var viewController = ElementsProvider.obtainElement(identifier: "savedSearch.params.map")
}
