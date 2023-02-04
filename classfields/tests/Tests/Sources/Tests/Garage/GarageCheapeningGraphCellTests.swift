import AutoRuProtoModels
import XCTest
import Snapshots
@testable import AutoRuGarageCard

final class GarageCheapeningGraphCellModernLayoutTests: BaseUnitTest {
    func test_GarageCheapeningGraphCell() throws {
        let graphData: Auto_Api_Vin_AdditionalReportLayoutData.CheapeningGraphData = try .init(jsonString: Self.graphDataJsonMock)

        let layout = GarageCheapeningGraphCell(graphData: graphData)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}

private extension GarageCheapeningGraphCellModernLayoutTests {
    static let graphDataJsonMock =
    """
        {
            "chartPoints": [
            {
                "age": 9,
                "price": 518743,
                "pricePercentageDiff": 0
            },
            {
                "age": 10,
                "price": 501318,
                "pricePercentageDiff": -3
            }
            ],
            "avgAnnualDiscountPercent": -3
        }
    """
}
