//
//  GarageProvenOwnerCellTests.swift
//  Tests
//
//  Created by Igor Shamrin on 24.08.2021.
//
import AutoRuModels
import Snapshots
@testable import AutoRuGarageCard

final class GarageProvenOwnerCellTests: BaseUnitTest {
    func test_garageProvenOwnerCellUploading() {
        makeSnapshot(state: .uploading)
    }

    func test_garageProvenOwnerCellProven() {
        makeSnapshot(state: .proven)
    }

    func test_garageProvenOwnerCellRejected() {
        makeSnapshot(state: .rejected)
    }

    func test_garageProvenOwnerCellUploadFailed() {
        makeSnapshot(state: .uploadFailed)
    }

    func test_garageProvenOwnerCellVerifying() {
        makeSnapshot(state: .verifying)
    }

    func test_garageProvenOwnerCellUnverified() {
        makeSnapshot(state: .unverified)
    }

    private func makeSnapshot(state: ProvenOwnerStatus, identifier: String = #function) {
        let layout = GarageProvenOwnerCell(state: state,
                                           passVerificationBlock: {},
                                           showChatSupportBlock: {},
                                           retryUploadBlock: {},
                                           reshootPhotoBlock: {})
        Snapshot.compareWithSnapshot(layout: layout,
                                     maxWidth: DeviceWidth.iPhone11,
                                     identifier: identifier)
    }
}
