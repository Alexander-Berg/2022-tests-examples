import AutoRuProtoModels
import Foundation

final class BrandCertificationCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_emptyStateUpdating() {
        Step("Сертификация производителем (нет записей), хедер обновляется") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecords() {
        Step("Сертификация производителем (нет записей), хедер не обновляется") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_hasRecord() {
        Step("Сертификация производителем 1 запись") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
