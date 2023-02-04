import XCTest

import Snapshots
import AutoRuStockCard

class StockCardTests: BaseUnitTest {
    func test_filtersParamsWithoutSelected() {
        let layout = StockCardQuickFiltersLayout(
            model: makeChipsParam(selected: false),
            openFilter: {}
        )
        Snapshot.compareWithSnapshot(layout: layout)
    }

    func test_filtersParamsWithSelected() {
        let layout = StockCardQuickFiltersLayout(
            model: makeChipsParam(selected: true),
            openFilter: {}
        )
        Snapshot.compareWithSnapshot(layout: layout)
    }

    //MARK: - Private

    private func makeChipsParam(selected: Bool) -> StockCardQuickFiltersModel {
        let params = [
            StockCardQuickFiltersModel.Param(
                id: "quickFiltersComplectation",
                title: "Комплектация",
                selected: selected,
                onTap: nil
            ),
            StockCardQuickFiltersModel.Param(
                id: "quickFiltersEngine",
                title: "Двигатель",
                selected: false,
                onTap: nil
            ),
            StockCardQuickFiltersModel.Param(
                id: "quickFiltersTransmission",
                title: "Коробка",
                selected: false,
                onTap: nil)
        ]
        return StockCardQuickFiltersModel(
            params: params,
            selectedFiltersCount: selected ? 1 : 0,
            initialContentOffset: nil,
            onDidScroll: { _ in }
        )
    }
}
