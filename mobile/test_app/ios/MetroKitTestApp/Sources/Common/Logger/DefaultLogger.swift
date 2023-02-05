//
//  DefaultLogger.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 29/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public final class DefaultLogger {

    public func log(level: LogLevel, tag: String?, message: String, file: String? = #file, line: Int? = #line,
        function: String? = #function)
    {
        if !(filter?.shouldWrite(level: level, tag: tag) ?? true) {
            return
        }
        
        let retMessage: String
        
        if let prefix = DefaultLogger.getMessagePrefix(file: file, line: line, function: function) {
            retMessage = "\(prefix) → \(message)"
        } else {
            retMessage = message
        }
        
        _writer?.write(level: level, tag: tag, message: retMessage)
    }
    
    // MARK: - Private
    
    private weak var _filter: LogFilter?
    private weak var _writer: LogWriter?
    
    private static func getMessagePrefix(file: String?, line: Int?, function: String?) -> String? {
        if file == nil && line == nil && function == nil {
            return nil
        }
        
        let filename = file?.components(separatedBy: "/").last ?? ""
        let lineStr = line.map { ":\($0)" } ?? ""
        let funcStr = function.map { " \($0)" } ?? ""
        
        return "\(filename)\(lineStr)\(funcStr)"
    }
    
}

extension DefaultLogger: Logger {
    
    public var filter: LogFilter? {
        get {
            return _filter
        }
        set {
            _filter = newValue
        }
    }
    
    public var writer: LogWriter? {
        get {
            return _writer
        }
        set {
            _writer = newValue
        }
    }
    
    public func log(level: LogLevel, tag: String?, message: String) {
        _writer?.write(level: level, tag: tag, message: message)
    }

}
