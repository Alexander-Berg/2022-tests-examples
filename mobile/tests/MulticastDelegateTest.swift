//
//  MulticastDelegateTest.swift
//  TurboApp-Unit-Tests
//
//  Created by Timur Turaev on 31.03.2022.
//

import XCTest
@testable import TurboApp

internal final class MulticastDelegateTest: XCTestCase {
    func testRecursiveInvokation() throws {
        let delegate = TestDelegate()
        let delegateBag = TurboAppMulticastDelegate<TestDelegate>()
        delegateBag.addDelegate(delegate)

        delegateBag.invoke { _ in
            delegateBag.invoke { _ in }
        }

        delegateBag.invoke { _ in
            delegateBag.invoke { _ in
                delegateBag.invoke { _ in
                    delegateBag.addDelegate(delegate)
                }
            }
        }
    }
}

private final class TestDelegate {}
