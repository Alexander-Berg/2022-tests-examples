import AutoRuColorSchema
import Foundation
import Snapshots
import XCTest
@testable import AutoRuCarTinderPromo

final class CarTinderBannerTest: BaseUnitTest {
    func testCarTinderBanner() {
        let layout = CarTinderBanner(model: .initial).getLayout()
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "user_sale_list_car_tinder_banner"
        )
    }
}
