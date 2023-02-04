//
//  XCTestExpectation+Ensure.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 08.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

extension XCTestExpectation {
    func yreEnsureFullFilledWithTimeout(
        timeout: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) {
        XCTContext.runActivity(named: "Проверяем исполнение ожидания: \(self.expectationDescription)") { _ -> Void in
            let result = XCTWaiter.yreWait(for: [self], timeout: timeout)
            XCTAssert(result, message, file: file, line: line)
        }
    }
}

extension Array where Element == XCTestExpectation {
    func yreEnsureFullFilledWithTimeout(
        timeout: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) {
        for expectation in self {
            expectation.yreEnsureFullFilledWithTimeout(
                timeout: timeout,
                message: message,
                file: file,
                line: line
            )
        }
    }
}
