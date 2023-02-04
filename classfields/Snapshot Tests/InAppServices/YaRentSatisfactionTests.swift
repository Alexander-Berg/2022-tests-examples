//
//  YaRentSatisfactionTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 06.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
@testable import YREYaRentSatisfactionRatingModule

final class YaRentSatisfactionTests: XCTestCase {
    func testUnselectedScore() {
        let view = self.makeSatisfactionRatingView()
        view.selectItem(at: 0, animated: false)
        self.assertSnapshot(view)
    }

    func testPositiveScore() {
        let view = self.makeSatisfactionRatingView()
        view.selectItem(at: 1, animated: false)
        self.assertSnapshot(view)
    }

    func testNeutralScore() {
        let view = self.makeSatisfactionRatingView()
        view.selectItem(at: 4, animated: false)
        self.assertSnapshot(view)
    }
    
    func testNegativeScore() {
        let view = self.makeSatisfactionRatingView()
        view.selectItem(at: 10, animated: false)
        self.assertSnapshot(view)
    }

    private func makeSatisfactionRatingView() -> YaRentSatisfactionRatingContentView {
        let view = YaRentSatisfactionRatingContentView()
        view.frame = UIScreen.main.bounds
        view.backgroundColor = ColorScheme.Background.primary
        view.layoutIfNeeded()
        return view
    }
}
