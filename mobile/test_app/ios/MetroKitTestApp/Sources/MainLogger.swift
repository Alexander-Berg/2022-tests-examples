//
//  Logger.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 16/07/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import UIKit

final class MainLogger: NSObject {
    static let instance = MainLogger(YMLLoggerFactory.getLoggerInstance())
    
    static func setup(with window: UIWindow) {
        instance.writter.setup(with: window)
    }
    
    private init(_ impl: YMLLogger) {
        self.impl = impl
        filter = MainLogFilter()
        writter = MainLogWritter()
        
        impl.setFilterWith(filter)
        impl.addWriter(with: writter)

        #if DEBUG
            impl.isIsStdWriterEnabled = true
        #endif
    }
    
    private let impl: YMLLogger
    private let filter: MainLogFilter
    private let writter: MainLogWritter
}

// MARK: - Private: Filter

private class MainLogFilter: NSObject, YMLLogFilter {

    func shouldWrite(with logLevel: YMLLogLevel, tag: String) -> Bool {
        if logLevel == .info || logLevel == .debug {
            return false
        }
        
        return true
    }
    
}


// MARK: - Private: Writter

private class MainLogWritter: NSObject, YMLLogWriter {

    private let deviceLog = DeviceLog()
    
    func setup(with window: UIWindow) {
        deviceLog.container = window
    }
    
    // MARK: - YMLLogWriter

    func write(with logLevel: YMLLogLevel, tag: String, message: String) {
        let level: LogLevel
        
        switch logLevel {
        case .debug:
            level = .debug
        case .info:
            level = .info
        case .warning:
            level = .warning
        case .error:
            level = .error
        }
        
        deviceLog.write(level: level, tag: tag, message: message)
    }
    
}
