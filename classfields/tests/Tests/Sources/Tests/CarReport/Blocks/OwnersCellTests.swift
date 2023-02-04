import AutoRuProtoModels
import Foundation

final class OwnersCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecords() {
        Step("Владельцы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_hasRecordsWithComment() {
        Step("Владельцы (с комментарием)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecords() {
        Step("Владельцы (нет)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecordsUpdating() {
        Step("Владельцы (нет)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_moreInfo() {
        Step("Владельцы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
