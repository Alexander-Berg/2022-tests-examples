//
//  ManualScreenTests.swift
//  UI Tests
//
//  Created by Anfisa Klisho on 22.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREAppConfig

final class ManualScreenTests: BaseTestCase {
    func testShareButton() {
        let config = ExternalAppConfiguration.commonUITests
        config.selectedTabItem = .home

        self.relaunchApp(with: config)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnManualCell()

        WebPageSteps()
            .screenIsPresented()
            .tapOnShareButton()
            .isActivityVCPresented()
    }
}
