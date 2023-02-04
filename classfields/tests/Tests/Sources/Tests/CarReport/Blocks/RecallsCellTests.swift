import AutoRuProtoModels
import Foundation

final class RecallsCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_noRecalls() {
        Step("Отзывные") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecallsUpdating() {
        Step("Отзывные (обновляется)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_hasRecallsRecord() {
        Step("Отзывные (есть)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
