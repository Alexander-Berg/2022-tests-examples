import AutoRuProtoModels
import Foundation

final class TaxiCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecord() {
        Step("Работа в такси") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_empty() {
        Step("Работа в такси (пустой)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_emptyUpdating() {
        Step("Работа в такси (пустой, обновляется)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
