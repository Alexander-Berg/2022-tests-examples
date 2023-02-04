import AutoRuProtoModels
import Foundation

final class TaxCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Налог") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
