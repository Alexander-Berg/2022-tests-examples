//
//  VillageOfferListDataSourceTests.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 09.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest

import YREServiceLayer
import YREServiceInterfaces
import YREFiltersModel

final class VillageOfferListDataSourceTests: XCTestCase {
    func testReportingSearchResults() {
        // given
        let mockSearchResultsReporter = SearchResultsReporterMock()
        let dataSource = Self.makeOfferSnippetDataSource(
            searchResultsReporter: mockSearchResultsReporter
        )

        // when
        dataSource.obtainDataChunk(withOptions: nil)

        // then
        let expectedHandlers: [XCTestExpectation] = [
            mockSearchResultsReporter.report,
        ]
        self.wait(for: expectedHandlers, timeout: Self.timeout)
    }

    // MARK: - Private

    private static let pageSize: Int = 20
    private static let timeout: TimeInterval = 1

    private static func makeOfferSnippetDataSource(
        searchResultsReporter: SearchResultsReporterProtocol = SearchResultsMockReporter()
    ) -> VillageOfferListDataSource {
        let villagesService = VillagesServiceMock()
        let parameters = VillageOffersRequestParameters(
            identifier: "",
            offerTypes: [],
            primarySale: nil,
            priceMin: nil,
            priceMax: nil,
            lotAreaMin: nil,
            lotAreaMax: nil,
            houseAreaMin: nil,
            houseAreaMax: nil,
            buildingTypes: [],
            hasHeatingSupply: nil,
            hasWaterSupply: nil,
            hasSewerageSupply: nil,
            hasElectricitySupply: nil,
            hasGasSupply: nil,
            deliveryDate: nil,
            sort: nil,
            rawPrependParameters: nil,
            rawAppendParameters: nil
        )
        let dataSource = VillageOfferListDataSource(
            villageID: "",
            parameters: parameters,
            chunkState: YREListChunkState(asChunkedWithFirstChunk: 0),
            villagesService: villagesService,
            searchResultsReporter: searchResultsReporter)
        return dataSource
    }
}
