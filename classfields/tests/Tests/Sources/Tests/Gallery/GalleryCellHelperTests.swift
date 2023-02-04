//
//  GalleryCellHelperTests.swift
//  Tests
//
//  Created by Alexander Malnev on 6/8/21.
//

import Foundation
import XCTest
import AutoRuCommonViews
import AutoRuAppearance
import Snapshots
@testable import AutoRuModels
@testable import AutoRuCellHelpers
import AutoRuColorSchema
import AutoRuProgressIndicators

class GalleryCellHelperTests: BaseUnitTest {
    typealias AutoRuInprogressPanoramaInfo = MediaItem.AutoRuInprogressPanoramaInfo

    override func setUp() {
        super.setUp()

        Spinner.disableAnimationForTests = true
    }

    func testAddPanoramaProcessingState() {
        validateSnapshotForPanoramaState(AutoRuInprogressPanoramaInfo(placeholder: nil, state: .inprogress(uploadPercent: nil)))
    }

    func testAddPanoramaProcessingPercentState() {
        validateSnapshotForPanoramaState(AutoRuInprogressPanoramaInfo(placeholder: nil, state: .inprogress(uploadPercent: 33)))
    }

    func testAddPanoramaProcessingErrorState() {
        validateSnapshotForPanoramaState(AutoRuInprogressPanoramaInfo(placeholder: nil, state: .error(message: "Error")))
    }

    private func validateSnapshotForPanoramaState(_ panoramaInfo: AutoRuInprogressPanoramaInfo, id: String = #function) {
        let panoramaItem = MediaItem.panorama(.autoRuInprogress(panoramaInfo))
        let model = galleryCellHelperModelWith(panoramaItem)
        let galleryCellHelper = GalleryCellHelper(model: model)
        galleryCellHelper.precalculateLayout(indexPath: IndexPath(item: 0, section: 0), width: DeviceWidth.iPhone11)
        let view = galleryCellHelper.createCellView(width: DeviceWidth.iPhone11)
        view.backgroundColor = ColorSchema.Background.surface

        Snapshot.compareWithSnapshot(view: view, identifier: id)
    }

    private func galleryCellHelperModelWith(_ items: MediaItem...) -> GalleryCellHelperModel {
        guard let mediaContent = MediaContent(items: items) else {
            XCTFail("Не получилось собрать модель для отображения в лейауте галереи")
            fatalError()
        }
        return GalleryCellHelperModel(
            mediaContent: mediaContent,
            initialIndex: nil,
            layoutModifier: nil,
            onTap: nil,
            galleryContext: nil
        )
    }
}
