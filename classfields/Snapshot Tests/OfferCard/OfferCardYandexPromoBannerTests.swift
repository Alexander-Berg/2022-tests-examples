//
//  OfferCardYandexPromoBannerTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 08.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREOfferCardModule
import YREDesignKit

final class OfferCardYandexPromoBannerTests: XCTestCase {
    func testLayout() {
        let node = OfferCardYandexRentPromoBannerNode()
        self.assertSnapshot(node, backgroundColor: ColorScheme.Background.primary)
    }
}
