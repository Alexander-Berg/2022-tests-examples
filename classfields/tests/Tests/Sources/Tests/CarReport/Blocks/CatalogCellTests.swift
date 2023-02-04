import AutoRuProtoModels
import AutoRuStandaloneCarHistory
import Foundation

import AutoRuBackendLayout

final class CatalogCellTests: BaseUnitTest, CarReportCardBlockTest {
    private var characteristicsData: CarReportCharacteristics!

    func test_collapsed() {
        Step("Каталог. Свёрнут") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_expanded() {
        Step("Каталог. Развёрнут") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
