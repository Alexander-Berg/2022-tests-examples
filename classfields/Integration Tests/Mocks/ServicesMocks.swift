//
//  ServicesMocks.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 25.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import UIKit
import YREServiceLayer
import YREServiceLayerBase
import YREServiceInterfaces
import YREAnalyticsGateway
import YREModel
import YREModelObjc
import YRECoreUtils
import YREAppState
import YREDelegateCollection
@testable import YREWeb
import YREAnalytics

let httpHeaders = HTTPHeaders(
    metricaDeviceIDFallback: "Fallback",
    tamperProvider: TamperProviderMock(),
    authProvider: HttpAuthorizationProviderMock()
)

let webServices = YREWebServices(
    connectionSettings: ConnectionSettingsMock(),
    authProvider: HttpAuthorizationProviderMock(),
    tamperProvider: TamperProviderMock(),
    appEnvironmentManager: AppEnvironmentManager(),
    paidLoopInfoProvider: PaidLoopInfoProviderMock(),
    metricaDeviceIDFallback: "Fallback",
    httpHeaders: httpHeaders
)
let webServicesFactory = WebServicesFactory(webServices: webServices)

// MARK: - AdsService
final class AdsServiceMock: AdsService {
    func activate() {}

    func fetchItems(request: FetchRequest, completion: @escaping FetchCompletion) -> AdsService.FetchRequestToken? {
        nil
    }

    func cancel(by token: FetchRequestToken) {}
}

// MARK: - AnyOfferSearchService
final class AnyOfferSearchServiceMock {
    init(offer: YREOffer) {
        self.offer = offer
    }

    private let offer: YREOffer
}

extension AnyOfferSearchServiceMock: AnyOfferSearchService {
    func getSiteSnippetWithSiteID(
        _ siteID: String!,
        parameters: [String: AnyObject]?,
        completionBlock: YREGetSiteSnippetCompletionBlock?
    ) -> YREDataLayerTaskProtocol? {
        YREDataLayerTask()
    }

    func getVillageSnippetWithVillageID(
        _ villageID: String!,
        parameters: [String: AnyObject]?,
        completionBlock: YREGetVillageSnippetCompletionBlock?
    ) -> YREDataLayerTaskProtocol? {
        YREDataLayerTask()
    }

    func getOffer(
        offerID: String,
        parameters: [String: AnyObject]?,
        completionBlock: YREOfferByIDCompletionBlock?
    ) -> YREDataLayerTaskProtocol {
        let task = YREDataLayerTask()
        completionBlock?(task, offer, false)
        return task
    }

    func getSite(
        siteID: String,
        parameters: [String: AnyObject]?,
        completionBlock: YRESiteByIDCompletionBlock?
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func getSimilarOffers(
        offerID: String,
        completion: @escaping (YREDataLayerTaskProtocol?, [YREOffer]?) -> Void
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func getSimilarSites(
        siteID: String,
        completion: @escaping (YREDataLayerTaskProtocol?, [YRESiteSnippet]?) -> Void
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func getOfferSnippets(
        parameters: [String: AnyObject]?,
        from: Date?,
        to: Date?,
        page: Int,
        pageSize: Int,
        completionBlock: YREGetOfferSnippetsCompletionBlock?
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func getOfferSnippetsCount(
        parameters: [String: AnyObject]?,
        from: Date?,
        to: Date?,
        isSearchExtendingEnabled: Bool,
        completionBlock: YREGetOfferSnippetsCountCompletionBlock?
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func getYandexRentBoundingBox(
        rgid: UInt,
        completion: YREGetBoundingBoxCompletion?
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }
}

// MARK: - YREViewedOfferService
final class ViewedOfferServiceMock: NSObject, YREViewedOfferService {
    var itemsChangedRegistrar: YREDelegateCollectionRegistrar<YREViewedHistoryItemsChangedObserver> {
        .init()
    }

    var listChangedRegistrar: YREDelegateCollectionRegistrar<YREViewedHistoryListChangedObserver> {
        .init()
    }

    func isOfferViewed(forID offerID: String) -> Bool {
        false
    }

    func isSiteViewed(forID siteID: String) -> Bool {
        false
    }

    func isVillageViewed(forID villageID: String) -> Bool {
        false
    }

    func isMultihouseViewed(forID multihouseID: String) -> Bool {
        false
    }

    func abstractOfferIsViewed(_ offer: YREAbstractOffer) -> Bool {
        false
    }

    func markAbstractOfferViewed(_ offer: YREAbstractOffer) {}

    func markMultihouseViewed(forID multihouseID: String) {}

    func clearViewStatus(forOffersOlderThan date: Date) {}
}

// MARK: - YRECallHistoryService
final class CallHistoryServiceMock: NSObject, YRECallHistoryService {
    var itemsChangedRegistrar: YREDelegateCollectionRegistrar<YRECallHistoryItemsChangedObserver> { .init() }

    var listChangedRegistrar: YREDelegateCollectionRegistrar<YRECallHistoryListChangedObserver> { .init() }

    func addItem(toCallHistory abstractOffer: YREAbstractOffer, withCompletionBlock completionBlock: YRETaskStateCompletionBlock? = nil) {
        let taskState = YRETaskState.succeeded()
        completionBlock?(taskState)
    }

    func isItemAdded(_ offer: YREAbstractOffer) -> Bool {
        false
    }

    func updateCallHistoryItems(
        forItems items: [YREAbstractOffer],
        withCompletionBlock completionBlock: YRETaskStateCompletionBlock? = nil
    ) {
        let taskState = YRETaskState.succeeded()
        completionBlock?(taskState)
    }

    func setCallHistoryItemOutdatedForID(
        _ identifier: String,
        itemType: DataLayerCallHistoryItemType,
        withCompletionBlock completionBlock: YRETaskStateCompletionBlock? = nil
    ) {
        let taskState = YRETaskState.succeeded()
        completionBlock?(taskState)
    }

    func removeOldCallHistoryItems(_ completionBlock: YRETaskStateCompletionBlock? = nil) {
        let taskState = YRETaskState.succeeded()
        completionBlock?(taskState)
    }

    func callHistoryListDataSource() -> ListDataSourceProtocol? {
        nil
    }

    func reloadAllCallHistoryItemsIfNeeded(_ completionBlock: YRETaskStateCompletionBlock? = nil) {
        let taskState = YRETaskState.succeeded()
        completionBlock?(taskState)
    }

    func reloadAllCallHistoryItems(completionBlock: YRETaskStateCompletionBlock? = nil) {
        let taskState = YRETaskState.succeeded()
        completionBlock?(taskState)
    }
}

// MARK: - YREFavoriteOffersService
final class FavoriteOffersServiceMock: NSObject, YREFavoriteOffersService {
    var webServices: YREWebServicesFactoryProtocol { webServicesFactory }

    var listChangedRegistrar: YREDelegateCollectionRegistrar<YREFavoriteListChangedObserver> { .init() }

    var itemsChangedRegistrar: YREDelegateCollectionRegistrar<YREFavoriteItemsChangedObserver> { .init() }

    var syncingStateRegistrar: YREDelegateCollectionRegistrar<YREFavoritesSyncingStateObserver> { .init() }

    func activate() {}

    func synchronize() {}

    func isSyncing() -> Bool {
        false
    }

    func addFavoritesItem(withAnyOfferID anyOfferID: String, itemType: FavoritesItemType) {}

    func addFavoritesItems(withIDs ids: [String], itemType: FavoritesItemType) {}

    func removeFavoritesItem(withAnyOfferID offerID: String, itemType: FavoritesItemType) {}

    func removeFavoritesItems(withIDs ids: [String], itemType: FavoritesItemType) {}

    func isOfferWithID(inFavorites anyOfferID: String, itemType: FavoritesItemType) -> Bool {
        false
    }

    func isAbstractOffer(inFavorites abstractOffer: YREAbstractOffer) -> Bool {
        false
    }

    func currentFavoriteItemsCount() -> UInt {
        0
    }

    func maximumFavoriteItemsCount() -> UInt {
        100
    }

    func canAddToFavorites(withCount itemsCountToAdd: Int) -> Bool {
        false
    }

    func obtainFavoriteIDs(completionBlock: @escaping YREFavoriteIDsListCompletionBlock) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func getFavoriteOffers(
        byIDs offerIDs: [String],
        forFavoritesType favoritesType: FavoritesItemType,
        withHidden: Bool,
        withCompletionBlock completionBlock: YREFavoriteOffersListCompletionBlock? = nil
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func patchFavorites(
        withAddedOfferIDs addedIDs: [Any],
        removedOfferIDs removedIDs: [Any],
        completionBlock: YREFavoritesSyncCompletionBlock? = nil
    ) -> YREDataLayerTaskProtocol {
        YREDataLayerTask()
    }

    func fetchFavoriteItemsCount(_ completionBlock: @escaping YREFavoriteObjectsCountBlock) {}
}

// MARK: - Service Mocks

final class PhonesServiceMock: PhonesServiceProtocol {
    func getPhones(
        abstractOffer: YREAbstractOffer,
        completion: @escaping YREGetPhonesCompletionBlock
    ) -> YREDataLayerTaskProtocol {
        return YREDataLayerTask()
    }
}

final class SiteCallbackServiceMock: SiteCallbackServiceProtocol {
    @discardableResult
    func requestSiteCallback(
        phone: String,
        siteID: String,
        utmLink: String?,
        model: SiteCallbackServiceModel,
        screenReferrer: ScreenReferrer,
        completionBlock: SiteCallbackServiceCompletion?
    ) -> YREDataLayerTaskProtocol {
        return YREDataLayerTask()
    }
}

final class YaRentMarketingSubscriptionsServiceMock: YaRentMarketingSubscriptionsServiceProtocol {
    func fetchSubscriptionStatus(
        completion: @escaping (TaskResult<YaRentMarketingSubscriptionState, CommonServiceError>) -> Void
    ) -> DataTask<YaRentMarketingSubscriptionState, CommonServiceError> {
        return .idle()
    }

    func subscribeToNews(
        completion: @escaping (TaskResult<Void, CommonServiceError>) -> Void
    ) -> DataTask<Void, CommonServiceError> {
        return .idle()
    }
}

// MARK: - ServiceFactoryProtocol

final class ServiceFactoryMock: ServiceFactoryProtocol {
    func makePhonesService() -> PhonesServiceProtocol {
        return PhonesServiceMock()
    }

    func makeSiteCallbackService() -> SiteCallbackServiceProtocol {
        return SiteCallbackServiceMock()
    }

    func makeRentMarketingSubscriptionsService() -> YaRentMarketingSubscriptionsServiceProtocol {
        return YaRentMarketingSubscriptionsServiceMock()
    }

    func makePromoManagementService() -> PromoManagementServiceProtocol {
        return PromoManagementServiceMock()
    }

    func makeInventoryService() -> InventoryServiceProtocol {
        return InventoryServiceMock()
    }

    func makeRentContractService() -> RentContractServiceProtocol {
        return RentContractServiceMock()
    }

    func makeFilesService() -> FilesServiceProtocol {
        return FilesServiceMock()
    }

    func makeRentShowingsService() -> RentShowingsServiceProtocol {
        return RentShowingsServiceMock()
    }
}

// MARK: - AuthStateObservable
final class AuthStateObservableMock: NSObject, AuthStateObservable {
    final class AuthStateObservation: NSObject, StateObservationProtocol {
        func invalidate() {
            // do nothing
        }
    }

    var authState = AuthStateReaderMock()

    var authStateReader: YREAuthStateReader { self.authState }
    var observer: AuthStateObserver? = nil

    func observe(by observer: AuthStateObserver) -> StateObservationProtocol {
        self.observer = observer
        return AuthStateObservation()
    }

    func notifyObserver() {
        self.observer?.authStateObservableDidUpdate(self)
    }
}

// MARK: - PushNotificationsGatewayProtocol & PushNotificationsHolder
final class PushNotificationGatewayMock: PushNotificationsGatewayProtocol, PushNotificationsHolder {
    func hasPayloadToProcess() -> Bool {
        false
    }

    func popPayload() -> PushNotificationRawPayloadWrapperProtocol? {
        nil
    }

    weak var delegate: PushNotificationsGatewayDelegate?

    var holder: PushNotificationsHolder { self }

    func checkPushNotificationsPermissionsState(_ completion: @escaping PushPermissionsCheckingCompletion) {}

    func onApplicationDidLaunch(withOptions launchOptions: [AnyHashable: Any]?) {}

    func onApplicationDidBecomeActive() {}

    func onApplicationWillBecomeInactive() {}

    func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {}

    func didFailToRegisterForRemoteNotifications(withError error: Error) {}

    func didReceiveRemoteNotification(
        userInfo: [AnyHashable: Any]?,
        fetchCompletionHandler: ((UIBackgroundFetchResult) -> Void)?
    ) {}
}

// MARK: - FirebaseGatewayProtocol
final class FirebaseGatewayMock: FirebaseGatewayProtocol {
    func activate() {}

    func fetchInstanceID(completion: @escaping (String?) -> Void) {}

    func reportEvent(name: String, parameters: [String: Any]?) {}

    func setUserID(_ userID: String?) {}

    func setUserProperty(name: String, value: String?) {}

    func fetchRemoteConfigWithRemoteKeys(
        _ remoteKeys: [RemoteKey],
        completionHandler: @escaping ([RemoteKey: String]?) -> Void
    ) {}

    func appDidReceiveMessage(_ message: [AnyHashable: Any]) {}
}

// MARK: - AdjustInfoProvider
final class AdjustInfoProviderMock: NSObject, AdjustInfoProvider {
    var adjustID: String? = nil
}

// MARK: - YRESavedSearchCountManager
final class SavedSearchUpdatesCountManagerMock: NSObject, YRESavedSearchCountManager {
    var lastUpdatesCount: UInt { 0 }
    var lastSearchesCount: UInt { 0 }
    var registrar: YREDelegateCollectionRegistrar<YRESavedSearchCountObserver>? { nil }
}

// MARK: - AnalyticsProtocol
final class AnalyticsReporterMock: NSObject, AnalyticsProtocol {
    func report(_ event: YREAnalyticsEvent) {
    }
}

// MARK: - PromoManagementServiceProtocol
final class PromoManagementServiceMock: PromoManagementServiceProtocol {
    var availablePromosToShow: Set<Promo> = .init()

    func markPromoUnavailableForSession(_ promo: Promo) {}
    func markPromoWasShown(_ promo: Promo) {}
}

// MARK: - InventoryServiceProtocol
final class InventoryServiceMock: InventoryServiceProtocol {
    func getInventory(ownerRequestID: String) async -> TaskResult<YaRentInventory, CommonServiceError> {
        return .cancelled
    }
    func updateInventory(
        ownerRequestID: String,
        inventory: YaRentInventory
    ) async -> TaskResult<YaRentInventory, EditInventoryError> {
        return .cancelled
    }

    func confirmInventory(
        ownerRequestID: String,
        sentSMSInfo: YaRentSentSmsInfo,
        codeFromSMS: String
    ) async -> TaskResult<Void, ConfirmationSMSError> {
        return .cancelled
    }

    func getConfirmationCode(
        ownerRequestID: String
    ) async -> TaskResult<YaRentSentSmsInfo, SendingSMSError> {
        return .cancelled
    }
}

// MARK: - RentContractServiceProtocol

final class RentContractServiceMock: RentContractServiceProtocol {
    func getContract(with contractId: String) async -> TaskResult<YaRentContractSummary, CommonServiceError> {
        return .cancelled
    }

    func askSigningSMSCode(for contractID: String) async -> TaskResult<YaRentSentSmsInfo, SendingSMSError> {
        return .cancelled
    }

    func signContract(
        contractID: String,
        sentSMSInfo: YaRentSentSmsInfo,
        codeFromSMS: String,
        acceptedTermsVersion: UInt32?
    ) async -> TaskResult<YaRentContractSignCodeResponse, ConfirmationSMSError> {
        return .cancelled
    }

    func requestChanges(
        contractID: String,
        comment: String
    ) async -> TaskResult<Void, InputChangesError> {
        return .cancelled
    }
}

// MARK: - FilesServiceMock

final class FilesServiceMock: FilesServiceProtocol {
    func getDownloadURL(identity: DownloadURLIdentity) async -> TaskResult<URL, FilesServiceError> {
        return .cancelled
    }
    
    func uploadRentImage(data: Data, identity: YaRentImageEntityIdentity) async -> TaskResult<YaRentImage, Error> {
        return .cancelled
    }
}

// MARK: - RentShowingsServiceMock
final class RentShowingsServiceMock: RentShowingsServiceProtocol {
    func getShowings() async -> TaskResult<[YaRentShowing], CommonServiceError> {
        return .cancelled
    }

    func getShowingCard(showingID: String) async -> TaskResult<YaRentShowingCard, YaRentShowingCardError> {
        return .cancelled
    }

    func fillTenantCheckInDate(
        showingID: String,
        date: String
    ) async -> TaskResult<Void, YaRentShowingCheckInDateError> {
        return .cancelled
    }
}
