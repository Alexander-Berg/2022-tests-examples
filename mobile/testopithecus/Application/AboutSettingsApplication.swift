//
// Created by Elizaveta Y. Voronina on 2/17/20.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class AboutSettingsApplication: AboutSettings {
    private let rootSettingsPage = RootSettingsPage()
    private let aboutSettingsPage = AboutPage()

    public func openAboutSettings() throws {
        try XCTContext.runActivity(named: "Opening About settings") { _ in
            try self.rootSettingsPage.aboutButton.tapCarefully()
        }
    }

    public func isAppVersionValid() -> Bool {
        XCTContext.runActivity(named: "Validating app version") { _ in
            return self.aboutSettingsPage.version.label.hasPrefix("Version \(ApplicationState.shared.general.version) of")
        }
    }

    public func isCopyrightValid() -> Bool {
        XCTContext.runActivity(named: "Validating Copyright") { _ in
            let currentYear = Calendar.current.component(.year, from: Date())
            return self.aboutSettingsPage.copyright.label == "2008-\(currentYear) Yandex LLC"
        }
    }

    public func closeAboutSettings() throws {
        try XCTContext.runActivity(named: "Closing About settings and going to Root settings") { _ in
            try self.aboutSettingsPage.backButton.tapCarefully()
        }
    }
}
