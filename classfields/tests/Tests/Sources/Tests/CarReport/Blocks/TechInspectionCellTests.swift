import AutoRuProtoModels
import Foundation

final class TechInspectionCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecord() {
        Step("Техосмотры") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_empty() {
        Step("Техосмотры (пустой)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_emptyUpdating() {
        Step("Техосмотры (пустой, обновляется)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
