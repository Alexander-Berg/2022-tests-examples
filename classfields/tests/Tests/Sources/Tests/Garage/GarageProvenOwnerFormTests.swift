//
//  GarageProvenOwnerFormTests.swift
//  Tests
//
//  Created by Igor Shamrin on 24.08.2021.
//

import AutoRuModels
import Snapshots
@testable import AutoRuGarageForm

final class GarageProvenOwnerFormTests: BaseUnitTest {
    func test_garageProvenOwnerFormUploading() {
        makeSnapshot(status: .uploading)
    }

    func test_garageProvenOwnerFormProven() {
        makeSnapshot(status: .proven)
    }

    func test_garageProvenOwnerFormRejected() {
        makeSnapshot(status: .rejected)
    }

    func test_garageProvenOwnerFormUploadFailed() {
        makeSnapshot(status: .uploadFailed)
    }

    func test_garageProvenOwnerFormVerifying() {
        makeSnapshot(status: .verifying)
    }

    func test_garageProvenOwnerFormUnverified() {
        makeSnapshot(status: .unverified)
    }

    private func makeSnapshot(status: ProvenOwnerStatus, identifier: String = #function) {
        let layout = ProvenOwnerCell(provenOwnerStatus: status,
                                     onVerify: {},
                                     onContactSupport: {},
                                     onRetryUpload: {},
                                     onRetakePhotos: {})
        Snapshot.compareWithSnapshot(layout: layout,
                                     maxWidth: DeviceWidth.iPhone11,
                                     identifier: identifier)
    }
}
