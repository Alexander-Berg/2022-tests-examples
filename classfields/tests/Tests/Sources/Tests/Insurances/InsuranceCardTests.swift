import XCTest
import Snapshots
import AutoRuFetchableImage
import AutoRuProtoModels
import SwiftProtobuf
@testable import AutoRuInsurance

class InsuranceCardTests: BaseUnitTest {
    func test_uploadButtonNoAttachment() {
        let layout = UploadPolicyButton(attachmentStatus: .noAttachment, onUpload: {}, onRetryUpload: {}, onOpenPdf: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_uploadButtonUploading() {
        let layout = UploadPolicyButton(attachmentStatus: .uploading, onUpload: {}, onRetryUpload: {}, onOpenPdf: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_uploadButtonUploadError() {
        let layout = UploadPolicyButton(attachmentStatus: .uploadError, onUpload: {}, onRetryUpload: {}, onOpenPdf: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_uploadButtonHasAttachment() {
        let layout = UploadPolicyButton(attachmentStatus: .hasAttachment, onUpload: {}, onRetryUpload: {}, onOpenPdf: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_photoPreview() {
        let layout = PhotoPreview(image: FetchableImage(image: .namedOrEmpty("safe-deal-docs")), aspectRatio: 1.333, onAspectRatioChanged: { _ in }, onRetakePhoto: {}, onRemovePhoto: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_insuranceCard() {
        let fields: [(InsuranceCardViewController.Fields, String)] = [
            (.insuranceCompanyName, "РОСГОССТРАХ"),
            (.accidentNumber, "+7 (911) 123-45-67"),
            (.serialAndNumber, "XXX 1234567890"),
            (.validFrom, "01.01.2022"),
            (.validTill, "01.01.2023")
        ]

        let layout = InsuranceCardViewController.Layout(filledFields: fields)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
