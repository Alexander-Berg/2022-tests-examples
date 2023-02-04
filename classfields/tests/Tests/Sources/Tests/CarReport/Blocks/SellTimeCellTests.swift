import AutoRuProtoModels
import Foundation

final class SellTimeCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Время продажи") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
