import Foundation

extension DeviceLog: LogWriter {
    
    public func write(level: LogLevel, tag: String?, message: String) {
        report(level: level, scope: tag ?? "", message: message)
    }

}
