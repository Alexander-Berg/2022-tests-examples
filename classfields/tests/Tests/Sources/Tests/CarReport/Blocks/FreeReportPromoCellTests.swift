import AutoRuProtoModels
@testable import AutoRuStandaloneCarHistory
import Foundation

final class FreeReportPromoCellTests: BaseUnitTest, CarReportCardBlockTest {
    static let statuses: [String] = ["OK", "ERROR", "UNKNOWN", "INCOMPLETE", "LOCKED"]

    // TODO: UI test

    func test_blocksReady() {
        for status in Self.statuses {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
        }
    }

    func test_blocksNotReady() {
        for status in Self.statuses {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
        }
    }
}
