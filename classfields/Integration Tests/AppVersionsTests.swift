//
//  AppVersionsTests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 9/17/18.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation

import XCTest
@testable import YandexRealty

class AppVersionsTests: XCTestCase {
    
    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }
    
    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }
    
    func testBundleVersion() {
        // swiftlint:disable:next line_length
        XCTAssertTrue(AppVersions.isProbeVersionEqualToStored(), "`AppVersions` probe version is different from CFBundleShortVersionString stored in Info.plist for development localization. Did you forget to add new `AppVersions.Version` enum case and version?")
    }
}
