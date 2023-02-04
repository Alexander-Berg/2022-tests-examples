//
//  OfferPlanListAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Fedor Solovev on 02.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class OfferPlanListAPIStubConfigurator {
    enum StubKind {
        enum OfferPlanList {
            case oneOfferByPlan
            case manyOffersByPlan
            case finishedOfferByPlan
            case overflowingOffersByPlan
            case withVirtualTours

            var stubName: String {
                switch self {
                    case .oneOfferByPlan:
                        return "planSearch-salarevo-one-offer.debug"
                    case .manyOffersByPlan:
                        return "planSearch-salarevo-many-offers.debug"
                    case .finishedOfferByPlan:
                        return "planSearch-salarevo-one-offer-finished.debug"
                    case .overflowingOffersByPlan:
                        return "planSearch-salarevo-overflowingOffers.debug"
                    case .withVirtualTours:
                        return "planSearch-salarevo-virtualTours.debug"
                }
            }
        }
    }

    static func setupSiteSearchResultsList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-salarevo.debug")
    }

    static func setupSiteCard(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json", filename: "siteCard-salarevo.debug")
    }

    static func setupOfferStat(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/site/375274/offerStat", filename: "offerStat-salarevo.debug")
    }

    static func setupOfferPlanList(using dynamicStubs: HTTPDynamicStubs, siteID: String = "375274") {
        dynamicStubs.setupStub(remotePath: "/2.0/site/\(siteID)/planSearch", filename: "planSearch-salarevo.debug")
    }

    static func setupOfferPlanListWithOneOffer(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.OfferPlanList) {
        dynamicStubs.setupStub(remotePath: "/2.0/site/375274/planSearch", filename: stubKind.stubName)
    }
}
