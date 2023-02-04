import AutoRuProtoModels
import SwiftProtobuf
import Foundation

import AutoRuStandaloneCarHistory
import AutoRuBackendLayout

final class MileagesGraphCellTests: BaseUnitTest, CarReportCardBlockTest {
    typealias Graph = Auto_Api_Vin_AdditionalReportLayoutData.MileagesGraphData
    typealias Point = Auto_Api_Vin_AdditionalReportLayoutData.MileagesGraphData.MileagePoint

    func test_statusOK() {
        Step("Чарт пробега") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_statusNotOK() {
        Step("Чарт пробега") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}

extension MileagesGraphCellTests: BackendLayoutOutput {
    var additionalData: Auto_Api_Vin_AdditionalReportLayoutData? {
        let graph = Graph.with { graphData in
            graphData.id = "mileages_graph"
            graphData.chartPoints = [
                Point.with { pointData in
                    pointData.mileage = 0
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1371153600))
                },
                Point.with { pointData in
                    pointData.mileage = 9231
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1371153600))
                },
                Point.with { pointData in
                    pointData.mileage = 14705
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1397058115))
                },
                Point.with { pointData in
                    pointData.mileage = 25271
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1417852610))
                },
                Point.with { pointData in
                    pointData.mileage = 25271
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1417868448))
                },
                Point.with { pointData in
                    pointData.mileage = 45041
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1459673205))
                },
                Point.with { pointData in
                    pointData.mileage = 45041
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1466946224))
                },
                Point.with { pointData in
                    pointData.mileage = 56000
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1496591697))
                },
                Point.with { pointData in
                    pointData.mileage = 57000
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1517296998))
                },
                Point.with { pointData in
                    pointData.mileage = 56705
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1521611762))
                },
                Point.with { pointData in
                    pointData.mileage = 123000
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1587640854))
                },
                Point.with { pointData in
                    pointData.mileage = 121311
                    pointData.date = Google_Protobuf_Timestamp(date: Date(timeIntervalSince1970: 1617032696))
                }
            ]
        }

        return Auto_Api_Vin_AdditionalReportLayoutData.with { additionalData in
            additionalData.mileagesGraphData = [graph]
        }
    }
}
