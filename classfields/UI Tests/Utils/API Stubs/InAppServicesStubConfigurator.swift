//
//  InAppServicesStubConfigurator.swift
//  UI Tests
//
//  Created by Ella Meltcina on 9/25/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

enum InAppServicesStubConfigurator {
    static func setupEmptyServiceInfo(using dynamicStubs: HTTPDynamicStubs, overrideFlats: Bool = true) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-empty.debug")

        if overrideFlats {
            RentAPIStubConfiguration.setupEmptyFlats(using: dynamicStubs)
        }
    }

    static func setupBannedServiceInfo(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-banned.debug")
    }

    static func setupServiceInfoWithOwner(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-owner.debug")
    }

    static func setupServiceInfoWithTenant(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-tenant.debug")
    }

    static func setupServiceInfoWithOfferListOnly(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-offers-list.debug")

        RentAPIStubConfiguration.setupEmptyFlats(using: dynamicStubs)
    }

    static func setupServiceInfoRentUserWithOffers(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-rent-offers-collection.debug")

        RentAPIStubConfiguration.setupEmptyFlats(using: dynamicStubs)
    }

    static func setupServiceInfoWithOfferCollection(
        withPromoOffer: Bool,
        using dynamicStubs: HTTPDynamicStubs
    ) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/service/info",
            filename: withPromoOffer
                ? "service-info-offers-collection-promo.debug"
                : "service-info-offers-without-yarent-collection.debug"
        )
    }

    static func setupServiceInfoWithOfferCollectionWithSpecifiedActions(
        using dynamicStubs: HTTPDynamicStubs
    ) {
        dynamicStubs.register(method: .GET,
                              path: "/2.0/service/info",
                              filename: "service-info-offers-collection-specified-actions.debug")
    }
}
