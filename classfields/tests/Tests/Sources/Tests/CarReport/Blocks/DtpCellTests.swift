import AutoRuProtoModels
import Foundation

final class DtpCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_emptyStateUpdating() {
        Step("ДТП, нет записей, хедер обновляется") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_emptyState() {
        Step("ДПТ, нет записей, хедер не обновляется") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_hasRecord() {
        Step("ДПТ, есть запись") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
