//
//  AppStateMocks.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 25.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREModel
import YREModelObjc
import YREAppState
import YRELegacyFiltersCore
import verticalios
import XCTest
import YREFiltersModel

// MARK: - YREAuthStateReader
final class AuthStateReaderMock: NSObject, YREAuthStateReader {
    var login: String? = nil
    var privateKey: String = "mock"
    var isAuthorized: Bool = false
    var autologinAttempted: Bool = false
    var token: String? = "testToken"
    var uuid: String? = "testUUID"
}

// MARK: - YREAuthStateWriter
final class AuthStateWriterMock: NSObject, YREAuthStateWriter {
    func updateToken(_ token: String?) {}

    func updateLogin(_ login: String?) {}

    func updateUUID(_ uuid: String?) {}

    func updateAutologinAttempted(_ autologinAttempted: Bool) {}

    var token: String? { nil }

    var login: String? { nil }

    var uuid: String? { nil }

    var privateKey: String { "mock" }

    var isAuthorized: Bool { false }

    var autologinAttempted: Bool { true }
}

// MARK: - YREUserDataWriter
final class UserDataWriterMock: NSObject, YREUserDataWriter {
    func performUserDataChangesAndNotify(_ changes: @escaping (YREUserDataWriterContext) -> Void) {}

    var userID: NSNumber?

    var userDisplayName: String?

    var userAvatarURL: String?

    var userType: UserType = .unknown

    var userPaymentType: UserPaymentType = .unknown

    var userOffersCount: NSNumber?

    var isMosRuAvailable: ConstantParamBool = .paramBoolFalse

    var mosRuTrustStatus: MosRuTrustStatus = .unknown

    var mosRuConnectionState: MosRuConnectionState = .idle

    var communicationChannelType: CommunicationChannelType = .onlyPhone

    var isNumberRedirectEnabled: ConstantParamBool = .paramBoolFalse

    var hasSelectedPhones: ConstantParamBool = .paramBoolFalse
}

// MARK: - YREOfferListStateWriter
final class OfferListStateWriterMock: NSObject, YREOfferListStateWriter {
    func updateShouldDisplayNotGrannysBanner(_ shouldDisplayNotGrannysBanner: Bool) {}
    func updateYandexRentBannerHidden(byUser isYandexRentBannerHiddenByUser: Bool) {}

    var shouldDisplayNotGrannysBanner: Bool { false }
    var isYandexRentBannerHiddenByUser: Bool { false }
}

// MARK: - YREUserOfferSettingsWriter
final class UserOfferSettingsWriterMock: NSObject, YREUserOfferSettingsWriter {
    func updateDraftSearchTypeFilterSnapshot(_ draftSearchTypeFilterSnapshot: YVFilterStateProtocol?) {}

    func updateDraftActionCategoryFilterSnapshot(_ draftActionCategoryFilterSnapshot: YVFilterStateProtocol?) {}

    func updateDraftLocation(_ draftLocation: YREOfferAddressProtocol?) {}

    var draftSearchTypeFilterSnapshot: YVFilterStateProtocol?
    var draftActionCategoryFilterSnapshot: YVFilterStateProtocol?
    var draftLocation: YREOfferAddressProtocol?
}

// MARK: - YREAppSessionStateReader
final class AppSessionStateReaderMock: NSObject, YREAppSessionStateReader {
    var firstLaunchDate: Date? { nil }
    var isFirstLaunch: Bool { false }
    var launchCount: UInt { 1 }
    var sessionStartDate: Date? { nil }
    var lastDeepLinkInfo: DeepLinkInfo? { nil }
    var appVersion: UInt { 0 }
    var utmLink: URL? { nil }
}

// MARK: - YRENavigationStateWriter
final class NavigationStateWriterMock: NSObject, YRENavigationStateWriter {
    var mainListOpened: Bool { true }
    var lastSelectedTab: TabBarItemType { .search }
    var lastSelectedSubscriptionScreen: SubscriptionsContainerScreenType { .favorites }
    var lastSelectedCommunicationScreen: CommunicationContainerScreenType { .calls }
    var shouldDisplayEmbeddedMainFilters: Bool { true }
    var pushNotificationIntroWasShown: Bool { true }
    var startupPromoWasShown: Bool { true }
    var houseUtilitiesPromoWasShown: Bool { true }
    var houseUtilitiesOwnerPromoWasShown: Bool { true }
    var savedSearchesWidgetPromoWasShown: Bool { true }
    var savedSearchesWidgetPromoBannerWasHidden: Bool { true }
    var userOffersOverQuotaAlertMayBeShown: Bool { false }
    var userOffersOverQuotaAlertShownAt: Date? { nil }

    func updateMainListOpened(_ mainListOpened: Bool) {}
    func updateLastSelectedTab(_ lastSelectedTab: TabBarItemType) {}
    func updateLastSelectedSubscriptionScreen(_ lastSelectedSubscriptionScreen: SubscriptionsContainerScreenType) {}
    func updateLastSelectedCommunicationScreen(_ lastSelectedCommunicationScreen: CommunicationContainerScreenType) {}
    func updateShouldDisplayEmbeddedMainFilters(_ shouldDisplayEmbeddedMainFilters: Bool) {}
    func updatePushNotificationIntroWasShown(_ pushNotificationIntroWasShown: Bool) {}
    func updateStartupPromoWasShown(_ startupPromoWasShown: Bool) {}
    func updateHouseUtilitiesTenantPromoWasShown(_ houseUtilitiesTenantPromoWasShown: Bool) {}
    func updateHouseUtilitiesOwnerPromoWasShown(_ houseUtilitiesOwnerPromoWasShown: Bool) {}
    func updateSavedSearchesWidgetPromoWasShown(_ savedSearchesWidgetPromoWasShown: Bool) {}
    func updateSavedSearchesWidgetPromoBannerWasHidden(_ savedSearchesWidgetPromoBannerWasHidden: Bool) {}
    func updateUserOffersOverQuotaAlertMayBeShown(_ userOffersOverQuotaAlertMayBeShown: Bool) {}
    func updateUserOffersOverQuotaAlertShown(at userOffersOverQuotaAlertShownAt: Date?) {}
}

// MARK: - YRESearchStateWriter
final class SearchStateWriterMock: NSObject, YRESearchStateWriter {
    var search: YRESearchProtocol {
        let search = YREMutableSearch()
        guard let filterRoot = FilterRootFactory().makeFilterRoot(
            with: .buy,
            category: .apartment
        ) else {
            XCTFail("Can not create filter root.")
            return YREMutableSearch()
        }

        let searchTypeFilter = filterRoot.searchTypeFilter

        guard let actionCategoryFilter = filterRoot.actionCategoryFilter as? YREBuyApartamentFilter else {
            XCTFail("Can not convert actionCategoryFilter to \(YREBuyApartamentFilter.self)")
            return YREMutableSearch()
        }

        let searchTypeFilterState = searchTypeFilter.stateSerializer.state(from: searchTypeFilter)
        let actionCategoryFilterState = actionCategoryFilter.stateSerializer.state(from: actionCategoryFilter)

        search.lastSearchTypeFilterSnapshot = searchTypeFilterState
        search.lastActionCategoryFilterSnapshot = actionCategoryFilterState
        search.geoIntent = PolygonGeoIntent(points: [])
        search.sortType = .byPrice
        search.viewPort = .init(minLatitude: 50, minLongitude: 50, maxLatitude: 60, maxLongitude: 60)
        return search
    }

    var regionConfigurationUpdatedAt: Date? {
        nil
    }

    func updateSearch(_ search: YRESearchProtocol!) {}
    func updateLastSearchTypeFilterSnapshot(_ lastFilterSnapshot: YVFilterStateProtocol!) {}
    func updateLastActionCategoryFilterSnapshot(_ lastFilterSnapshot: YVFilterStateProtocol!) {}
    func updateSerializedFilter(_ serializedFilter: [AnyHashable: Any]!) {}
    func updateGeoIntent(_ geoIntent: YREGeoIntentProtocol!) {}
    func updateRegion(_ region: YRERegion!) {}
    func updateRegionConfiguration(_ regionConfiguration: YRERegionConfiguration!) {}
    func update(_ sortType: SortType) {}
    func updateViewPort(_ viewPort: YRECoordinateRegion!) {}
    func updateRegionConfigurationUpdated(at date: Date!) {}
}

// MARK: - YREConnectionSettings & YREConnectionSettingsReader
final class ConnectionSettingsMock: NSObject, YREConnectionSettings, YREConnectionSettingsReader {
    var backendEndpointType: BackendEndpointType { .dev }

    var mobileFrontendEndpointType: FrontendEndpointType { .dev }

    var desktopFrontendEndpointType: FrontendEndpointType { .dev }

    var productionBackendServerEndpoint: String { "https://" }

    var developmentBackendServerEndpoint: String { "https://" }

    var localBackendServerEndpoint: String { "https://" }

    var productionMobileFrontendServerEndpoint: String { "https://" }

    var developmentMobileFrontendServerEndpoint: String { "https://" }

    var localMobileFrontendServerEndpoint: String { "https://" }

    var productionDesktopFrontendServerEndpoint: String { "https://" }

    var developmentDesktopFrontendServerEndpoint: String { "https://" }

    var localDesktopFrontendServerEndpoint: String { "https://" }

    var prodRentDesktopFrontendServerEndpoint: String { "https://" }

    var devRentDesktopFrontendServerEndpoint: String { "https://" }

    var localRentDesktopFrontendServerEndpoint: String { "https://" }

    var devGraphQLBackendServerEndpoint: String { "https://" }
}

// MARK: - YREMapSettingsWriter
final class MapSettingsWriterMock: NSObject, YREMapSettingsWriter {
    var visibleLayerType: MapLayerType { .scheme }
    var selectedHeatmapType: HeatmapType { .ecology }
    var shouldDisplayHeatmapsSwitcher: Bool { true }
    var isRentPromoWasFinished: Bool { false }
    var rentPromoShowingCount: UInt { 0 }

    func updateVisibleLayerType(_ visibleLayerType: MapLayerType) {}
    func updateSelectedHeatmapType(_ selectedHeatmapType: HeatmapType) {}
    func updateShouldDisplayHeatmapsSwitcher(_ shouldDisplay: Bool) {}
    func updateIsRentPromoWasFinished(_ rentPromoWasFinished: Bool) {}
    func updateRentPromoShowingCount(_ rentPromoShowingCount: UInt) {}
}

// MARK: - YREGeoRouterSettingsWriter
final class GeoRouterSettingsWriterMock: NSObject, YREGeoRouterSettingsWriter {
    func update(_ routerType: GeoRouterType) {}

    func updateRoutingTargetObject(_ object: GeoSearchObjectProtocol?) {}

    func updateWizardWasShown(_ wasShown: Bool) {}

    var routerType: GeoRouterType { .auto }

    var routingTargetObject: GeoSearchObjectProtocol?

    var wizardWasShown: Bool { false }
}
