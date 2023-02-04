import AutoRuProtoModels
import Foundation

final class PtsCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_ptsOkWithComment() {
        Step("ПТС") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_ptsProblemWithComment() {
        Step("ПТС") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_moreInfo() {
        Step("ПТС") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
