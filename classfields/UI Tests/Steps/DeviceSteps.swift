//
//  DeviceSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 24.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class DeviceSteps {
    @discardableResult
    func isPasteboardEquals(to text: String) -> Self {
        XCTContext.runActivity(named: "Проверяем соответствие буфера обмена строке \"\(text)\"") { _ -> Void in
            XCTAssertEqual(UIPasteboard.general.string, text)
        }
        return self
    }

    @discardableResult
    func isPasteboardEquals(to url: URL) -> Self {
        XCTContext.runActivity(named: "Проверяем соответствие буфера обмена урлу \"\(url.absoluteString)\"") { _ -> Void in
            // swiftlint:disable:next force_unwrapping
            XCTAssertTrue(url.yre_isEqualWithoutQueryItems(to: UIPasteboard.general.url!))
        }
        return self
    }
}
