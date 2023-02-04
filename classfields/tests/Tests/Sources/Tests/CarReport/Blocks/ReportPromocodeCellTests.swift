import AutoRuProtoModels
import Foundation

final class ReportPromocodeCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Промка скидки на  отчет") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
