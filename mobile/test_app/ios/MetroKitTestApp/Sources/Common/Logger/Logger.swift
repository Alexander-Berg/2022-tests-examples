//
//  Logger.swift
//  YandexMetro
//
//  Created by Ilya Lobanov on 29/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public protocol LogWriter: class {
    func write(level: LogLevel, tag: String?, message: String)
}

public protocol LogFilter: class {
    func shouldWrite(level: LogLevel, tag: String?) -> Bool
}

public protocol Logger: class {
    var filter: LogFilter? { get set }
    var writer: LogWriter? { get set }
    
    func log(level: LogLevel, tag: String?, message: String)
}

public extension Logger {
    
    func log(level: LogLevel, message: String) {
        log(level: level, tag: nil, message: message)
    }
    
}

