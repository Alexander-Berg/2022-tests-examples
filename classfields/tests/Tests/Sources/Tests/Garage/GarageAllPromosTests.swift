import AutoRuProtoModels
import Snapshots
@testable import AutoRuGarageAllPromos
import CoreGraphics

final class GarageAllPromosTests: BaseUnitTest {
    func test_bigPromoCell() {
        let imageSize = CGSize(width: 343, height: 250)
        let logoSize = CGSize(width: 100, height: 16)
        let layout = BigPromoCell(
            bigPromo: .init(
                picture: .init(image: .testImage(withFixedSize: imageSize), aspectRatio: 4.0/3),
                title: "Test title",
                description: "Test description",
                logo: .init(image: .testImage(withFixedSize: logoSize), aspectRatio: 16.0/100),
                color: .green
            ),
            onTap: {}
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_commonPromoCell() {
        let imageSize = CGSize(squareSize: 81)
        let logoSize = CGSize(width: 100, height: 16)
        let layout = CommonPromoCell(
            commonPromo: .init(
                picture: .init(image: .testImage(withFixedSize: imageSize), aspectRatio: 4.0/3),
                title: "Test title",
                logo: .init(image: .testImage(withFixedSize: logoSize), aspectRatio: 16.0/100),
                color: .green
            ),
            onTap: {}
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

}
