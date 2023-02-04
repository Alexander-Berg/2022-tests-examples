import AutoRuModels
import AutoRuUtils
@testable import AutoRuPhotoGallery
import AutoRuFetchableImage
import AutoRuProtoModels
import XCTest
import Snapshots

final class FullscreenGalleryCustomSlidesTests: BaseUnitTest {
    func test_bestPriceSlide() {
        let view = NewCarRequestPreview()

        view.display(
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 100)),
            title: "BMW X5",
            openBestPriceRequestForm: { }
        )

        view.frame = CGRect(x: 0, y: 0, width: DeviceWidth.iPhone11, height: DeviceHeight.iPhone11)

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_reportSlide() {
        let view = ReportPreview()

        let model: Auto_Api_RawVinReportResponse = .init(mockFile: "carfax_free_report")

        view.display(
            report: model,
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 100)),
            openReport: { }
        )

        view.frame = CGRect(x: 0, y: 0, width: DeviceWidth.iPhone11, height: DeviceHeight.iPhone11)

        Snapshot.compareWithSnapshot(view: view)
    }
}
