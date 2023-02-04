import AutoRuProtoModels
import Foundation

final class TotalAuctionCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_hasRecord() {
        Step("Аукцион битых") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_empty() {
        Step("Аукцион битых (пустой)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_emptyUpdating() {
        Step("Аукцион битых (пустой, обновляется)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
