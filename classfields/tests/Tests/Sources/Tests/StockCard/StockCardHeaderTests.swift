//
//  StockCardHeaderTests.swift
//  Tests
//
//  Created by Aleksey Gotyanov on 11/13/20.
//

import XCTest
import AutoRuProtoModels
import AutoRuModels
import Snapshots
@testable import AutoRuStockCard

class StockCardHeaderTests: BaseUnitTest {
    func testStockCardHeaderWithGetBestPriceButton() {
        let catalogResponse: Auto_Api_CatalogResponse = .init(mockFile: "complectation_comparison_subtree_skoda_octavia")

        let layout = StockCardHeaderLayout.make(
            model: StockCardHeaderLayout.Model(
                tags: [],
                photo: nil,
                subtree: catalogResponse,
                notice: nil,
                priceRange: 10_000_000...20_000_000,
                callbacks: .init(onTap: { fatalError() }, onNewCarRequest: {})
            )
        )

        Snapshot.compareWithSnapshot(layout: layout)
    }

    func testStockCardHeaderWithoutGetBestPriceButton() {
        let catalogResponse: Auto_Api_CatalogResponse = .init(mockFile: "complectation_comparison_subtree_skoda_octavia")

        let layout = StockCardHeaderLayout.make(
            model: StockCardHeaderLayout.Model(
                tags: [],
                photo: nil,
                subtree: catalogResponse,
                notice: nil,
                priceRange: 10_000_000...20_000_000,
                callbacks: .init(onTap: { fatalError() }, onNewCarRequest: nil)
            )
        )

        Snapshot.compareWithSnapshot(layout: layout)
    }
}
