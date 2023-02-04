import AutoRuProtoModels
import Foundation

final class OffersCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecords() {
        Step("Размещения на Авто.Ру") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecords() {
        Step("Размещения на Авто.Ру (нет)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecordsUpdating() {
        Step("Размещения на Авто.Ру (нет)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
