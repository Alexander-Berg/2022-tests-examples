@testable import AutoRuGarageCard
@testable import AutoRuGarageSharedUI
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
import AutoRuAppConfig

final class GarageLandingTests: BaseUnitTest {
    func test_header_cell() {
        Snapshot.compareWithSnapshot(layout: GarageLandingHeaderCellLegacy(), maxWidth: DeviceWidth.iPhone11)
    }

    func test_estimate_cell() {
        Snapshot.compareWithSnapshot(layout: GarageLandingEstimateCell(), maxWidth: DeviceWidth.iPhone11)
    }

    func test_pro_auto_cell() {
        Snapshot.compareWithSnapshot(layout: GarageLandingProAutoCell(), maxWidth: DeviceWidth.iPhone11)
    }

    func test_recalls_cell() {
        Snapshot.compareWithSnapshot(layout: GarageLandingRecallsCell(), maxWidth: DeviceWidth.iPhone11)
    }

    func test_proven_owner_cell() {
        Snapshot.compareWithSnapshot(layout: GarageLandingProvenOwnerCell(), maxWidth: DeviceWidth.iPhone11)
    }

    func test_insurance_cell() {
        Snapshot.compareWithSnapshot(layout: GarageLandingInsuranceCell(), maxWidth: DeviceWidth.iPhone11)
    }

    func test_promo_cell() {
        let model = GarageCardPromo(
            id: "123",
            promoType: .superPromo,
            title: "Title test",
            description: "Subtitle test",
            image: .init(
                image: .testImage(withFixedSize: .init(width: 300, height: 190)),
                imageType: .picture,
                imageAspectRatio: 18/11
            ),
            logo: .init(
                image: .testImage(withFixedSize: .zero),
                imageType: .logo,
                imageAspectRatio: 1
            ),
            color: .blue,
            popUp: nil,
            url: nil,
            openDirectly: false
        )
        Snapshot.compareWithSnapshot(
            layout: GarageLandingPromoCell(model: model, onTap: { _ in }),
            maxWidth: DeviceWidth.iPhone11
        )
    }
}
