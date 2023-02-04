//
//  Utils.swift
//  Tests
//
//  Created by Andrei Iarosh on 25.03.2021.
//

import XCTest
import Dispatch
import Foundation

extension BaseUnitTest {
    func asyncAfter(_ time: TimeInterval, _ closure: @escaping () -> Void) {
        let exp = expectation(description: "Dummy expectation")

        DispatchQueue.main.asyncAfter(deadline: .now() + time, execute: {
            exp.fulfill()
            closure()
        })

        wait(for: [exp], timeout: time + 1)
    }
}
