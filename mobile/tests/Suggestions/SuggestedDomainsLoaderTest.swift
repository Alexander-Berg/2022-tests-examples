//
//  SuggestedDomainsLoaderTest.swift
//  ExclusiveEmailTests
//
//  Created by Timur Turaev on 02.07.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import ExclusiveEmail

internal final class SuggestedDomainsLoaderTest: XCTestCase {
    private var httpClient: HTTPClientProtocol?

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
        //            .realAndCapture(withTabs: withTabs)
        //            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)
    }

    func testLoadingSuggestions() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        let expectation = self.expectation(description: #function)
        loader.loadSuggestionsFor(email: Email(login: "abc", domain: "yandex"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .occupied)
            XCTAssertEqual(model.emails.count, 2)
            XCTAssertTrue(model.emails.allSatisfy { $0.login == "abc" })

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingSuggestionsResponseWithoutStatus() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        let expectation = self.expectation(description: #function)
        loader.loadSuggestionsFor(email: Email(login: "abc", domain: "yandex"), limit: 2) { result in
            let error = try! XCTUnwrap(result.toError())
            XCTAssertTrue(error.localizedDescription.contains("doesnt contains status for not-zero suggest request"))
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingZeroSuggest() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        let expectation = self.expectation(description: #function)
        loader.loadZeroSuggest { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.email, Email(login: "doxxerACM", domain: "timur-1.ru"))

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingEmptyZeroSuggest() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        let expectation = self.expectation(description: #function)
        loader.loadZeroSuggest { result in
            let error = try! XCTUnwrap(result.toError())
            XCTAssertTrue(error.localizedDescription.contains("No email in response"))
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingSuggestsOfValidDomain() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        var expectation = self.expectation(description: #function)

        // empty
        loader.loadSuggestionsFor(email: Email(login: "", domain: "someValidDomain"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .available)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        expectation = self.expectation(description: #function)

        // valid
        loader.loadSuggestionsFor(email: Email(login: "abc", domain: "someValidDomain"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .available)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        expectation = self.expectation(description: #function)

        // invalid
        loader.loadSuggestionsFor(email: Email(login: "русские буквы", domain: "someValidDomain"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .available)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingSuggestsOfEmptyDomain() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        var expectation = self.expectation(description: #function)

        // empty
        loader.loadSuggestionsFor(email: Email(login: "", domain: ""), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .notAllowed)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        expectation = self.expectation(description: #function)

        // valid
        loader.loadSuggestionsFor(email: Email(login: "abc", domain: ""), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .notAllowed)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        expectation = self.expectation(description: #function)

        // invalid
        loader.loadSuggestionsFor(email: Email(login: "русские буквы", domain: ""), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .notAllowed)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }

    func testLoadingSuggestsOfInvalidDomain() throws {
        let loader = SuggestedDomainsLoader(httpClient: self.httpClient!)

        var expectation = self.expectation(description: #function)

        // empty
        loader.loadSuggestionsFor(email: Email(login: "", domain: "русские буквы в домене"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .available)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        expectation = self.expectation(description: #function)

        // valid
        loader.loadSuggestionsFor(email: Email(login: "abc", domain: "русские буквы в домене"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .available)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        expectation = self.expectation(description: #function)

        // invalid
        loader.loadSuggestionsFor(email: Email(login: "русские буквы", domain: "русские буквы в домене"), limit: 2) { result in
            let model = try! XCTUnwrap(result.toOptional(), result.toError()!.localizedDescription)

            XCTAssertEqual(model.domainStatus, .available)
            XCTAssertEqual(model.emails.count, 2)

            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
    }
}
