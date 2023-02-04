import AutoRuProtoModels
import Foundation

import AutoRuBackendLayout

final class ReloadReportCellTests: BaseUnitTest, CarReportCardBlockTest, BackendLayoutOutput {
    var requestingVinResolutionUpdate: Bool = false

    func test_canReload() {
        Step("Обновление отчета, можно перезагрузить") {
            requestingVinResolutionUpdate = false
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_reloading() {
        Step("Обновление отчета, перезагружается") {
            requestingVinResolutionUpdate = true
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_waitTillReload() {
        Step("Обновление отчета, ожидание") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
