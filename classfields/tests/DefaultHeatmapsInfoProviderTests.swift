//
//  DefaultHeatmapsInfoProviderTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 30.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc

class DefaultHeatmapsInfoProviderTests: XCTestCase {
    func testFallbackHeatmapsInfoCompleteness() {
        let heatmapsInfo = DefaultHeatmapsInfoProvider.fallbackHeatmapsInfo()
        let extractor = HeatmapsInfoExtractor(heatmapsInfo: heatmapsInfo)
        
        XCTAssertNotNil(extractor[.transport])
        XCTAssertNotNil(extractor[.infrastructure])
        
        // https://st.yandex-team.ru/VSAPPS-5073
        // XCTAssertNotNil(extractor[.ecology])
        
        XCTAssertNotNil(extractor[.priceSell])
        XCTAssertNotNil(extractor[.priceRent])
        XCTAssertNotNil(extractor[.profitability])
        XCTAssertNotNil(extractor[.carsharing])
        
        // Not applicable
        XCTAssertNil(extractor[.schools])
        XCTAssertNil(extractor[.unspecified])
    }
    
    func testHeatmapsInfoFromLegacyHeatmapAvailabilityInfo() {
        let legacyInfo = YREPlainHeatmapAvailabilityInfo(hasTransportLayer: true,
                                                         hasInfrastructureLayer: true,
                                                         hasEcologyLayer: false)
        
        let heatmapsInfo = DefaultHeatmapsInfoProvider.fallbackHeatmapsInfo(withLegacyInfo: legacyInfo)
        let extractor = HeatmapsInfoExtractor(heatmapsInfo: heatmapsInfo)
        
        XCTAssertNotNil(extractor[.transport])
        XCTAssertNotNil(extractor[.infrastructure])
        
        XCTAssertNil(extractor[.ecology])
        XCTAssertNil(extractor[.priceSell])
        XCTAssertNil(extractor[.priceRent])
        XCTAssertNil(extractor[.profitability])
        XCTAssertNil(extractor[.carsharing])
        
        // Not applicable
        XCTAssertNil(extractor[.schools])
        XCTAssertNil(extractor[.unspecified])
    }
}
