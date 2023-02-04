import AutoRuProtoModels
import Foundation

final class InsuranceCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecords() {
        Step("Страховки") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecords() {
        Step("Страховки (нет)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
