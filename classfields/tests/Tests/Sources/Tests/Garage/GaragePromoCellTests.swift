//
//  GaragePromoCellTests.swift
//  Tests
//
//  Created by Igor Shamrin on 15.10.2021.
//

@testable import AutoRuGarageCard
@testable import AutoRuGarageSharedUI
import Foundation
import AutoRuProtoModels
import AutoRuUtils
import AutoRuFetchableImage
import AutoRuCellHelpers
import AutoRuTableController
import XCTest
import Snapshots

final class GaragePromoCellTests: BaseUnitTest {
    func test_PromoItemWithLogo() throws {
        let layout = GaragePromoItemCell(
            model: makeItemModelStub(imageType: .logo),
            onTap: { _ in }
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    private func makeItemModelStub(
        imageType: Auto_Api_Vin_Garage_PartnerPromo.Picture.PictureType
    ) -> GarageCardPromo {
        return GarageCardPromo(
            id: "test",
            promoType: .commonPromo,
            title: "Тест промо",
            description: "Тест промо",
            image: .init(image: FetchableImage.testImage(withFixedSize: .init(width: 200, height: 140)),
                         imageType: .picture,
                         imageAspectRatio: 200.0/140),
            logo: .init(image: FetchableImage.testImage(withFixedSize: .init(width: 128, height: 16)),
                        imageType: .logo,
                        imageAspectRatio: 128.0/16),
            color: .purple,
            popUp: nil,
            url: nil,
            openDirectly: false
        )
    }
}
