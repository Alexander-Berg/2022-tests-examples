import AutoRuProtoModels
import Foundation
import AutoRuStandaloneCarHistory

import AutoRuBackendLayout

final class PriceStatsGraphCellTests: BaseUnitTest, CarReportCardBlockTest {
    func test_cell() {
        Step("Чарт цен") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}

extension PriceStatsGraphCellTests: BackendLayoutOutput {
    typealias Graph = Auto_Api_Vin_AdditionalReportLayoutData.PriceStatsGraphData
    typealias Segment = Auto_Api_Vin_AdditionalReportLayoutData.PriceStatsGraphData.PriceSegment

    var additionalData: Auto_Api_Vin_AdditionalReportLayoutData? {
        let graph = Graph.with { graphData in
            graphData.id = "price_predict"
            graphData.predictedPrice = 2604500
            graphData.histogram = [
                    Segment.with { segmentData in
                        segmentData.priceFrom = 1800000
                        segmentData.priceTo = 1894444
                        segmentData.count = 1
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 1894444
                        segmentData.priceTo = 1988888
                        segmentData.count = 0
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 1988888
                        segmentData.priceTo = 2083332
                        segmentData.count = 0
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 2083332
                        segmentData.priceTo = 2177776
                        segmentData.count = 0
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 2177776
                        segmentData.priceTo = 2272220
                        segmentData.count = 1
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 2272220
                        segmentData.priceTo = 2366664
                        segmentData.count = 1
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 2366664
                        segmentData.priceTo = 2461108
                        segmentData.count = 0
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 2461108
                        segmentData.priceTo = 2555552
                        segmentData.count = 0
                    },
                    Segment.with { segmentData in
                        segmentData.priceFrom = 2555552
                        segmentData.priceTo = 2650000
                        segmentData.count = 1
                    }
            ]
            graphData.showSegments = [
                Segment.with { segmentData in
                    segmentData.priceFrom = 1800000
                    segmentData.priceTo = 2083332
                    segmentData.count = 1
                },
                Segment.with { segmentData in
                    segmentData.priceFrom = 2083332
                    segmentData.priceTo = 2366664
                    segmentData.count = 2
                },
                Segment.with { segmentData in
                    segmentData.priceFrom = 2366664
                    segmentData.priceTo = 2650000
                    segmentData.count = 1
                }
            ]
        }

        return Auto_Api_Vin_AdditionalReportLayoutData.with { additionalData in
            additionalData.priceStatsData = [graph]
        }
    }
}
