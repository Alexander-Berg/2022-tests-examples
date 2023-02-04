//
//  SnippetsListAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 08.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

// swiftlint:disable nesting
final class SnippetsListAPIStubConfigurator {
    enum StubKind {
        enum Offer {
            enum AdditionalActions: String {
                case noShareURL = "offerSnippet-noShareURL.debug"
                case withUserNote = "offerSnippet-withUserNote.debug"
                case withoutUserNote = "offerSnippet-withoutUserNote.debug"
            }

            enum CallButton: String {
                case enabled = "offerSnippet-callButton-enabled.debug"
                case hidden = "offerSnippet-callButton-hidden.debug"
            }

            enum Common: String {
                case singleOffer = "offerSnippet-common-singleItem.debug"
                case empty = "offerSnippet-common-empty.debug"
            }

            case common(Common)
            case noLargeImages
            case noPhoto
            case additionalActions(AdditionalActions)
            case callButton(CallButton)

            var stubName: String {
                switch self {
                    case .noPhoto:
                        return "offerSnippet-noPhoto-apartment.debug"
                        
                    case .noLargeImages:
                        return "offerSnippet-noLargeImages.debug"

                    case .additionalActions(let option):
                        return option.rawValue

                    case .callButton(let option):
                        return option.rawValue

                    case .common(let option):
                        return option.rawValue
                }
            }
        }

        enum Site: String {
            case common = "siteSnippet-common.debug"
            case empty = "siteSnippet-empty.debug"
        }

        enum Village: String {
            case noPhoto = "villagesSnippet-noPhoto.debug"
        }
    }

    static func setupOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Offer) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: stubKind.stubName)
    }

    static func setupSitesList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Site) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: stubKind.rawValue)
    }

    static func unsetupSitesList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: StubKind.Site.empty.rawValue)
    }

    static func setupVillagesList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Village) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json",
                               filename: stubKind.rawValue)
    }

    static func setupGetOfferTwoPhoneNumbers(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.setupStub(remotePath: "2.0/offers/\(offerID)/phones", filename: "offerCard-getPhonesTwoPhones.debug")
    }

    static func setupGetOfferOnePhoneNumber(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.setupStub(remotePath: "2.0/offers/\(offerID)/phones", filename: "offerCard-getPhonesOnePhone.debug")
    }

    static func setupGetOfferNumbersError(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.setupStub(remotePath: "2.0/offers/\(offerID)/phones", filename: "offerCard-getPhonesError.debug")
    }

    static func setupGetSiteOnePhoneNumber(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/newbuilding/229293/contacts", filename: "siteCard-getPhonesOnePhone.debug")
    }
    
    static func setupGetOfferOnePhoneNumbers(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.setupStub(remotePath: "2.0/offers/\(offerID)/phones", filename: "offerCard-getPhones-onephone.debug")
    }

    static func setupGetOfferBillingPhoneNumbers(using dynamicStubs: HTTPDynamicStubs, offerID: String = "157352957988240017") {
        dynamicStubs.setupStub(remotePath: "2.0/offers/\(offerID)/phones",
                               filename: "offerCard-getPhones-onephone-billing.debug")
    }

    static func setupGetSiteTwoPhoneNumbers(using dynamicStubs: HTTPDynamicStubs, siteID: String = "229293") {
        dynamicStubs.setupStub(
            remotePath: "2.0/newbuilding/\(siteID)/contacts",
            filename: "siteCard-getPhonesTwoPhones.debug"
        )
    }

    static func setupGetSiteSinglePhoneNumber(using dynamicStubs: HTTPDynamicStubs, siteID: String = "296015") {
        dynamicStubs.setupStub(
            remotePath: "2.0/newbuilding/\(siteID)/contacts",
            filename: "siteCard-getPhone.debug"
        )
    }

    static func setupGetSiteNumbersError(using dynamicStubs: HTTPDynamicStubs, siteID: String = "229293") {
        dynamicStubs.setupStub(
            remotePath: "2.0/newbuilding/\(siteID)/contacts",
            filename: "siteCard-getPhonesError.debug"
        )
    }

    static func setupGetVillageTwoPhoneNumbers(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/village/1835631/contacts", filename: "villageCard-getPhonesTwoPhones.debug")
    }

    static func setupGetVillageOnePhoneNumber(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/village/1835631/contacts", filename: "villageCard-getPhonesOnePhone.debug")
    }

    static func setupGetVillageNumbersError(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/village/1835631/contacts", filename: "villageCard-getPhonesError.debug")
    }
}

extension SnippetsListAPIStubConfigurator {
    static func setupListExpectation(
        _ expectation: XCTestExpectation,
        using dynamicStubs: HTTPDynamicStubs
    ) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/offerWithSiteSearch.json",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .respondWith(.ok(.contentsOfJSON("offerSnippet-common-singleItem.debug"))),
                ])
                .build()
        )
    }
}
