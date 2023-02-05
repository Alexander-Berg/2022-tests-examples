//
//  DomainStatusLoaderTest.swift
//  ExclusiveEmailTests
//
//  Created by Timur Turaev on 22.07.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import ExclusiveEmail

internal final class DomainStatusLoaderTest: XCTestCase {
    private var httpClient: HTTPClientProtocol!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
        //            .realAndCapture(withTabs: withTabs)
        //            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)
    }

    func testLoadingNonExistantExclusiveEmail() throws {
        let loader = DomainStatusLoader(httpClient: self.httpClient)

        let expectation = self.expectation(description: #function)
        loader.loadDomainStatus { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)
            XCTAssertEqual(model, .notFound)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingExistantExclusiveEmail() throws {
        let loader = DomainStatusLoader(httpClient: self.httpClient)

        let expectation = self.expectation(description: #function)
        loader.loadDomainStatus { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            // swiftlint:disable multiline_arguments
            let nextAvailableRegisterDate = DateComponents(calendar: .current,
                                                           year: 2021, month: 9, day: 13,
                                                           hour: 14, minute: 17, second: 29, nanosecond: 135_999_917).date!
            let expectedModel = DomainStatusModel.found(.init(email: Email(login: "e", domain: "doxxer.ru"),
                                                              status: .registered,
                                                              registerAllowed: false,
                                                              nextAvailableRegisterDate: nextAvailableRegisterDate))

            XCTAssertEqual(model, expectedModel)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testReceivingUnknownStatus() throws {
        let loader = DomainStatusLoader(httpClient: self.httpClient)

        let expectation = self.expectation(description: #function)
        loader.loadDomainStatus { result in
            let model = try! XCTUnwrap(result.toError())

            print(model.localizedDescription)
            XCTAssertTrue(model.localizedDescription.contains("bad response data"))
            XCTAssertFalse(model.yo_isConnectionError)
            XCTAssertFalse(model.yo_isNetworkError)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testReceivingMalformedResponse() throws {
        let loader = DomainStatusLoader(httpClient: self.httpClient)

        let expectation = self.expectation(description: #function)
        loader.loadDomainStatus { result in
            let model = try! XCTUnwrap(result.toError())

            print(model.localizedDescription)
            XCTAssertTrue(model.localizedDescription.contains("bad response data"))
            XCTAssertFalse(model.yo_isConnectionError)
            XCTAssertFalse(model.yo_isNetworkError)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testPendingRegistrarResponse() {
        let loader = DomainStatusLoader(httpClient: self.httpClient)

        let expectation = self.expectation(description: #function)
        loader.loadDomainStatus { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            let expectedModel = DomainStatusModel.found(.init(email: Email(login: "mobilemail3605new", domain: "user-33.ru"),
                                                              status: .pending,
                                                              registerAllowed: false,
                                                              nextAvailableRegisterDate: nil))

            XCTAssertEqual(model, expectedModel)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }
}
