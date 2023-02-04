import AutoRuProtoModels
import Foundation
import AutoRuBackendLayout

final class CheapeningGraphTests: BaseUnitTest, CarReportCardBlockTest {
    typealias AdditionalData = Auto_Api_Vin_AdditionalReportLayoutData
    typealias GraphData = Auto_Api_Vin_AdditionalReportLayoutData.CheapeningGraphData
    typealias ChartPoint = GraphData.PriceWithAge

    var graphData: GraphData?

    func test_cheap_6Points() {
        Step("Дешевеет, 6 точек") {
            graphData = GraphData.with { (model: inout GraphData) in
                model.id = "reduction_price_graph"
                model.avgAnnualDiscountPercent = -10
                model.chartPoints = [
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 1_200_000
                        model.age = 2
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 1_000_000
                        model.age = 3
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 980_000
                        model.age = 5
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 750_000
                        model.age = 6
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 700_000
                        model.age = 7
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 500_000
                        model.age = 10
                    }
                ]
            }
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_cheap_3Points() {
        Step("Дешевеет, 3 точки") {
            graphData = GraphData.with { (model: inout GraphData) in
                model.id = "reduction_price_graph"
                model.avgAnnualDiscountPercent = -10
                model.chartPoints = [
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 1_200_000
                        model.age = 2
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 1_000_000
                        model.age = 3
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 980_000
                        model.age = 5
                    }
                ]
            }
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_rise_6Points() {
        Step("Дорожает, 6 точек") {
            graphData = GraphData.with { (model: inout GraphData) in
                model.id = "reduction_price_graph"
                model.avgAnnualDiscountPercent = 10
                model.chartPoints = [
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 500_000
                        model.age = 2
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 700_000
                        model.age = 3
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 750_000
                        model.age = 5
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 980_000
                        model.age = 6
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 1_000_000
                        model.age = 7
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 1_200_000
                        model.age = 10
                    }
                ]
            }
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_rise_3Points() {
        Step("Дорожает, 3 точки") {
            graphData = GraphData.with { (model: inout GraphData) in
                model.id = "reduction_price_graph"
                model.avgAnnualDiscountPercent = 10
                model.chartPoints = [
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 500_000
                        model.age = 2
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 700_000
                        model.age = 3
                    },
                    ChartPoint.with { (model: inout ChartPoint) in
                        model.price = 750_000
                        model.age = 5
                    }
                ]
            }
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}

extension CheapeningGraphTests: BackendLayoutOutput {
    var additionalData: Auto_Api_Vin_AdditionalReportLayoutData? {
        guard let graphData = graphData else {
            return nil
        }
        return Auto_Api_Vin_AdditionalReportLayoutData.with { additionalData in
            additionalData.cheapeningGraphData = [graphData]
        }
    }
}
