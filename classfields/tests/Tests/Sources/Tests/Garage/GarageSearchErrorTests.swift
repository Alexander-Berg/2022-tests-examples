//
//  GarageSearchErrorTests.swift
//  
//
//  Created by Igor Shamrin on 21.01.2022.
//

import XCTest
import Snapshots
@testable import AutoRuGarageForm

final class GarageSearchErrorTests: BaseUnitTest {
    func test_Vin_search_error_try_later() {
        let layout = ErrorIllustrationLayout(error: .vinNotFoundTryLater)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_Vin_search_error_add_manually() {
        let layout = ErrorIllustrationLayout(error: .vinNotFoundAddManually)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_Vin_search_error_no_internet() {
        let layout = ErrorIllustrationLayout(error: .noInternet)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_Vin_search_error_server_error() {
        let layout = ErrorIllustrationLayout(error: .serverError)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
