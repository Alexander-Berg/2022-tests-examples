import XCTest
import Snapshots
@testable import AutoRuGarageForm

final class GarageVinSearchResultCellTests: BaseUnitTest {

    func test_cell() throws {
        let layout = GarageSearchResultCell(
            viewModel: .init(
                mark: "Марка",
                model: "Модель",
                color: "Черный",
                engine: "1.6л, 100 л.с.",
                allowChangeRegion: true,
                region: "Москва",
                vin: "XW8ZZZ1KZBG000050",
                image: .testImage(withFixedSize: .init(width: 64, height: 64)),
                isAdded: false
            ),
            onRegionTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_cell_without_region() throws {
        let layout = GarageSearchResultCell(
            viewModel: .init(
                mark: "Марка",
                model: "Модель",
                color: "Черный",
                engine: "1.6л, 100 л.с.",
                allowChangeRegion: false,
                region: "Москва",
                vin: "XW8ZZZ1KZBG000050",
                image: .testImage(withFixedSize: .init(width: 64, height: 64)),
                isAdded: false
            ),
            onRegionTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
