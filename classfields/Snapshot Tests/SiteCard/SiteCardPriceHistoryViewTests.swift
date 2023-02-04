//
//  SiteCardPriceHistoryViewTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 16.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YRESiteCardModule
import SwiftUI
import YREDesignKit

final class SiteCardPriceHistoryViewTests: XCTestCase {
    func testIncreasingChart() {
        let priceHistoryView = PriceHistoryView(
            points: [8_500_000, 9_000_000, 9_200_000, 10_000_000],
            priceTitle: "4,2 млн ₽",
            style: .increasing,
            onTap: {}
        ).frame(width: UIScreen.main.bounds.width - 32)
        let view = ZStack {
            ColorScheme.Background.primary.color
            priceHistoryView
        }
        self.assertSnapshot(view)
    }

    func testDecreasingChart() {
        let priceHistoryView = PriceHistoryView(
            points: [10_000_000, 9_200_000, 9_000_000, 8_500_000],
            priceTitle: "4,2 млн ₽",
            style: .decreasing,
            onTap: {}
        ).frame(width: UIScreen.main.bounds.width - 32)
        let view = ZStack {
            ColorScheme.Background.primary.color
            priceHistoryView
        }
        self.assertSnapshot(view)
    }

    func testNeutralChart() {
        let priceHistoryView = PriceHistoryView(
            points: [10_000_000, 9_200_000, 9_000_000, 9_000_000],
            priceTitle: "4,2 млн ₽",
            style: .neutral,
            onTap: {}
        ).frame(width: UIScreen.main.bounds.width - 32)
        let view = ZStack {
            ColorScheme.Background.primary.color
            priceHistoryView
        }
        self.assertSnapshot(view)
    }
}
