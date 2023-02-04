import AutoRuProtoModels
import Foundation

final class CarsharingCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_mayBeUsed() {
        Step("Автомобиль мог использоваться в каршеринге") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_wasNotUsed() {
        Step("Автомобиль не регистрировался для работы в каршеринге") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_wasUsed() {
        Step("Использовался") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_isUsed() {
        Step("Используется") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
