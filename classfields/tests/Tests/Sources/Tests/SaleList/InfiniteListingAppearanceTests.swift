import XCTest
import YandexMobileAds
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import AutoRuTableController
import Snapshots
@testable import AutoRuCellHelpers
@testable import AutoRuYogaLayout
@testable import AutoRuSaleList

final class InfiniteListingAppearanceTests: BaseUnitTest {
    func test_geoRadiusBubbles() {
        let model = GeoRadiusBubblesModel(
            geoRegionTitle: "Москва",
            radiuses: [
                .init(type: .currentRegion, offerCount: 10, isActive: false),
                .init(type: .withRadius(100), offerCount: 20, isActive: true),
                .init(type: .wholeRussia, offerCount: 30, isActive: false)
            ],
            action: { _ in },
            initialContentOffset: nil,
            onDidScroll: { _ in },
            getContentOffset: { nil },
            scrollToActiveRadius: { false }
        )
        let cellHelper = GeoRadiusBubblesCellHelper(model: model)

        Snapshot.compareWithSnapshot(cellHelper: cellHelper)
    }
}
