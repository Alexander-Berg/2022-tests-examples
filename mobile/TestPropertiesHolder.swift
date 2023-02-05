//
// Created by Alexey Zarovny on 12.10.2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import Foundation
import XCTest

class TestPropertiesHolder {
    static let holder = TestPropertiesHolder()
    private var configDict: [String: String]!

    func getProperties() -> [String: String] {
        if configDict == nil {
            guard let propertiesFile = Bundle(for: type(of: self)).path(forAuxiliaryExecutable: "test.properties"),
                  let propertiesFileContent = try? String(contentsOfFile: propertiesFile) else {
                XCTFail("Unable to read properties file")
                return [:]
            }

            configDict = [:]

            let properties = propertiesFileContent.components(separatedBy: CharacterSet.newlines)
            properties.forEach { propertyString in
                if propertyString.isEmpty || propertyString.contains("=") == false {
                    return
                }
                let property = propertyString.components(separatedBy: "=")
                configDict[property[0]] = property[1]
            }
        }
        return configDict
    }
}

enum TestPropertiesKeys {
    static let useNewAccountManagement = "use.new.account.management"
    static let tusUrl = "tus.url"
    static let tusToken = "tus.token"
    static let tusConsumer = "tus.consumer"
    static let tusDefaultTags = "tus.default.tags"
    static let tusLockDuration = "tus.lock.duration"
}
