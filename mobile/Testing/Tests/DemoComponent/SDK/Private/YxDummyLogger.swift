//  Created by Nikolai Puchko on 06.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.

final class YxDummyLogger: YxLogger {
    func clean() {
      
    }

    func _debug(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _info(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _warning(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, error: Error) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String], crash: Bool) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, error: Error, crash: Bool) {

    }
}
