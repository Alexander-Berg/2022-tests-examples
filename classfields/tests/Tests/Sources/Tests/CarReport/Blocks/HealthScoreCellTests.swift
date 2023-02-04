import AutoRuAppearance
import AutoRuProtoModels
import SwiftProtobuf
import XCTest
@testable import AutoRuStandaloneCarHistory

final class HealthScoreCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_free_healthScore() {
        Step("Проверяем блок скора в бесплатном отчете") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_bought_healthScore_loading() {
        Step("Проверяем блок скора в полном отчете, скор ещё не подсчитан") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_bought_healthScore_value() {
        Step("Проверяем блок скора в полном отчете, только значение, без диапазона") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_bought_healthScore_range() {
        Step("Проверяем блок скора в полном отчете, значение и диапазон") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
