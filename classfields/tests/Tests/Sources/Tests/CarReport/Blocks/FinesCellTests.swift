import AutoRuProtoModels
import Foundation

final class FinesCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecords() {
        Step("Штрафы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_hasThree() {
        Step("Штрафы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noRecords() {
        Step("Штрафы (нет)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_noSTSKnown() {
        Step("Штрафы (не знаем СТС)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_isUpdating() {
        Step("Штрафы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_notPaid() {
        Step("Штрафы (есть неоплаченные)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
