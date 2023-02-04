import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRu AutoRuFilters
final class FiltersCounterTests: BaseTest {
    private lazy var steps = MainSteps(context: self)

    private static let moscowRegionId = 1
    private static let spbRegionId = 10174

    override func setUp() {
        super.setUp()

        setupServer()
        launch()
    }

    func test_singleRegion_nonZeroFiltered_nonZeroTotal() {
        mocker.mock_searchOfferLocatorCounters(
            type: .cars,
            distances: [
                .init(radius: 200, count: 500),
                .init(radius: 1000, count: 100500)
            ]
        )

        openFilters()
            .tap(.geo)
            .should(provider: .geoRegionPicker, .exist)
            .focus { picker in
                picker
                    .focus(on: .expandableRegion(Self.moscowRegionId), ofType: .geoRegionPickerCell) { cell in
                        cell.tap(.checkbox)
                    }
                    .tap(.doneButton)
            }
            .wait(for: 2)
            .focus(on: .searchButton, ofType: .stylizedButton) { button in
                button
                    .should(.title, .match("Показать 500 предложений"))
                    .should(.subtitle, .match("и ещё 100 000 в других городах"))
            }
    }

    func test_singleRegion_zeroFiltered_nonZeroTotal() {
        mocker.mock_searchOfferLocatorCounters(
            type: .cars,
            distances: [
                .init(radius: 200, count: 0),
                .init(radius: 1000, count: 100500)
            ]
        )

        openFilters()
            .tap(.geo)
            .should(provider: .geoRegionPicker, .exist)
            .focus { picker in
                picker
                    .focus(on: .expandableRegion(Self.moscowRegionId), ofType: .geoRegionPickerCell) { cell in
                        cell.tap(.checkbox)
                    }
                    .tap(.doneButton)
            }
            .wait(for: 2)
            .focus(on: .searchButton, ofType: .stylizedButton) { button in
                button
                    .should(.title, .match("Ничего не найдено"))
                    .should(.subtitle, .match("и ещё 100 500 в других городах"))
            }
    }

    func test_singleRegion_zeroFiltered_zeroTotal() {
        mocker.mock_searchOfferLocatorCounters(
            type: .cars,
            distances: [
                .init(radius: 200, count: 0),
                .init(radius: 1000, count: 0)
            ]
        )

        openFilters()
            .tap(.geo)
            .should(provider: .geoRegionPicker, .exist)
            .focus { picker in
                picker
                    .focus(on: .expandableRegion(Self.moscowRegionId), ofType: .geoRegionPickerCell) { cell in
                        cell.tap(.checkbox)
                    }
                    .tap(.doneButton)
            }
            .wait(for: 2)
            .focus(on: .searchButton, ofType: .stylizedButton) { button in
                button
                    .should(.title, .match("Ничего не найдено"))
                    .should(.subtitle, .be(.hidden))
            }
    }

    func test_multipleRegions() {
        mocker
            .mock_searchOfferLocatorCounters(type: .cars, distances: [])
            .mock_searchCount(count: 123)

        openFilters()
            .tap(.geo)
            .should(provider: .geoRegionPicker, .exist)
            .focus { picker in
                picker
                    .focus(on: .expandableRegion(Self.moscowRegionId), ofType: .geoRegionPickerCell) { cell in
                        cell.tap(.checkbox)
                    }
                    .focus(on: .expandableRegion(Self.spbRegionId), ofType: .geoRegionPickerCell) { cell in
                        cell.tap(.checkbox)
                    }
                    .tap(.doneButton)
            }
            .wait(for: 2)
            .focus(on: .searchButton, ofType: .stylizedButton) { button in
                button
                    .should(.title, .match("Показать 123 предложения"))
                    .should(.subtitle, .be(.hidden))
            }
    }

    private func openFilters() -> FiltersSteps {
        steps.openFilters()
    }

    private func setupServer() {
        _ = mocker
            .mock_base()

        mocker.startMock()
    }
}
