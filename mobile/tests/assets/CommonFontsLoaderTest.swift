//
//  CommonFontsLoaderTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 28.11.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class CommonFontsLoaderTest: XCTestCase {
    
    func testRegularFont() {
        let font = CommonFontsLoader.regular(withSize: 17.0)
        
        XCTAssertNotNil(font)
    }
    
    func testMediumFont() {
        let font = CommonFontsLoader.medium(withSize: 17.0)
        
        XCTAssertNotNil(font)
    }
    
    func testLightFont() {
        let font = CommonFontsLoader.light(withSize: 17.0)
        
        XCTAssertNotNil(font)
    }
    
    func testBoldFont() {
        let font = CommonFontsLoader.bold(withSize: 17.0)
        
        XCTAssertNotNil(font)
    }
    
    func testLogotypeFont() {
        let font = CommonFontsLoader.logotype(withSize: 17.0)
        
        XCTAssertNotNil(font)
    }
    
}
