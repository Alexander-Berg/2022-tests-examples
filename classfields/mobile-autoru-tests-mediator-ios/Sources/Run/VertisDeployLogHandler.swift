import Foundation
import Vapor

#if os(Linux)
import Glibc
#else
import Darwin.C
#endif

private struct LogRecord: Encodable {
    let _time: Date
    let _level: String
    let _message: String
}

struct VertisDeployLogHandler: LogHandler {
    var metadata = Logger.Metadata()
    var logLevel: Logger.Level = .info

    let writeQueue = DispatchQueue.global()

    private static let rfc3339Formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        return formatter
    }()

    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(rfc3339Formatter)
        return encoder
    }()

    subscript(metadataKey metadataKey: String) -> Logger.Metadata.Value? {
        get {
            return self.metadata[metadataKey]
        }
        set {
            self.metadata[metadataKey] = newValue
        }
    }

    func log(
        level: Logger.Level,
        message: Logger.Message,
        metadata: Logger.Metadata?,
        source: String,
        file: String,
        function: String,
        line: UInt
    ) {
        writeQueue.sync {
            let text = message.description.replacingOccurrences(of: "\n", with: " ")

            let record = LogRecord(_time: Date(), _level: Self.mapLogLevel(level), _message: text)

            defer { fflush(stdout) }

            guard let jsonData = try? Self.encoder.encode(record),
                  let json = String(data: jsonData, encoding: .utf8) else {
                print("Unable to log message: \(record)")
                return
            }

            print(json)
        }
    }

    private static func mapLogLevel(_ level: Logger.Level) -> String {
        switch level {
        case .trace: return "TRACE"
        case .debug: return "DEBUG"
        case .info, .notice: return "INFO"
        case .warning: return "WARN"
        case .error: return "ERROR"
        case .critical: return "FATAL"
        }
    }
}
