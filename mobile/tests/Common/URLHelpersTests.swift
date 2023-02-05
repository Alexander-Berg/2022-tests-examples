//
//  URLHelpersTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 25.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class URLHelpersTests: XCTestCase {
    
    func testPercentEscapes() {
        let testString = ":/?&=;+!@#$()',*[]"
        let correctString = "%3A%2F%3F%26%3D%3B%2B%21%40%23%24%28%29%27%2C%2A%5B%5D"
        XCTAssertEqual(testString.percentEscapedString(), correctString)
    }
}
