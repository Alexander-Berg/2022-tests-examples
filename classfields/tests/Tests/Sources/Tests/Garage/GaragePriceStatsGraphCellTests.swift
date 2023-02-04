import AutoRuProtoModels
import XCTest
import Snapshots
@testable import AutoRuGarageCard

final class GaragePriceStatsGraphCellModernLayoutTests: BaseUnitTest {
    func test_GaragePriceStatsGraphCell() throws {
        let priceStats = try Auto_Api_Vin_Garage_PriceStats(jsonString: Self.priceStatsJsonMock)

        let layout = GaragePriceStatsGraphCell(
            priceTitle: "Test price title",
            estimatedPrice: 1_000_000,
            tradeInPrice: 1_000_000,
            graphData: priceStats.priceDistribution,
            displayListingLink: true,
            displayTradeIn: true,
            onAllOffersTap: {}
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_GaragePriceStatsGraphCell_withoutListing() throws {
        let priceStats = try Auto_Api_Vin_Garage_PriceStats(jsonString: Self.priceStatsJsonMock)

        let layout = GaragePriceStatsGraphCell(
            priceTitle: "Test price title",
            estimatedPrice: 1_000_000,
            tradeInPrice: 1_000_000,
            graphData: priceStats.priceDistribution,
            displayListingLink: false,
            displayTradeIn: true
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_GaragePriceStatsGraphCell_withoutSegments() throws {
        var priceStats = try Auto_Api_Vin_Garage_PriceStats(jsonString: Self.priceStatsJsonMock)
        priceStats.priceDistribution.showSegments = []

        let layout = GaragePriceStatsGraphCell(
            priceTitle: "Test price title",
            estimatedPrice: 1_000_000,
            tradeInPrice: 1_000_000,
            graphData: priceStats.priceDistribution,
            displayListingLink: false,
            displayTradeIn: true
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_GaragePriceStatsGraphCell_priceOnly() throws {
        var priceStats = try Auto_Api_Vin_Garage_PriceStats(jsonString: Self.priceStatsJsonMock)
        priceStats.priceDistribution.showSegments = []

        let layout = GaragePriceStatsGraphCell(
            priceTitle: "Test price title",
            estimatedPrice: 1_000_000,
            tradeInPrice: 1_000_000,
            graphData: priceStats.priceDistribution,
            displayListingLink: false,
            displayTradeIn: false
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_garagePriceStatsGraphCellTappableSegments() throws {
        let priceStats = try Auto_Api_Vin_Garage_PriceStats(jsonString: Self.priceStatsJsonMock)

        let layout = GaragePriceStatsGraphCell(
            priceTitle: "Test price title",
            estimatedPrice: 1_000_000,
            tradeInPrice: 1_000_000,
            graphData: priceStats.priceDistribution,
            displayListingLink: false,
            displayTradeIn: false,
            onSegmentTap: { _ in }
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}

private extension GaragePriceStatsGraphCellModernLayoutTests {
    static let priceStatsJsonMock =
    """
    {
        "priceDistribution": {
        "histogram": [
            {
            "priceFrom": "490000",
            "priceTo": "518888",
            "count": "2"
            },
            {
            "priceFrom": "518888",
            "priceTo": "547776",
            "count": "0"
            },
            {
            "priceFrom": "547776",
            "priceTo": "576664",
            "count": "2"
            },
            {
            "priceFrom": "576664",
            "priceTo": "605552",
            "count": "1"
            },
            {
            "priceFrom": "605552",
            "priceTo": "634440",
            "count": "3"
            },
            {
            "priceFrom": "634440",
            "priceTo": "663328",
            "count": "2"
            },
            {
            "priceFrom": "663328",
            "priceTo": "692216",
            "count": "0"
            },
            {
            "priceFrom": "692216",
            "priceTo": "721104",
            "count": "2"
            },
            {
            "priceFrom": "721104",
            "priceTo": "750000",
            "count": "1"
            }
        ],
        "showSegments": [
            {
            "priceFrom": "490000",
            "priceTo": "576664",
            "count": "4"
            },
            {
            "priceFrom": "576664",
            "priceTo": "663328",
            "count": "6"
            },
            {
            "priceFrom": "663328",
            "priceTo": "750000",
            "count": "3"
            }
        ],
        "predictedPrice": 649500
        },
        "cheapening": {
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
    }
    """
}
