import Foundation

public protocol Logger {
    func info(_ string: String)
    func error(_ string: String)
}
