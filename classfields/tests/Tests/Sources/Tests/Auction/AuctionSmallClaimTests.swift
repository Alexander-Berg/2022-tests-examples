import XCTest
import AutoRuProtoModels
import AutoRuUtils
import AutoRuNavigationContainer
import AutoRuColorSchema
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuAuctionSmallClaim

final class AuctionSmallClaimTests: BaseUnitTest {
    func test_claimAppearance() {
        let model = ClaimInfoModel(priceRange: "1 000 000 – 2 000 000 ₽")
        let layout = ClaimInfoLayout(model: model, onSubmitTap: { }, onMoreTap: { }, onCloseTap: { })

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }
}
