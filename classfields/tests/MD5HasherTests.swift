//
//  MD5HasherTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Fedor Solovev on 12.05.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

final class MD5HasherTests: XCTestCase {
    private var md5hasher: MD5Hasher?

    override func setUp() {
        super.setUp()

        self.md5hasher = MD5Hasher()
    }

    override func tearDown() {
        super.tearDown()
        
        self.md5hasher = nil
    }

    func testHashValue_WhenNormalString_ShouldBeCorrectHash() {
        // given
        let string = "hello"

        // when
        let md5 = self.md5hasher?.hashValue(string)

        // then
        XCTAssertEqual(md5, "5d41402abc4b2a76b9719d911017c592", "md5 hashes are not equal")
    }

    func testHashValue_WhenEmptyString_ShouldBeCorrectHash() {
        // given
        let string = ""

        // when
        let md5 = self.md5hasher?.hashValue(string)

        // then
        XCTAssertEqual(md5, "d41d8cd98f00b204e9800998ecf8427e", "md5 hashes are not equal")
    }
}
