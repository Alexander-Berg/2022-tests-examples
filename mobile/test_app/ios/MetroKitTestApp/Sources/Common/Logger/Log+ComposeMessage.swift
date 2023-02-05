//
//  Log+ComposeMessage.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 29/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension Log {

    public static func compose(level: LogLevel, tag: String?, message: String) -> String {
        var components: [String] = []
        
        components.append("\(getIcon(level: level))")
        components.append("\(getTimeString())")
        
        if let tag = tag {
            components.append("[\(tag)]")
        }
        
        components.append(message)
        
        return components.joined(separator: " ")
    }
    
    // MARK: - Private
    
    private static func getIcon(level: LogLevel) -> String {
        switch level {
        case .debug:
            return "🔨"
        case .info:
            return "💡"
        case .warning:
            return "⚠️"
        case .error:
            return "⛔️"
        }
    }

    private static func getTimeString() -> String {
        let date = Date()
        let calendar = Calendar.current

        let hours = calendar.component(.hour, from: date)
        let minutes = calendar.component(.minute, from: date)
        let seconds = calendar.component(.second, from: date)
        
        return String(format: "%0.2d:%0.2d:%0.2d", hours, minutes, seconds)
    }

}
