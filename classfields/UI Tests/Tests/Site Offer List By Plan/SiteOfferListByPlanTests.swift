//
//  SiteOfferListByPlanTests.swift
//  UI Tests
//
//  Created by Fedor Solovev on 05.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class SiteOfferListByPlanTests: BaseTestCase {
    func testSiteOfferList() {
        SiteOfferListByPlanAPIStubConfigurator.setupSiteSearchResultsList(using: self.dynamicStubs)
        SiteOfferListByPlanAPIStubConfigurator.setupSiteCard(using: self.dynamicStubs)
        SiteOfferListByPlanAPIStubConfigurator.setupOfferStat(using: self.dynamicStubs)
        SiteOfferListByPlanAPIStubConfigurator.setupOfferPlanList(using: self.dynamicStubs)

        self.performCommonTests { siteOfferListByPlanSteps in
            siteOfferListByPlanSteps
                .isScreenPresented()
                .isListNonEmpty()
                .isCallButtonTappable()
                .isFilterButtonTappable()
                .isSortButtonTappable()

            let cell = siteOfferListByPlanSteps.cell(withIndex: 0)
            cell
                .isPresented()
                .isFavoritesButtonTappable()

            cell.tap()

            let offerCardSteps = OfferCardSteps()
            offerCardSteps
                .isOfferCardPresented()
        }
    }

    private func performCommonTests(specificTests: (SiteOfferListByPlanSteps) -> Void) {
        self.relaunchApp(with: .cardTests)

        let list = SearchResultsListSteps()
        let siteCard = SiteCardSteps()

        list
            .isScreenPresented()

        let siteList = list.withSiteList()
        siteList
            .isListNonEmpty()

        let cell = siteList.cell(withIndex: 0)
        cell
            .isPresented()
            .tap()

        SiteOfferListByPlanAPIStubConfigurator.setupSiteSearchResultsListByPlan(using: self.dynamicStubs)

        siteCard
            .isScreenPresented()
            .isRoomStatisticsCellTappable(title: "1-комнатные")
            .tapRoomStatisticsCell(title: "1-комнатные")

        let siteOfferListByPlanSteps = SiteOfferListByPlanSteps()
        specificTests(siteOfferListByPlanSteps)
    }
}
