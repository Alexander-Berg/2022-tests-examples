//
//  SiteCardAPIStubConfiguration.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 18.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class SiteCardAPIStubConfiguration {
    enum StubKind {
        enum FlatStatus {
            enum SoldSubtype {
                case flat
                case flatWithStatParams
                case apartmentWithStatParams
                case flatsAndApartmentsWithStatParams
            }

            enum CeilingHeightSubtype {
                case singleValue
                case range
            }

            case unknown
            case onSale
            case notOnSale
            case soonAvailable
            case temporaryUnavailable
            case sold(SoldSubtype)
            case inProject
            case noLargeImages
            case noPhoto
            case singlePhoto
            case severalPhotos
            case billing
            case withoutReviews
            case withReviews
            case callback
            case ceilingHeight(CeilingHeightSubtype)
            case minimalInfo
            case tours
            case overviews
            case allBlocks

            var stubName: String {
                switch self {
                    case .unknown:
                        return "siteCard-flatStatusUnknown.debug"
                    case .onSale:
                        return "siteCard-flatStatusOnSale.debug"
                    case .notOnSale:
                        return "siteCard-flatStatusNotOnSale.debug"
                    case .soonAvailable:
                        return "siteCard-flatStatusSoonAvailable.debug"
                    case .temporaryUnavailable:
                        return "siteCard-flatStatusTemporaryUnavailable.debug"
                    case let .sold(subtype):
                        switch subtype {
                            case .flat:
                                return "siteCard-flatStatusSold.debug"
                            case .flatWithStatParams:
                                return "siteCard-flatStatusSold-statParams-flat.debug"
                            case .apartmentWithStatParams:
                                return "siteCard-flatStatusSold-statParams-apartment.debug"
                            case .flatsAndApartmentsWithStatParams:
                                return "siteCard-flatStatusSold-statParams-flatsAndApartments.debug"
                        }
                    case .inProject:
                        return "siteCard-flatStatusInProject.debug"
                    case .noLargeImages:
                        return "siteCard-noLargeImages.debug"
                    case .noPhoto:
                        return "siteCard-noPhoto.debug"
                    case .singlePhoto:
                        return "siteCard-singlePhoto.debug"
                    case .severalPhotos:
                        return "siteCard-severalPhotos.debug"
                    case .billing:
                        return "siteCard-billing.debug"
                    case .withoutReviews:
                        return "siteCard-withoutReviews.debug"
                    case .withReviews:
                        return "siteCard-withReviews.debug"
                    case .callback:
                        return "siteCard-withCallback.debug"
                    case let .ceilingHeight(subtype):
                        switch subtype {
                            case .singleValue:
                                return "siteCard-ceilingHeightSingleValue.debug"
                            case .range:
                                return "siteCard-ceilingHeightRange.debug"
                        }
                    case .minimalInfo:
                        return "siteCard-minimal-info.debug"
                    case .tours:
                        return "siteCard-withTours.debug"
                    case .overviews:
                        return "siteCard-withOverviews.debug"
                    case .allBlocks:
                        return "siteCard-allBlocks.debug"
                }
            }
        }
    }

    struct Consts {
        enum Site: String {
            
            case levelAmurskaya
            case novaDom
            case severniyFontan

            var siteId: String {
                switch self {
                    case .levelAmurskaya:
                        return "521570"
                    case .novaDom:
                        return "1678495"
                    case .severniyFontan:
                        return "296015"
                }
            }
        }
    }

    static func setupSiteSearchResultsListMoscow(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-sites-moscow.debug")
    }

    static func setupSiteSearchResultsListMoscowBilling(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-sites-moscow-billing.debug")
    }

    static func setupSiteSearchResultsListNovosibirsk(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/offerWithSiteSearch.json", filename: "offerWithSiteSearch-sites-novosibirsk.debug")
    }

    static func setupSiteCardLevelAmurskaya(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json", filename: "siteCard-levelAmurskaya.debug")
    }

    static func setupSiteCardNovaDom(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json", filename: "siteCard-novaDom.debug")
    }

    static func setupSiteCard(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.FlatStatus) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json", filename: stubKind.stubName)
    }

    static func setupSiteCardSalarevoPark(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/siteWithOffersStat.json",
                               filename: "siteCard-salarevoPark.debug")
    }

    static func setupOfferStat(using dynamicStubs: HTTPDynamicStubs, site: Consts.Site) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/site/\(site.siteId)/offerStat",
            filename: "offerStat-\(site.rawValue).debug"
        )
    }

    static func setupOfferStatEmptyList(using dynamicStubs: HTTPDynamicStubs, site: Consts.Site) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/site/\(site.siteId)/offerStat",
            filename: "offerStat-\(site.rawValue)-emptyList.debug"
        )
    }

    static func setupOfferStatNoPrimaryOffers(using dynamicStubs: HTTPDynamicStubs, site: Consts.Site) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/site/\(site.siteId)/offerStat",
            filename: "offerStat-\(site.rawValue)-no-primaryOffers.debug"
        )
    }

    static func setupOfferStatNoAnyOffers(using dynamicStubs: HTTPDynamicStubs, site: Consts.Site) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/site/\(site.siteId)/offerStat",
            filename: "offerStat-\(site.rawValue)-no-anyOffers.debug"
        )
    }

    static func setupGetTwoPhones(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/newbuilding/521570/contacts", filename: "siteCard-getPhonesTwoPhones.debug")
    }

    static func setupGetPhonesError(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/newbuilding/521570/contacts", filename: "siteCard-getPhonesError.debug")
    }

    static func setupGetPhones(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "2.0/newbuilding/521570/contacts", filename: "siteCard-getPhones.debug")
    }

    static func setupOfferStatSalarevoPark(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/site/375274/offerStat", filename: "offerStat-salarevoPark.debug"
        )
    }

    static func setupPriceStatistics(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "2.0/newbuilding/521570/price-statistics-series",
            filename: "siteCard-price-statistics-series.debug"
        )
    }

    static func setupSimilarSitesList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "1.0/newbuilding/siteLikeSearch",
            filename: "siteCard-similar-sites.debug"
        )
    }
}

extension SiteCardAPIStubConfiguration {
    static func setupSiteCardExpectation(
        _ expectation: XCTestExpectation,
        using dynamicStubs: HTTPDynamicStubs
    ) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/siteWithOffersStat.json",
            middleware: MiddlewareBuilder
                .chainOf([
                    .expectation(expectation),
                    .respondWith(.ok(.contentsOfJSON("siteCard-billing.debug"))),
                ])
                .build()
        )
    }
}
