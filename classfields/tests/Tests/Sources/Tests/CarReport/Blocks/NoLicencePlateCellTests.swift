import AutoRuProtoModels
import Foundation

final class NoLicencePlateCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_foreign() {
        Step("Не указан госномер") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_user() {
        Step("Не указан госномер") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
