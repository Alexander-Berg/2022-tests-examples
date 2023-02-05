//
//  DefaultLogger.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 29/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public struct Log {

    public static var logger: Logger { return _logger }

    public static func log(level: LogLevel, tag: String?, message: String, file: String = #file, line: Int = #line,
        function: String = #function)
    {
        _logger.log(level: level, tag: tag, message: message, file: file, line: line, function: function)
    }
    
    // MARK: - Tag and message
    
    public static func error(tag: String?, message: String, file: String = #file, line: Int = #line,
        function: String = #function)
    {
        log(level: .error, tag: tag, message: message, file: file, line: line, function: function)
    }
    
    public static func warning(tag: String?, message: String, file: String = #file, line: Int = #line,
        function: String = #function)
    {
        log(level: .warning, tag: tag, message: message, file: file, line: line, function: function)
    }
    
    public static func debug(tag: String?, message: String, file: String = #file, line: Int = #line,
        function: String = #function)
    {
        log(level: .debug, tag: tag, message: message, file: file, line: line, function: function)
    }
    
    public static func info(tag: String?, message: String, file: String = #file, line: Int = #line,
        function: String = #function)
    {
        log(level: .info, tag: tag, message: message, file: file, line: line, function: function)
    }
    
    // MARK: - Message only
    
    public static func error(_ message: String, file: String = #file, line: Int = #line, function: String = #function) {
        log(level: .error, tag: nil, message: message, file: file, line: line, function: function)
    }
    
    public static func warning(_ message: String, file: String = #file, line: Int = #line, function: String = #function) {
        log(level: .warning, tag: nil, message: message, file: file, line: line, function: function)
    }
    
    public static func debug(_ message: String, file: String = #file, line: Int = #line, function: String = #function) {
        log(level: .debug, tag: nil, message: message, file: file, line: line, function: function)
    }
    
    public static func info(_ message: String, file: String = #file, line: Int = #line, function: String = #function) {
        log(level: .info, tag: nil, message: message, file: file, line: line, function: function)
    }
    
    // MARK: - Private
    
    private init() {}
    
    private static let _logger = DefaultLogger()

}

