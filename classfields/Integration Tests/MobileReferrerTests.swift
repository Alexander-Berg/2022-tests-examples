//
//  MobileReferrerTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 20.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREMapScreenModule
import YREModel
import YREModelObjc
@testable import YREAnalytics
import YREOfferCardModule
import YREFiltersModel

private final class ReferrerAnalyticsReporterMock: NSObject, AnalyticsProtocol {
    init(check: @escaping (YREAnalyticsEvent) -> Void) {
        self.check = check
    }

    func report(_ event: YREAnalyticsEvent) {
        self.check(event)
    }

    private let check: (YREAnalyticsEvent) -> Void
}

final class MobileReferrerTests: XCTestCase {
    func testMapToOfferCardReferrer() {
        let mapScreenModuleDepsMock = MapScreenModuleDepsMock(anyOfferSearchService: anyOfferSearchService)
        let router = MapScreenRouter(
            moduleDeps: mapScreenModuleDepsMock,
            serviceDeps: .init(
                mapSettingsWriter: MapSettingsWriterMock(),
                navigationStateWriter: NavigationStateWriterMock(),
                appSessionStateReader: AppSessionStateReaderMock(),
                filterInteractor: filterInteractor,
                mapScreenAnalyticsReporter: .init(analyticsReporter: AnalyticsReporterMock())
            ),
            routerPartsFactory: RouterPartsFactoryMock(),
            navigationContext: .tabBar(tabItemViewController: .init()),
            interactor: MapScreenInteractorMock()
        )

        let routerPartsFactoryMock = RouterPartsFactoryMock()
        let offerSnippetInteractor = routerPartsFactoryMock.makeOfferSnippetInteractor(
            offerData: .geoPoint(YREGeoPoint.customPoint(withCenter: .init(lat: 0.0, lon: 0.0)))
        )
        let offerSnippetPresenter = routerPartsFactoryMock.makeOfferSnippetPresenter(
            interactor: offerSnippetInteractor,
            favoritesModule: FavoritesModuleMock()
        )

        let expectation = self.expectation(description: "Waiting for analytics request.")

        OfferCardAnalyticsHelper.analyticsReporter = ReferrerAnalyticsReporterMock { event in
            guard let event = event as? AnalyticsOfferShowEvent else { return }
            let expectedReferrer = ScreenReferrer(previousScreen: .map, currentScreen: .offerCard)
            XCTAssertEqual(event.referrer, expectedReferrer)
            expectation.fulfill()
        }

        router.offerSnippetPresenterWantsOpenOfferCard(offerSnippetPresenter, forAbstractOffer: offer)
        mapScreenModuleDepsMock.offerCardViewController?.beginAppearanceTransition(true, animated: false)
        mapScreenModuleDepsMock.offerCardViewController?.endAppearanceTransition()

        self.wait(for: [expectation], timeout: 5)
    }

    func testOfferCardToMapReferrer() {
        let mapScreenModuleDepsMock = MapScreenModuleDepsMock(anyOfferSearchService: anyOfferSearchService)
        let router = MapScreenRouter(
            moduleDeps: mapScreenModuleDepsMock,
            serviceDeps: .init(
                mapSettingsWriter: MapSettingsWriterMock(),
                navigationStateWriter: NavigationStateWriterMock(),
                appSessionStateReader: AppSessionStateReaderMock(),
                filterInteractor: filterInteractor,
                mapScreenAnalyticsReporter: .init(analyticsReporter: AnalyticsReporterMock())
            ),
            routerPartsFactory: RouterPartsFactoryMock(),
            navigationContext: .tabBar(tabItemViewController: .init()),
            interactor: MapScreenInteractorMock()
        )

        let routerPartsFactoryMock = RouterPartsFactoryMock()

        let expectation = self.expectation(description: "Waiting for analytics request.")

        OfferCardAnalyticsHelper.analyticsReporter = ReferrerAnalyticsReporterMock { event in
            guard let event = event as? AnalyticsOfferShowEvent else { return }
            let expectedReferrer = ScreenReferrer(previousScreen: .listing, currentScreen: .offerCard)
            XCTAssertEqual(event.referrer, expectedReferrer)
            expectation.fulfill()
        }

        let transformer = FilterTransformer { root in // swiftlint:disable:this closure_body_length
            .withRegionInfo(
                filterRoot: root,
                regionInfo: .init(
                    region: .init(
                        rgid: 0,
                        plainGeoIntent: .init(
                            geoObject: YREGeoObject(
                                name: nil,
                                shortName: nil,
                                address: nil,
                                searchParams: nil,
                                scope: nil,
                                type: .city,
                                center: nil,
                                boundingBox: nil
                            )
                        )
                    ),
                    configuration: .init(
                        regionID: nil,
                        geoID: nil,
                        subjectFederationID: nil,
                        locativeName: nil,
                        availabilityFlags: .init(
                            hasCommercialBuildings: .paramBoolFalse,
                            hasMetro: .paramBoolFalse,
                            hasSites: .paramBoolFalse,
                            hasVillages: .paramBoolFalse,
                            hasConcierge: .paramBoolFalse,
                            hasYandexRent: .paramBoolFalse,
                            hasDeveloperLegendaPromo: .paramBoolFalse,
                            hasPaidSites: .paramBoolFalse
                        ),
                        parameterAvailability: nil,
                        schoolInfo: .init(
                            hasSchoolLayer: false,
                            total: nil,
                            highRatingColor: nil,
                            lowRatingColor: nil
                        ),
                        heatmapsInfo: [],
                        isDirty: false
                    )
                )
            )
        }
        router.presentSearchResultsList(filterTransformer: transformer, listDisplayingOptions: nil)

        // Wait for `presentSearchResultsList` completion.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            router.mainOfferListPresenter(
                routerPartsFactoryMock.makeMainListPresenter(with: routerPartsFactoryMock.makeOffersInteractor()),
                didSelect: self.offerSnippet
            )
            mapScreenModuleDepsMock.offerCardViewController?.beginAppearanceTransition(true, animated: false)
            mapScreenModuleDepsMock.offerCardViewController?.endAppearanceTransition()
        }

        self.wait(for: [expectation], timeout: 5)
    }

    private let offerSnippet = YREOfferSnippet(
        identifier: "",
        type: .sell,
        category: .apartment,
        partnerId: nil,
        internal: .paramBoolFalse,
        creationDate: nil,
        update: nil,
        newFlatSale: .paramBoolTrue,
        primarySale: .paramBoolTrue,
        flatType: .newFlat,
        large1242ImageURLs: nil,
        appLargeImageURLs: nil,
        largeImageURLs: nil,
        middleImageURLs: nil,
        miniImageURLs: nil,
        smallImageURLs: nil,
        fullImageURLs: nil,
        mainImageURLs: nil,
        previewImages: nil,
        offerPlanImages: nil,
        floorPlanImages: nil,
        youtubeVideoReviewURL: nil,
        location: nil,
        metro: nil,
        price: nil,
        offerPriceInfo: nil,
        vas: nil,
        roomsOffered: 1,
        roomsTotal: 1,
        area: nil,
        livingSpace: nil,
        floorsTotal: 0,
        floorsOffered: [],
        lotArea: nil,
        lotType: .unknown,
        housePart: .paramBoolFalse,
        houseType: .unknown,
        studio: .paramBoolFalse,
        commissioningDate: nil,
        isFreeReportAvailable: .paramBoolFalse,
        isPurchasingReportAvailable: .paramBoolFalse,
        building: nil,
        garage: nil,
        author: nil,
        isFullTrustedOwner: .paramBoolFalse,
        salesDepartments: nil,
        commercialDescription: nil,
        villageInfo: nil,
        siteInfo: nil,
        isOutdated: false,
        viewsCount: 0,
        uid: nil,
        share: nil,
        queryContext: nil,
        apartment: nil,
        chargeForCallsType: .byCampaign,
        yandexRent: .paramBoolUnknown,
        userNote: nil,
        virtualTours: nil,
        offerChatType: .unknown,
        hasPaidCalls: .paramBoolUnknown
    )

    private let offer = YREOffer(
        identifier: "",
        type: .sell,
        category: .apartment,
        partnerId: nil,
        internal: .paramBoolUnknown,
        creationDate: nil,
        newFlatSale: .paramBoolFalse,
        primarySale: .paramBoolFalse,
        flatType: .secondary,
        urlString: nil,
        update: nil,
        roomsTotal: 3,
        roomsOffered: 3,
        floorsTotal: 30,
        floorsOffered: [1],
        author: nil,
        isFullTrustedOwner: .paramBoolTrue,
        trust: .yreConstantParamOfferTrustHigh,
        viewsCount: 0,
        floorCovering: .unknown,
        area: YREArea(unit: .m2, value: 100),
        livingSpace: nil,
        kitchenSpace: nil,
        roomSpace: nil,
        large1242ImageURLs: nil,
        appLargeImageURLs: nil,
        minicardImageURLs: nil,
        middleImageURLs: nil,
        largeImageURLs: nil,
        fullImageURLs: nil,
        previewImages: nil,
        offerPlanImages: nil,
        floorPlanImages: nil,
        youtubeVideoReviewURL: nil,
        suspicious: .paramBoolUnknown,
        active: .paramBoolUnknown,
        hasAlarm: .paramBoolUnknown,
        housePart: .paramBoolUnknown,
        price: nil,
        offerPriceInfo: nil,
        location: .init(
            regionID: nil,
            geoID: nil,
            subjectFederationId: nil,
            subjectFederationRgid: nil,
            subjectFederationName: nil,
            address: nil,
            streetAddress: nil,
            geocoderAddress: nil,
            point: nil,
            allHeatmapPoints: nil,
            expectedMetros: nil,
            ponds: nil,
            parks: nil,
            schools: nil,
            metroList: nil
        ),
        metro: nil,
        house: nil,
        building: nil,
        garage: nil,
        lotArea: nil,
        lotType: .unknown,
        offerDescription: nil,
        commissioningDate: nil,
        ceilingHeight: nil,
        haggle: .paramBoolUnknown,
        mortgage: .paramBoolUnknown,
        rentPledge: .paramBoolUnknown,
        electricityIncluded: .paramBoolUnknown,
        cleaningIncluded: .paramBoolUnknown,
        withKids: .paramBoolUnknown,
        withPets: .paramBoolUnknown,
        supportsOnlineView: .paramBoolUnknown,
        zhkDisplayName: nil,
        apartment: nil,
        dealStatus: .primarySale,
        agentFee: nil,
        commission: nil,
        prepayment: nil,
        securityPayment: nil,
        taxationForm: .unknown,
        isFreeReportAvailable: .paramBoolTrue,
        isPurchasingReportAvailable: .paramBoolTrue,
        paidExcerptsInfo: nil,
        generatedFromSnippet: false,
        heating: .paramBoolUnknown,
        water: .paramBoolUnknown,
        sewerage: .paramBoolUnknown,
        electricity: .paramBoolUnknown,
        gas: .paramBoolUnknown,
        utilitiesIncluded: .paramBoolUnknown,
        salesDepartments: nil,
        isOutdated: false,
        uid: nil,
        share: nil,
        enrichedFields: nil,
        history: nil,
        commercialDescription: nil,
        villageInfo: nil,
        siteInfo: nil,
        vas: nil,
        queryContext: nil,
        chargeForCallsType: .noCharge,
        yandexRent: .paramBoolUnknown,
        userNote: nil,
        virtualTours: nil,
        offerChatType: .unknown,
        hasPaidCalls: .paramBoolUnknown
    )

    private lazy var anyOfferSearchService = AnyOfferSearchServiceMock(offer: self.offer)
}
