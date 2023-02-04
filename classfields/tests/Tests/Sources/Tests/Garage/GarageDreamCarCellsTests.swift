//
//  GarageDreamCarCellsTests.swift
//  
//
//  Created by Igor Shamrin on 13.01.2022.
//

import XCTest
import Snapshots
@testable import AutoRuGarageCard

final class GarageDreamCarCellsTests: BaseUnitTest {
    func test_proAvtoPromoCell() {
        let layout = PromoProAvtoCell(actionBlock: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_creditCell() {
        let layout = GarageCreditCell(actionBlock: {}, creditApplicationExist: false, isSberExclusive: false)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_offersCell() {
        let layout = OfferItemCell(
            model: .init(
                id: "123",
                image: .testImage(withFixedSize: .init(width: 167, height: 125)),
                price: "10 000 000",
                title: "Test title",
                subtitle: "Test subtitle"
            ),
            onTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
