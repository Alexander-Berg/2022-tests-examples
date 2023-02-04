//
//  SiteOfferListByPlanAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Fedor Solovev on 05.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class SiteOfferListByPlanAPIStubConfigurator {
    static func setupSiteSearchResultsList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-salarevo.debug")
    }

    static func setupSiteCard(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json", filename: "siteCard-salarevo.debug")
    }

    static func setupOfferStat(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/site/375274/offerStat", filename: "offerStat-salarevo.debug")
    }

    static func setupOfferPlanList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/site/375274/planSearch", filename: "planSearch-salarevo.debug")
    }

    static func setupSiteSearchResultsListByPlan(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-by-plan-salarevo.debug")
    }
}
