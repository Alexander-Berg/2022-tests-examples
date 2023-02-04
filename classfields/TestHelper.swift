import Foundation

public enum TestHelper {
    public static var isRunningTest: Bool {
        #if DEBUG
        return isRunningUnitTest || isRunningUITest
        #else
        return false
        #endif
    }

    public static var isRunningUnitTest: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.environment["XCTestSessionIdentifier"] ?? "" != ""
        #else
        return false
        #endif
    }

    public static var isRunningUITest: Bool {
        #if DEBUG
        return CommandLine.arguments.contains("--UITests")
        #else
        return false
        #endif
    }
}
