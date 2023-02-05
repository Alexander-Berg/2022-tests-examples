//
//  ChangeLoginLoaderTest.swift
//  ChangeLoginLoaderTest
//
//  Created by Timur Turaev on 29.07.2021.
//

import XCTest
import TestUtils
@testable import ExclusiveEmail

internal final class ChangeLoginLoaderTest: XCTestCase {
    func testChangeExclusiveEmailLogin() throws {
        let kind: TestHTTPClientKind =
        //            .realAndCapture(withTabs: withTabs)
        //            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        let loader = ChangeLoginLoader(httpClient: httpClient)

        let expectation = self.expectation(description: #function)
        loader.changeExclusiveEmailLogin("test_login") { result in
            XCTAssertNil(result.toError())
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }
}
