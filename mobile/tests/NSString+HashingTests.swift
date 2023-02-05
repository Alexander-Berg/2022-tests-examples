//
//  NSString+HashingTests.swift
//  YandexDisk
//
//  Created by Alexander Sinusov on 17.08.16.
//  Copyright © 2016 Yandex. All rights reserved.
//

import XCTest
#if !DEV_TEST
@testable import YandexDisk
#endif

class NSStringHashingTests: XCTestCase {
    let testString = "String for hashing"
    let testStringMD5 = "94d49f8845d5f628fb030b2d7fc3b8be"
    let testStringSHA1 = "76adbf82e8ebbe4e3f37ec3837ca4144f7e46ed0"
    let testStringSHA256 = "7f18f714dd192910c51c52e455a66731e4da365b25dca8031df6eb840c4b5466"

    func testMD5() {
        let hash: String = testString.md5
        XCTAssertEqual(hash, testStringMD5)
    }

    func testSHA1() {
        let hash: String = testString.sha1
        XCTAssertEqual(hash, testStringSHA1)
    }

    func testSHA256() {
        let hash: String = testString.sha256
        XCTAssertEqual(hash, testStringSHA256)
    }
}
