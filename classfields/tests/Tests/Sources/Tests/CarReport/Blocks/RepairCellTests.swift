import AutoRuProtoModels
import Foundation

final class RepairCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_repairs() {
        Step("Ремонт") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_notReady() {
        Step("Ремонт (блок не готов)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_repairsEmpty() {
        Step("Ремонт (пустой)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_repairsEmptyNotReady() {
        Step("Ремонт (пустой, не готов)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_repairsEmptyUpdating() {
        Step("Ремонт (пустой, обновляется)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_lockedItem() {
        Step("Ремонт (locked)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
