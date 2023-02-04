//
//  GarageAddCarTests.swift
//  Tests
//
//  Created by Igor Shamrin on 17.11.2021.
//

import XCTest
import Snapshots
@testable import AutoRuGarageSharedUI

final class GarageAddCarTests: BaseUnitTest {

    func test_addMyCarCellEmpty() {
        let layout = MyCarCell(
            govNumber: nil,
            showContinueButton: false,
            addByVin: {},
            onStateChanged: { _ in },
            onContinue: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_addMyCarCellWithGovNum() {
        let layout = MyCarCell(
            govNumber: .init(number: "а777аа", region: "777"),
            showContinueButton: true,
            addByVin: {},
            onStateChanged: { _ in },
            onContinue: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_addMyCarCellTaxi() {
        let layout = MyCarCell(
            govNumber: .init(number: "аa777а", region: "777"),
            showContinueButton: true,
            addByVin: {},
            onStateChanged: { _ in },
            onContinue: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_addDreamCarCell() {
        let layout = DreamCarCell {}
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_addExCarCell() {
        let layout = ExCarCell {}
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
