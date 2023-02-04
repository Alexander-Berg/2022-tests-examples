//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public enum Assertion {
    public static func assert(
        _ condition: @autoclosure () -> Bool,
        _ message: @autoclosure () -> String = .init(),
        file: StaticString = #file,
        function: StaticString = #function,
        line: UInt = #line
    ) {
        if condition() { return }

        self.assertionHandler?.handleFailure(
            disposition: .body,
            level: .assert,
            message: message(),
            file: file,
            function: function,
            line: line
        )
    }

    public static func precondition(
        _ condition: @autoclosure () -> Bool,
        _ message: @autoclosure () -> String = .init(),
        file: StaticString = #file,
        function: StaticString = #function,
        line: UInt = #line
    ) {
        if condition() { return }

        self.assertionHandler?.handleFailure(
            disposition: .body,
            level: .precondition,
            message: message(),
            file: file,
            function: function,
            line: line
        )
    }

    public static func assertionFailure(
        _ message: @autoclosure () -> String = .init(),
        file: StaticString = #file,
        function: StaticString = #function,
        line: UInt = #line
    ) {
        self.assertionHandler?.handleFailure(
            disposition: .body,
            level: .assert,
            message: message(),
            file: file,
            function: function,
            line: line
        )
    }

    public static func preconditionFailure(
        _ message: @autoclosure () -> String = .init(),
        file: StaticString = #file,
        function: StaticString = #function,
        line: UInt = #line
    ) {
        self.assertionHandler?.handleFailure(
            disposition: .body,
            level: .precondition,
            message: message(),
            file: file,
            function: function,
            line: line
        )
    }

    public static func fatalError(
        _ message: @autoclosure () -> String = .init(),
        file: StaticString = #file,
        function: StaticString = #function,
        line: UInt = #line
    ) {
        self.assertionHandler?.handleFailure(
            disposition: .body,
            level: .fatal,
            message: message(),
            file: file,
            function: function,
            line: line
        )
    }

    public static func parameterAssert(
        _ condition: @autoclosure () -> Bool,
        _ message: @autoclosure () -> String = .init(),
        file: StaticString = #file,
        function: StaticString = #function,
        line: UInt = #line
    ) {
        if condition() { return }

        self.assertionHandler?.handleFailure(
            disposition: .parameter,
            level: .assert,
            message: message(),
            file: file,
            function: function,
            line: line
        )
    }

    public static var assertionHandler: AssertionHandlerProtocol?
}
