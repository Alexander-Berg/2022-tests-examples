//
//  SemanticContainerTest.swift
//  StylerTests
//
//  Created by Nikita Ermolenko on 21.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import Styler

internal final class SemanticContainerTest: XCTestCase {
    private typealias Container = SemanticContainer<TestKey, TestValue, TestTransform>

    private enum TestData {
        static let correctExample = """
        {
            "hook": {
                "val": { "string": "hook" }
            },
            "line": {
                "ref": "hook"
            },
            "sinker": {
                "ref": "hook",
                "mod": { "string": "sinker" }
            }
        }
        """

        static let recursiveRef = """
        {
            "hook": {
                "ref": "line"
            },
            "line": {
                "ref": "sinker"
            },
            "sinker": {
                "ref": "hook",
                "mod": { "string": "sinker" }
            }
        }
        """

        static let missingKey = """
        {
            "hook": {
                "ref": "line"
            },
            "line": {
                "val": { "string": "line" }
            }
        }
        """

        static let extraKey = """
        {
            "hook": {
                "val": { "string": "hook" }
            },
            "line": {
                "ref": "hook"
            },
            "sinker": {
                "ref": "hook",
                "mod": { "string": "sinker" }
            },
            "blinker": {
                "val": { "string": "this is not expected to be here" }
            }
        }
        """

        static let failedTransform = """
        {
            "hook": {
                "val": { "string": "hook" }
            },
            "line": {
                "ref": "hook"
            },
            "sinker": {
                "ref": "hook",
                "mod": { "string": "puke" }
            }
        }
        """
    }

    func testShouldParseFromJSON() {
        func runTest() throws {
            let container = try self.container(from: TestData.correctExample)

            XCTAssertEqual(container.value(for: .hook).string, "hook")
            XCTAssertEqual(container.value(for: .line).string, "hook")
            XCTAssertEqual(container.value(for: .sinker).string, "sinkerhook")
        }
        XCTAssertNoThrow(try runTest())
    }

    func testShouldDetectReferenceRecursion() {
        let result = Result(catching: { try self.container(from: TestData.recursiveRef) })
        guard case .failure(StylerError.internalInconsistency(let what)) = result, what.contains("recursion") else {
            XCTFail("Result not satisfying: \(result)")
            return
        }
    }

    func testShouldDetectMissingKey() {
        let result = Result(catching: { try self.container(from: TestData.missingKey) })
        guard case .failure(StylerError.internalInconsistency(let what)) = result, what.contains("missing") else {
            XCTFail("Result not satisfying: \(result)")
            return
        }
    }

    func testShouldIgnoreExtraKey() {
        XCTAssertNoThrow(try self.container(from: TestData.extraKey))
    }

    func testShouldFailedTransform() {
        let result = Result(catching: { try self.container(from: TestData.failedTransform) })
        guard case .failure(StylerError.internalInconsistency(let what)) = result, what.contains("buegh") else {
            XCTFail("Result not satisfying: \(result)")
            return
        }
    }

    private func container(from string: String) throws -> Container {
        return try JSONDecoder().decode(Container.self, from: string.data(using: .utf8).value())
    }
}

private enum TestKey: String, SemanticKey, CaseIterable {
    case hook, line, sinker
}

private struct TestValue: Codable {
    fileprivate let string: String
}

private struct TestTransform: SemanticValueTransform, Codable {
    fileprivate let string: String

    func apply(to value: TestValue) throws -> TestValue {
        guard string != "puke" else {
            throw StylerError.internalInconsistency("buegh")
        }
        return TestValue(string: self.string + value.string)
    }
}
