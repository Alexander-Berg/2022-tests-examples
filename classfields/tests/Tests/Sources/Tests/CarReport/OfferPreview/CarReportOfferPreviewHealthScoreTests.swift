import AutoRuAppearance
import AutoRuProtoModels
import SwiftProtobuf
import XCTest
@testable import AutoRuStandaloneCarHistory

/// Health скор в превью оффера
final class CarReportOfferPreviewHealthScoreTests: BaseUnitTest, CarReportCardBlockTest {

    func test_bought_healthScore_ok() {
        Step("Проверяем купленное превью оффера: есть скор") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_bought_healthScore_updating() {
        Step("Проверяем купленное превью оффера: скор есть, но не готов") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_free_healthScore_noContent() {
        Step("Проверяем некупленное превью оффера: скора нет, так как не пришел HEALTH_SCORE в content") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_free_healthScore_locked() {
        Step("Проверяем некупленное превью оффера: есть скор в content") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
