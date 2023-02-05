//  Created by Denis Malykh on 13.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

@testable import YxSwissKnife

final class FakeLogger: YxLogger {

    struct LogData {
        let message: String
        let newlines: Bool
        let file: String
        let line: UInt
        let column: Int
        let function: String
        let params: [String: String]?
        let error: Error?
        let crash: Bool?

        init(
            message: String,
            newlines: Bool,
            file: String,
            line: UInt,
            column: Int,
            function: String,
            params: [String: String]? = nil,
            error: Error? = nil,
            crash: Bool? = nil
        ) {
            self.message = message
            self.newlines = newlines
            self.file = file
            self.line = line
            self.column = column
            self.function = function
            self.params = params
            self.error = error
            self.crash = crash
        }
    }

    private(set) var cleanCalledTimes = 0
    private(set) var debugEntries = [LogData]()
    private(set) var infoEntries = [LogData]()
    private(set) var warningEntries = [LogData]()
    private(set) var errorEntries = [LogData]()


    func clean() {
        cleanCalledTimes += 1
    }

    func _debug(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        params: [String: String]
    ) {
        debugEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                params: params
            )
        )
    }

    func _info(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        params: [String: String]
    ) {
        infoEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                params: params
            )
        )
    }

    func _warning(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        params: [String: String]
    ) {
        warningEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                params: params
            )
        )
    }

    func _error(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        params: [String: String]
    ) {
        errorEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                params: params
            )
        )
    }

    func _error(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        error: Error
    ) {
        errorEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                error: error
            )
        )
    }

    func _error(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        params: [String: String],
        crash: Bool
    ) {
        errorEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                params: params,
                crash: crash
            )
        )
    }

    func _error(
        _ message: String,
        newlines: Bool,
        file: String,
        line: UInt,
        column: Int,
        function: String,
        error: Error,
        crash: Bool
    ) {
        errorEntries.append(
            LogData(
                message: message,
                newlines: newlines,
                file: file,
                line: line,
                column: column,
                function: function,
                error: error,
                crash: crash
            )
        )
    }
}
