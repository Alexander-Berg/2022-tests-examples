//
//  NSAttributedStringTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 4/6/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRECoreUI
import YREDesignKit

final class NSAttributedStringTests: XCTestCase {
    func testLinesCount11() {
        // swiftlint:disable:next line_length
        let string = "Small batch pinterest jianbing austin fingerstache hell of occupy pabst cliche mixtape lo-fi shoreditch skateboard. Tumblr keffiyeh cornhole aesthetic put a bird on it shoreditch irony dreamcatcher flannel hammock sustainable butcher. Tousled green juice activated charcoal, beard ethical brunch heirloom 90's tote bag bitters letterpress and readymade privet. Vaporware food truck trust fund hexagon, keytar kitsch yuccie everyday carry meggings jean shorts leggings subway tile cliche the next level. Cloud bread ramps viral, slow-carb vinyl selfies swag."
        let style = TextStyle {
            $0.font = FigmaFontScheme.fixed.regular16
            $0.foregroundColor = ColorScheme.Text.primary
        }
        let attributedString = string.yre_attributed(style)
        let linesCount = attributedString.yre_linesCount(width: 420)
        let expectedLinesCount = 11
        XCTAssertEqual(linesCount, expectedLinesCount)

        let label = UILabel()
        label.numberOfLines = 0
        label.frame.size = .init(width: 420, height: 300)
        label.attributedText = attributedString
        label.backgroundColor = ColorScheme.Background.primary
        label.sizeToFit()
        self.assertSnapshot(label)
    }

    func testLinesCount10() {
        // swiftlint:disable:next line_length
        let string = "Small batch pinterest jianbing austin fingerstache hell ol occupy pabst cliche mixtape lo-fi shoreditch skateboard. Tumblr keffiyeh cornhole aesthetic put a bird on it shoreditch irony dreamcatcher flannel hammock sustainable butcher. Tousled green juice activated charcoal, beard ethical brunch heirloom 90's tote bag bitters letterpress readymade. Vaporware food truck trust fund hexagon, keytar kitsch yuccie everyday carry meggings jean shorts leggings subway tile cliche next level. Cloud bread ramps viral, slow-carb vinyl selfies."
        let style = TextStyle {
            $0.font = FigmaFontScheme.fixed.regular16
            $0.foregroundColor = ColorScheme.Text.primary
        }
        let attributedString = string.yre_attributed(style)
        let linesCount = attributedString.yre_linesCount(width: 420)
        let expectedLinesCount = 10
        XCTAssertEqual(linesCount, expectedLinesCount)

        // Create a snapshot to verify that `expectedLinesCount` is correct.
        let label = UILabel()
        label.numberOfLines = 0
        label.frame.size = .init(width: 420, height: 300)
        label.attributedText = attributedString
        label.backgroundColor = ColorScheme.Background.primary
        label.sizeToFit()
        self.assertSnapshot(label)
    }
}
