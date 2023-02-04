//
//  GarageFormTests.swift
//  Tests
//
//  Created by Aleksey Gotyanov on 1/26/21.
//

import XCTest
import Snapshots
@testable import AutoRuGarageForm

final class GarageFormTests: BaseUnitTest {
    func testAddExtraCarInfoLayout() {
        let layout = AddExtraCarInfoLayout()
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 414)
    }
}
