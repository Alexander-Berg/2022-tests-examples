//
//  APIStubConfigurator.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 29.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class APIStubConfigurator {}

// MARK: - Search
extension APIStubConfigurator {
    static func setupOfferSearchResultsList_OrenburgRegion(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-offers-orenburgRegion.debug")
    }

    static func setupOfferSearchResultsList_Spb(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-offers-spb.debug")
    }

    static func setupSiteSearchResultsList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-sites-moscow.debug")
    }

    static func setupSiteSearchResultsList_MoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-sites-moscow-mo.debug")
    }

    static func setupVillageSearchResultsList_MoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/offerWithSiteSearch.json",
            filename: "offerWithSiteSearch-village-moscow-mo.debug"
        )
    }

    static func setupCommuteMapSearch(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/mapSearch.json", filename: "commuteMapSearch.debug")
    }
    
    static func setupCommutePolygon(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-commute.debug")
    }

    static func setupMapSearch_MoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/mapSearch.json", filename: "mapSearch-moscow-mo.debug")
    }
}

// MARK: - Cards
extension APIStubConfigurator {
    static func setupOfferCard(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard.debug")
    }

    static func setupSiteCard(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json", filename: "siteCard.debug")
    }

    static func setupVillageCard(using dynamicStubs: HTTPDynamicStubs, villageID: String = "1831744") {
        dynamicStubs.setupStub(remotePath: "/2.0/village/\(villageID)/card", filename: "villageCard.debug")
    }
}

// MARK: - Deeplinks parsing
extension APIStubConfigurator {
    static func setupOfferCardDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub( remotePath: "/1.0/deeplink.json", filename: "offerCardDeeplink.debug", method: .POST)
    }

    static func setupSiteCardDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "siteCardDeeplink.debug", method: .POST)
    }

    static func setupVillageCardDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "villageCardDeeplink.debug", method: .POST)
    }

    static func setupVillageCardDeeplinkWithMoscowRegion(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "villageCardDeeplink-withMoscowRegion.debug", method: .POST)
    }
    
    static func setupFavoritesDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "favoritesDeeplink.debug", method: .POST)
    }
    
    static func setupSavedSearchesDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "savedSearchesDeeplink.debug", method: .POST)
    }

    static func setupOfferListDeeplink_Spb(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "offerListDeeplink.debug", method: .POST)
    }

    static func setupOfferListDeeplink_EmptyGeo(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "offerListDeeplink-emptyGeo.debug", method: .POST)
    }

    static func setupOfferListDeeplink_OrenburgRegion(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "offerListDeeplink-orenburgRegion.debug", method: .POST)
    }

    static func setupOfferListDeeplink_RentRoom(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "offerListDeeplink-rent-room.debug", method: .POST)
    }

    static func setupOfferListDeeplink_YaRent_Moscow(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "offerListDeeplink-yaRent-moscow.debug", method: .POST)
    }

    static func setupSiteListDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "siteListDeeplink.debug", method: .POST)
    }

    static func setupVillageListDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "villageListDeeplink-moscow-mo.debug", method: .POST)
    }

    static func setupCommuteWithAddressDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "commuteWithAddressDeeplink.debug", method: .POST)
    }

    static func setupCommuteWithoutAddressDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "commuteWithoutAddressDeeplink.debug", method: .POST)
    }

    static func setupPriceRentHeatmapDeeplink_MoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "priceRentHeatmapDeeplink-moscow-mo.debug", method: .POST)
    }

    static func setupJournalDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "journalDeeplink.debug", method: .POST)
    }

    static func setupInAppServicesDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "inAppServicesDeeplink.debug", method: .POST)
    }
    
    static func setupUnknownActionDeeplink(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/deeplink.json", filename: "unknownActionDeeplink.debug", method: .POST)
    }
}

// MARK: - User Utils

extension APIStubConfigurator {
    static func setupUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-owner.debug")
    }

    static func setupAgentUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-agent.debug")
    }


    static func setupLegalEntityUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-legalEntity.debug")
    }

    static func setupBannedUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-banned.debug")
    }

    static func setupUserWithOverQuota(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-overQuota.debug")
    }

    static func setupUserWithEnabledMosRu(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-owner-enabledMosRu.debug")
    }

    static func setupUserWithDisabledMosRu(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user",
                               filename: "user-owner-disabledMosRu.debug")
    }
}

// MARK: - Common Utils

extension APIStubConfigurator {
    static func setupRequiredFeaturesError(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/device/requiredFeature",
                               filename: "commonError.debug")
    }
}

// MARK: - Yandex rent

extension APIStubConfigurator {
    static func setupOfferSearchResultsList_YandexRent(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/offerWithSiteSearch.json",
            filename: "offerWithSiteSearch-yandexRent.debug"
        )
    }

    static func setupOfferSearchResultsList_IframeVirtualTour(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/offerWithSiteSearch.json",
            filename: "offerWithSiteSearch-iframeVirtualTour.debug"
        )
    }

    static func setupOfferSearchResultsList_YandexRent_VirtualTourOnly(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/offerWithSiteSearch.json",
            filename: "offerWithSiteSearch-yandexRent-virtualTourOnly.debug"
        )
    }
    
    static func setupOfferSearchResultsList_YandexRent_YoutubeVideo(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/offerWithSiteSearch.json",
            filename: "offerWithSiteSearch-yandexRent-youtubeVideo.debug"
        )
    }
}

// MARK: - Offers

extension APIStubConfigurator {
    static func setupOfferSearchResultsCount(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/offers/number",
            filename: "offersNumber-number.debug"
        )
    }
    
    static func setupOfferSearchResultsCountWithExtendedNumber_1(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/offers/number",
            filename: "offersNumber-withExtendedNumber-1.debug"
        )
    }
    
    static func setupOfferSearchResultsCountWithExtendedNumber_2(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/offers/number",
            filename: "offersNumber-withExtendedNumber-2.debug"
        )
    }
}

// MARK: - Phone binding

extension APIStubConfigurator {
    static func setupPhoneBinding(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/passport/phone/bind", filename: "passport-phone-bind.debug", method: .POST)
    }

    static func setupPhoneConfirmation(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/passport/phone/confirm", filename: "passport-phone-confirm.debug", method: .POST)
    }
}

// MARK: - Search extending

extension APIStubConfigurator {
    static func setupSearchExtendingResultWithPrice(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/offers/number/extend-filters",
            filename: "extendFilters-price.debug"
        )
    }
    
    static func setupSearchExtendingResultWithViewPort(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/offers/number/extend-filters",
            filename: "extendFilters-viewPort.debug"
        )
    }
    
    static func setupSearchExtendingResultWithPriceAndViewPort(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/offers/number/extend-filters",
            filename: "extendFilters-priceAndViewPort.debug"
        )
    }
}
