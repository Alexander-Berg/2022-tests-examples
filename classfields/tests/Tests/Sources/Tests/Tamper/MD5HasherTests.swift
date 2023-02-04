@testable import AutoRuUtils
import XCTest
import Foundation

final class MD5HasherTests: BaseUnitTest {
    func test_md5HashString() {
        let cases: [(String, String)] = [
            ("autoru", "fca83a8944f5cc4d21ce06d923050e7f"),
            ("", "d41d8cd98f00b204e9800998ecf8427e"),
            ("a", "0cc175b9c0f1b6a831c399e269772661"),
            ("autoruautoruautoruautoruautoruautoru", "eb4a636e9df08dad2841246b53787614")
        ]

        for c in cases {
            let hash = MD5Hasher.hashedString(from: c.0)
            XCTAssert(hash == c.1, "Неверный мд5 хеш для '\(c.0)': '\(hash)' вместо '\(c.1)'")
        }
    }

    func test_md5HashData() {
        let cases: [(String, Data)] = [
            ("autoru", Data([0xfc, 0xa8, 0x3a, 0x89, 0x44, 0xf5, 0xcc, 0x4d, 0x21, 0xce, 0x06, 0xd9, 0x23, 0x05, 0x0e, 0x7f])),
            ("", Data([0xd4, 0x1d, 0x8c, 0xd9, 0x8f, 0x00, 0xb2, 0x04, 0xe9, 0x80, 0x09, 0x98, 0xec, 0xf8, 0x42, 0x7e])),
            ("a", Data([0x0c, 0xc1, 0x75, 0xb9, 0xc0, 0xf1, 0xb6, 0xa8, 0x31, 0xc3, 0x99, 0xe2, 0x69, 0x77, 0x26, 0x61])),
            ("autoruautoruautoruautoruautoruautoru",
             Data([0xeb, 0x4a, 0x63, 0x6e, 0x9d, 0xf0, 0x8d, 0xad, 0x28, 0x41, 0x24, 0x6b, 0x53, 0x78, 0x76, 0x14]))
        ]

        for c in cases {
            let hash = MD5Hasher.hashedData(from: c.0)
            XCTAssert(hash == c.1, "Неверный мд5 хеш для '\(c.0)'")
        }
    }
}
