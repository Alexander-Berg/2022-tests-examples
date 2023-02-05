//
//  RegisterLoaderTest.swift
//  RegisterLoaderTest
//
//  Created by Timur Turaev on 29.07.2021.
//

import XCTest
import TestUtils
@testable import ExclusiveEmail

internal final class RegisterLoaderTest: XCTestCase {
    func testRegisterEmail() throws {
        let kind: TestHTTPClientKind =
        //            .realAndCapture(withTabs: withTabs)
        //            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)
        
        let loader = RegisterLoader(httpClient: httpClient)

        let expectation = self.expectation(description: #function)
        loader.registerEmail(Email(login: "test_login", domain: "test_domain")) { result in
            XCTAssertNil(result.toError())
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testPaymentRequiredError() throws {
        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: true)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        let loader = RegisterLoader(httpClient: httpClient)

        let expectation = self.expectation(description: #function)
        loader.registerEmail(Email(login: "test_login", domain: "test_domain")) { result in
            XCTAssertTrue(result.toError()!.yo_is402PaymentRequiredError)
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }
}
