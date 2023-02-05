//
//  SearchTest.swift
//  YandexMobileMailAutoTests
//
//  Created by Anastasia Kononova on 10/07/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class SearchTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.suggectAsYouType.rawValue]
    }

    public func testSuggestAsType() {
        // TODO: implement search test
    }
}
