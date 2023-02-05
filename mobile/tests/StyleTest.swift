//
//  StyleTest.swift
//  StylerTests
//
//  Created by Nikita Ermolenko on 21.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Utils
@testable import Styler

internal final class StyleTest: XCTestCase {
    private var bundle: Bundle {
        return Bundle(for: type(of: self))
    }

    private struct DecodingHelper<T: Decodable>: Decodable {
        let value: T
    }

    func testShouldParseColorProperly() {
        func color(from string: String) throws -> UIColor {
            let data = try #"{ "value": "\#(string)" }"#.data(using: .utf8).value()
            return try JSONDecoder().decode(DecodingHelper<StyleColor>.self, from: data).value.color(in: self.bundle, compatibleWith: nil)
        }

        XCTAssertEqual(try? color(from: "#ffFFfFFf"), .init(red: 1, green: 1, blue: 1, alpha: 1))
        XCTAssertEqual(try? color(from: "#000000Ff"), .init(red: 0, green: 0, blue: 0, alpha: 1))
        XCTAssertEqual(try? color(from: "#00000000"), .init(red: 0, green: 0, blue: 0, alpha: 0))

        XCTAssertEqual(try? color(from: "#Ff0000Ff"), .red)
        XCTAssertEqual(try? color(from: "#00fF00Ff"), .green)
        XCTAssertEqual(try? color(from: "#0000FffF"), .blue)

        XCTAssertEqual(try? color(from: "Color"), .white)

        // only quadruplets are allowed
        XCTAssertThrowsError(try color(from: "#ff0000"))
        XCTAssertThrowsError(try color(from: "#ff0000ffff"))
        XCTAssertThrowsError(try color(from: "#fff"))
        // only hexadecimal symbols are allowed
        XCTAssertThrowsError(try color(from: "#rtrtrtrt"))
        XCTAssertThrowsError(try color(from: "#RTRTRTRT"))
        XCTAssertThrowsError(try color(from: "#ААAAAAFF"))
    }

    func testShouldParseImageProperly() {
        func image(from string: String) throws -> UIImage {
            let data = try #"{ "value": "\#(string)" }"#.data(using: .utf8).value()
            return try JSONDecoder().decode(DecodingHelper<StyleImage>.self, from: data).value.image(in: self.bundle, compatibleWith: nil)
        }

        let targetImage = UIImage(named: "Image", in: self.bundle, compatibleWith: nil)
        XCTAssertNotNil(targetImage)
        XCTAssertEqual(try? image(from: "Image"), targetImage)

        XCTAssertThrowsError(try image(from: "MissingImageDon'tEverAddSuchFileToThisTarget;)"))
    }

    func testShouldParseStyleProperly() {
        enum ColorKey: String, SemanticKey, CaseIterable {
            case white
        }

        enum ImageKey: String, SemanticKey, CaseIterable {
            case avatar
        }

        func desc(_ font: UIFont) -> String {
            var description = font.debugDescription
            description.removeFirst(27) // <UICTFont: 0x000000000000>
            return description
        }

        Result(catching: { try Style<ColorKey, ImageKey>(named: "Test", in: self.bundle) }).onValue { style in
            XCTAssertEqual(style.semantics, .light)
            XCTAssertEqual(style.color(forKey: .white), .white)
            XCTAssertEqual(style.image(forKey: .avatar), UIImage(named: "Image", in: self.bundle, compatibleWith: nil))
        }.onError { error in
            XCTFail("Parse failed with error: \(error)")
        }
    }
}
