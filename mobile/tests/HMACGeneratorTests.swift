//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YandexDisk

final class StringHMACTests: XCTestCase {
    func testHMACWithMD5Algorithm() {
        let hmacGenerator = HMACGenerator(algorithm: .MD5)
        let result = try? hmacGenerator.hmac(from: "test", withKey: "test")
        XCTAssertEqual(result, "cd4b0dcbe0f4538b979fb73664f51abe")
    }

    func testHMACWithSHA1Algorithm() {
        let hmacGenerator = HMACGenerator(algorithm: .SHA1)
        let result = try? hmacGenerator.hmac(from: "test", withKey: "test")
        XCTAssertEqual(result, "0c94515c15e5095b8a87a50ba0df3bf38ed05fe6")
    }

    func testHMACWithSha256Algorithm() {
        let hmacGenerator = HMACGenerator(algorithm: .SHA256)
        let result = try? hmacGenerator.hmac(from: "test", withKey: "test")
        XCTAssertEqual(result, "88cd2108b5347d973cf39cdf9053d7dd42704876d8c9a9bd8e2d168259d3ddf7")
    }
}
