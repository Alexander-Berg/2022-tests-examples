//
//  OfferCardAPIStubConfiguration.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 10/19/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class OfferCardAPIStubConfiguration {
    static func setupOfferSearchResultsList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: "offerWithSiteSearch-offers-5205620753106733305.debug")
    }

    static func setupOfferSearchResultsList_offerFromSite(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: "offerWithSiteSearch-offers-2127925591568953902.debug")
    }

    static func setupOfferSearchResultsListBilling(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: "offerWithSiteSearch-billing-offers.debug")
    }

    static func setupOfferSearchResultsListYandexRent(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: "offerWithSiteSearch-yandexRent-offers.debug")
    }

    static func setupOfferCard(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-5205620753106733305.debug")
    }

    static func setupOfferCardInSite(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json",
                               filename: "offerCard-2127925591568953902-in-site.debug")
    }
    static func setupOfferCardBilling(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-billing.debug")
    }

    static func setupOfferCardWithUsernote(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-157352957988240017-userNote.debug")
    }

    static func setupOfferCardWithEditedUserNote(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json",
                               filename: "offerCard-157352957988240017-editedUserNote.debug")
    }

    static func setupOfferCardWithoutUsernote(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-157352957988240017.debug")
    }

    static func setupOfferCardWithoutYandexRent(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-7759004176174453249-without-yandexRent.debug")
    }

    static func setupOfferCardWithAnonym(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-7759004176174453249-anonym.debug")
    }

    static func setupOfferCardWithCallback(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-withCallback.debug")
    }

    static func setupOfferCardYandexRent(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/cardWithViews.json",
            filename: "offerCard-rentLongApartment-yaRent.debug"
        )
    }

    static func setupGetTwoPhones(using dynamicStubs: HTTPDynamicStubs) {
        Self.setupGetTwoPhones(using: dynamicStubs, offerId: "5205620753106733305")
    }

    static func setupGetPhonesError(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/offers/5205620753106733305/phones", filename: "offerCard-getPhonesError.debug")
    }

    static func setupNoLargeImages(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-noLargeImages.debug")
    }

    static func setupOfferCardWithImages(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/cardWithViews.json", filename: "offerCard-7759004176174453249-with-images.debug")
    }

    static func setupSimilarOffer(using dynamicStubs: HTTPDynamicStubs, for offerID: String = "5205620753106733305") {
        dynamicStubs.setupStub(remotePath: "/1.0/offer/\(offerID)/similar", filename: "similar.debug")
    }

    static func setupGetTwoPhonesFromGallery(using dynamicStubs: HTTPDynamicStubs) {
        Self.setupGetTwoPhones(using: dynamicStubs, offerId: "7759004176174453249")
    }

    private static func setupGetTwoPhones(using dynamicStubs: HTTPDynamicStubs, offerId: String) {
        dynamicStubs.setupStub(remotePath: "2.0/offers/\(offerId)/phones", filename: "offerCard-getPhonesTwoPhones.debug")
    }

    static func setupYandexRentDynamicBoundingBoxSpb(using dynamicStubs: HTTPDynamicStubs) {
         dynamicStubs.setupStub(
             remotePath: "1.0/dynamicBoundingBox",
             filename: "dynamicBoundingBox-yandexRent-spb.debug"
         )
     }

     static func setupIsPointInsideRentTrueSpb(using dynamicStubs: HTTPDynamicStubs) {
         dynamicStubs.setupStub(
             remotePath: "2.0/rent/is-point-rent",
             filename: "isPointInsideRent-yandexRent-true-spb.debug"
         )
     }

    static func setupIsPointInsideRent_False_Spb(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "2.0/rent/is-point-rent",
            filename: "isPointInsideRent-yandexRent-false-spb.debug"
        )
    }

    static func setupOfferCardWithAllContentTypes(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/cardWithViews.json",
            filename: "offerCard-all-content-types.debug"
        )
    }
}

extension OfferCardAPIStubConfiguration {
    static func setupOfferCardExpectation(
        _ expectation: XCTestExpectation,
        using dynamicStubs: HTTPDynamicStubs,
        offerID: String = "5205620753106733305"
    ) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/cardWithViews.json",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .respondWith(.ok(.contentsOfJSON("offerCard-\(offerID).debug"))),
                ])
                .build()
        )
    }
}
