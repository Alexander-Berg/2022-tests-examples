import AutoRuProtoModels
@testable import AutoRuStandaloneCarHistory
import Foundation

final class UserRatingCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Оценка отчета") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
