//
//  XCTestExtensions.swift
//  TestUtils
//
//  Created by Timur Turaev on 17.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Combine
import XCTest
import Utils

public extension XCTestCase {
    var testClassName: String? {
        guard let test = self.testRun?.test else { return nil }

        guard let className = test.name.split(separator: " ").first else {
            return nil
        }

        return String(className.unicodeScalars.filter(CharacterSet.alphanumerics.contains))
    }

    var testFunctionName: String? {
        guard let test = self.testRun?.test else { return nil }

        guard let functionName = test.name.split(separator: " ").last else {
            return nil
        }

        return String(functionName.unicodeScalars.filter(CharacterSet.alphanumerics.contains))
    }

    var defaultFixtureName: String {
        return "\(self.testClassName!)-\(self.testFunctionName!)"
    }

    @discardableResult
    func waitFor<T: Publisher>(_ publisher: T,
                               timeout: TimeInterval = 1,
                               file: StaticString = #file,
                               line: UInt = #line) throws -> Result<T.Output> {
        var futureResult: Result<T.Output>?
        let expectation = self.expectation(description: "Awaiting publisher")

        let cancellable = publisher.sink { result in
            futureResult = result
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: timeout)
        cancellable.cancel()

        return try XCTUnwrap(futureResult, "Awaited publisher did not complete", file: file, line: line)
    }

    func wait(for block: @escaping () -> Bool,
              timeout: TimeInterval,
              file: StaticString = #file,
              function: String = #function,
              line: UInt = #line) {
        let startTime = CACurrentMediaTime()

        while !block() && CACurrentMediaTime() - startTime < timeout {
            RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.05))
        }

        if !block() {
            XCTFail("Wating failed in [\(file): \(function):\(line)]", file: file, line: line)
        }
    }

    func expectation<T>(evaluating block: @escaping (T) -> Bool, for object: T) -> XCTestExpectation {
        let predicate = NSPredicate { any, _ -> Bool in
            guard let object = any as? T else { return false }

            return block(object)
        }
        return self.expectation(for: predicate, evaluatedWith: object, handler: nil)
    }
}
