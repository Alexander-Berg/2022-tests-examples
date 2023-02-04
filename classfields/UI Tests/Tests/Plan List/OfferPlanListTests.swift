//
//  OfferPlanListTests.swift
//  UI Tests
//
//  Created by Fedor Solovev on 02.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class OfferPlanListTests: BaseTestCase {
    func testOfferPlanList() {
        OfferPlanListAPIStubConfigurator.setupSiteSearchResultsList(using: self.dynamicStubs)
        OfferPlanListAPIStubConfigurator.setupSiteCard(using: self.dynamicStubs)
        OfferPlanListAPIStubConfigurator.setupOfferStat(using: self.dynamicStubs)
        OfferPlanListAPIStubConfigurator.setupOfferPlanListWithOneOffer(using: self.dynamicStubs, stubKind: .oneOfferByPlan)

        self.performCommonTests { offerPlanListSteps in
            offerPlanListSteps
                .isScreenPresented()
                .isListNonEmpty()
                .isFilterButtonTappable()
                .isSortPanelHeaderViewTappable()
                .isCallButtonTappable()

            let cell = offerPlanListSteps.cell(withIndex: 0)
            cell
                .isPresented()
                .isShowApartmentsButtonTappable()
                .tap()

            let offerCardSteps = OfferCardSteps()
            offerCardSteps
                .isOfferCardPresented()
        }
    }

    private func performCommonTests(specificTests: (OfferPlanListSteps) -> Void) {
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

        siteCard
            .isScreenPresented()
            .scrollToPlanCell()
            .tapPlanCell()
        
        let offerPlanListSteps = OfferPlanListSteps()
        specificTests(offerPlanListSteps)
    }
}
