//
//  GarageCardTests.swift
//  Tests
//
//  Created by Alexander Malnev on 6/8/21.
//

import Foundation
import AutoRuModernLayout
import Snapshots
@testable import AutoRuGarageCard

final class GarageCardTests: BaseUnitTest {
    func testAddPanoramaButton() {
        let layout = PhotoOverlayLayout(
            showAddPhotoButton: true,
            showPanoramaButton: true,
            onPhotoTap: {},
            onPanoramaTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func testNoPanoramaButtonIfHasPanorama() {
        let layout = PhotoOverlayLayout(
            showAddPhotoButton: true,
            showPanoramaButton: false,
            onPhotoTap: {},
            onPanoramaTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
