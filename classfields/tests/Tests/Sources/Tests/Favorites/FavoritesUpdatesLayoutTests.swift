import XCTest
import AutoRuAppearance
import Snapshots
@testable import AutoRuFavoriteSaleList
import AutoRuColorSchema

final class FavoritesUpdatesLayoutTests: BaseUnitTest {
    func test_layoutAppearance() {
        let model = FavoritesUpdates(soldCount: 5, changedPriceCount: 2)
        let layout = FavoritesUpdatesLayout.makeLayout(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }
}
