//
//  TestConfigHelper.swift
//  YRETestsUtils
//
//  Created by Arkady Smirnov on 2/26/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import UIKit

public class TestConfigHelper {
    public static func continueAfterFailure() -> Bool {
        let continueAfterFailure = self.object(Bool.self, forKey: "YRETestsContinueAfterFailure")
        return continueAfterFailure ?? false
    }

    public static func needsCollectSnapshots() -> Bool {
        let collectSnapshots = self.object(Bool.self, forKey: "YRETestsCollectSnapshots")
        return collectSnapshots ?? false
    }

    static func isDarkMode() -> Bool {
        // Get UIUserInterfaceStyle from fastlane (and teamcity) when launching on build agents
        guard let style = self.object(String.self, forKey: "UIUserInterfaceStyle") else {
            // Or get from device instead, if launching locally to generate new snapshots
            return UIScreen.main.traitCollection.userInterfaceStyle == .dark
        }
        return style == "Dark"
    }

    // MARK: - Private

    private final class StubClass {}

    private static func object<T>(_ type: T.Type, forKey key: String) -> T? {
        return Bundle(for: StubClass.self).object(forInfoDictionaryKey: key) as? T
    }
}
