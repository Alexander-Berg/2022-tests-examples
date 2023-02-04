import XCTest
import AutoRuElectroCars
import Snapshots
import AutoRuColorSchema

final class ElectrocarsBannerTests: BaseUnitTest {
    func test_electrocarsBannerListing() {
        let layout = ElectroCarsListingBannerLayout(onTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.background,
            identifier: "electrocars_banner_listing"
        )
    }

    func test_electrocarsBannerReview() {
        let layout = ElectroCarsReviewLayout(onTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.background,
            identifier: "electrocars_banner_review"
        )
    }
}
