import Foundation

public enum LogLevel {
    case error
    case warning
    case debug
    case info
    
    public static var all: [LogLevel] {
        return [.error, .warning, .debug, .info]
    }
}
