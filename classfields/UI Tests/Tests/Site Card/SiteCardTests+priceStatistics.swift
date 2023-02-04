//
//  SiteCardTests+priceStatistics.swift
//  UI Tests
//
//  Created by Alexey Salangin on 01.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

extension SiteCardTests {
    func testPriceStatistics() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)
        SiteCardAPIStubConfiguration.setupPriceStatistics(using: self.dynamicStubs)

        self.relaunchApp(with: .cardTests)

        SearchResultsListSteps()
            .isScreenPresented()
            .withSiteList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        let priceStatistics = SiteCardSteps()
            .isScreenPresented()
            .isLoadingIndicatorHidden()
            .scrollToPriceHistoryCell()
            .tapPriceStatisticsButton()

        priceStatistics.ensureViewAppeared()
        SitePriceStatisticsSteps.Room.allCases.forEach { room in
            SitePriceStatisticsSteps.Period.allCases.forEach { period in
                priceStatistics
                    .chooseRoom(room)
                    .choosePeriod(period)
                self.compareWithScreenshot(identifier: "sitePriceStatistics.\(room).\(period)")
            }
        }
    }
}
