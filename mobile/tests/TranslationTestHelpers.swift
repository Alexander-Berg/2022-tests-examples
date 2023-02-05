//
//  TranslationTestHelpers.swift
//  TranslationServicesTests
//
//  Created by Nikita Ermolenko on 12.05.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import Utils

internal enum TranslationTestHelpers {

    static var generateMessagesCacheFolderNameURL: URL {
        let url = try! FileManager.default
            .url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
            .appendingPathComponent("Messages", isDirectory: true)

        try! FileManager.default.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)

        return url
    }

    static func clearStorage() {
        ExtensionsEnvironment.sharedDefaults.removeObject(forKey: "recentToLanguageCodes")
        ExtensionsEnvironment.sharedDefaults.removeObject(forKey: "recentFromLanguageCodes")
        ExtensionsEnvironment.sharedDefaults.removeObject(forKey: "allAvailableLanguages")
        ExtensionsEnvironment.sharedDefaults.removeObject(forKey: "ignoredLanguageCodes")
    }
}
