//
//  WebServicesFactoryMock.swift
//  YREWeb-Unit-Tests
//
//  Created by Timur Guliamov on 09.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import Foundation
import YRECoreUtils
import YREWebModels
import YREFiltersModel
import YREModel
import YREModelObjc
import struct UIKit.CGFloat
import YREGraphQLModel
@testable import YREWeb

public final class WebServicesFactoryMock: WebServicesFactoryProtocol {
    public init() { }

    private(set) lazy var apiStatus = APIWebServiceMock()
    private(set) lazy var geoObjects = GeoObjectsWebServiceMock()
    private(set) lazy var search = SearchWebServiceMock()
    private(set) lazy var userOffers = UserOffersWebServiceMock()
    private(set) lazy var userOffersPayment = UserOfferProductPurchaseWebServiceMock()
    private(set) lazy var abuse = AbuseWebServiceMock()
    private(set) lazy var user = UserWebServiceMock()
    private(set) lazy var photos = PhotosWebServiceMock()
    private(set) lazy var favorites = FavoritesWebServiceMock()
    private(set) lazy var device = DeviceWebServiceMock()
    private(set) lazy var deepLink = DeepLinkWebServiceMock()
    private(set) lazy var heatmaps = HeatmapsWebServiceMock()
    private(set) lazy var regionInfo = RegionInfoWebServiceMock()
    private(set) lazy var schools = SchoolsWebServiceMock()
    private(set) lazy var authStateSender = AuthStateSenderWebServiceMock()
    private(set) lazy var archivalOffers = ArchivalOffersWebServiceMock()
    private(set) lazy var tags = TagsWebServiceMock()
    private(set) lazy var developers = DevelopersWebServiceMock()
    private(set) lazy var notificationSettings = NotificationSettingsWebServiceMock()
    private(set) lazy var villages = VillagesWebServiceMock()
    private(set) lazy var excerpts = ExcerptsWebServiceMock()
    private(set) lazy var geoSuggest = GeoSuggestWebServiceMock()
    private(set) lazy var savedSearch = SavedSearchWebServiceMock()
    private(set) lazy var bankCards = UserBankCardsWebServiceMock()
    private(set) lazy var offerStat = OfferStatWebServiceMock()
    private(set) lazy var chats = ChatsWebServiceMock()
    private(set) lazy var mortgage = MortgageWebServiceMock()
    private(set) lazy var banks = BankWebServiceMock()
    private(set) lazy var mosRuAuth = MosRuAuthWebServiceMock()
    private(set) lazy var eventLog = EventLogWebServiceMock()
    private(set) lazy var blogPosts = BlogPostsWebServiceMock()
    private(set) lazy var offerPlan = OfferPlanWebServiceMock()
    private(set) lazy var concierge = ConciergeWebServiceMock()
    private(set) lazy var personalization = PersonalizationWebServiceMock()
    private(set) lazy var siteCallback = SiteCallbackWebServiceMock()
    private(set) lazy var rent = RentWebServiceMock()
    private(set) lazy var rentFlats = RentFlatsWebServiceMock()
    private(set) lazy var rentShowings = RentShowingsWebServiceMock()
    private(set) lazy var rentCards = RentCardsWebServiceMock()
    private(set) lazy var searchExtending = SearchExtendingWebServiceMock()
    private(set) lazy var inAppService = InAppServiceWebServiceMock()
    private(set) lazy var houseUtilitiesService = HouseUtilitiesWebServiceMock()
    private(set) lazy var fileWebService = FilesWebServiceMock()
    private(set) lazy var priceStatisticsWebService = PriceStatisticsWebServiceMock()
    private(set) lazy var inventoryWebService = InventoryWebServiceMock()
    private(set) lazy var rentContractWebService = RentContractWebServiceMock()
    private(set) lazy var siteReviewsWebService = SiteReviewsWebServiceMock()
    private(set) lazy var rentDocumentsWebService = RentDocumentsWebServiceMock()

    public func makeApiStatus() -> APIWebServiceProtocol {
        return self.apiStatus
    }

    public func makeGeoObjects() -> GeoObjectsWebServiceProtocol {
        return self.geoObjects
    }

    public func makeSearch() -> SearchWebServiceProtocol {
        return self.search
    }

    public func makeUserOffers() -> UserOffersWebServiceProtocol {
        return self.userOffers
    }

    public func makeUserOffersPayment() -> UserOfferProductPurchaseWebServiceProtocol {
        return self.userOffersPayment
    }

    public func makeAbuse() -> AbuseWebServiceProtocol {
        return self.abuse
    }

    public func makeUser() -> UserWebServiceProtocol {
        return self.user
    }

    public func makePhotos() -> PhotosWebServiceProtocol {
        return self.photos
    }

    public func makeFavorites() -> FavoritesWebServiceProtocol {
        return self.favorites
    }

    public func makeDevice() -> DeviceWebServiceProtocol {
        return self.device
    }

    public func makeDeepLink() -> DeepLinkWebServiceProtocol {
        return self.deepLink
    }

    public func makeHeatmaps() -> HeatmapsWebServiceProtocol {
        return self.heatmaps
    }

    public func makeRegionInfo() -> RegionInfoWebServiceProtocol {
        return self.regionInfo
    }

    public func makeSchools() -> SchoolsWebServiceProtocol {
        return self.schools
    }

    public func makeAuthStateSender() -> AuthStateSenderWebServiceProtocol {
        return self.authStateSender
    }

    public func makeArchivalOffers() -> ArchivalOffersWebServiceProtocol {
        return self.archivalOffers
    }

    public func makeTags() -> TagsWebServiceProtocol {
        return self.tags
    }

    public func makeDevelopers() -> DevelopersWebServiceProtocol {
        return self.developers
    }

    public func makeNotificationSettings() -> NotificationSettingsWebServiceProtocol {
        return self.notificationSettings
    }

    public func makeVillages() -> VillagesWebServiceProtocol {
        return self.villages
    }

    public func makeExcerpts() -> ExcerptsWebServiceProtocol {
        return self.excerpts
    }

    public func makeGeoSuggest() -> GeoSuggestWebServiceProtocol {
        return self.geoSuggest
    }

    public func makeSavedSearch() -> SavedSearchWebServiceProtocol {
        return self.savedSearch
    }

    public func makeBankCards() -> UserBankCardsWebServiceProtocol {
        return self.bankCards
    }

    public func makeOfferStat() -> OfferStatWebServiceProtocol {
        return self.offerStat
    }

    public func makeChats() -> ChatsWebServiceProtocol {
        return self.chats
    }

    public func makeMortgage() -> MortgageWebServiceProtocol {
        return self.mortgage
    }

    public func makeBanks() -> BankWebServiceProtocol {
        return self.banks
    }

    public func makeMosRuAuth() -> MosRuAuthWebServiceProtocol {
        return self.mosRuAuth
    }

    public func makeEventLog() -> EventLogWebServiceProtocol {
        return self.eventLog
    }

    public func makeBlogPosts() -> BlogPostsWebServiceProtocol {
        return self.blogPosts
    }

    public func makeOfferPlan() -> OfferPlanWebServiceProtocol {
        return self.offerPlan
    }

    public func makeConcierge() -> ConciergeWebServiceProtocol {
        return self.concierge
    }

    public func makePersonalization() -> PersonalizationWebServiceProtocol {
        return self.personalization
    }

    public func makeSiteCallback() -> SiteCallbackWebServiceProtocol {
        return self.siteCallback
    }

    public func makeRent() -> RentWebServiceProtocol {
        return self.rent
    }

    public func makeRentFlats() -> RentFlatsWebServiceProtocol {
        return self.rentFlats
    }

    public func makeRentShowings() -> RentShowingsWebServiceProtocol {
        return self.rentShowings
    }

    public func makeRentCards() -> RentCardsWebServiceProtocol {
        return self.rentCards
    }

    public func makeSearchExtending() -> SearchExtendingWebServiceProtocol {
        return self.searchExtending
    }

    public func makeInAppService() -> InAppServiceWebServiceProtocol {
        return self.inAppService
    }

    public func makeHouseUtilitiesService() -> HouseUtilitiesWebServiceProtocol {
        return self.houseUtilitiesService
    }

    public func makeFileWebService() -> FilesWebServiceProtocol {
        return self.fileWebService
    }

    public func makePriceStatisticsWebService() -> PriceStatisticsWebServiceProtocol {
        self.priceStatisticsWebService
    }

    public func makeInventoryWebService() -> InventoryWebServiceProtocol {
        self.inventoryWebService
    }

    public func makeRentContractWebService() -> RentContractWebServiceProtocol {
        self.rentContractWebService
    }

    public func makeSiteReviewsWebService() -> SiteReviewsWebServiceProtocol {
        self.siteReviewsWebService
    }

    public func makeRentDocumentsWebService() -> RentDocumentsWebServiceProtocol {
        self.rentDocumentsWebService
    }
}

final class APIWebServiceMock: NSObject, APIWebServiceProtocol {
    func getApiStatus(
        withCompletionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class GeoObjectsWebServiceMock: NSObject, GeoObjectsWebServiceProtocol {
    func getRegion(
        forLocation location: MDCoords2D?,
        geoIntent: YREGeoIntentProtocol?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getRegionList(
        text: String?,
        page: Int,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getGeoSuggest(
        forText text: String?,
        location: MDCoords2D?,
        resultSize: UInt,
        action: kYREFilterAction,
        category: kYREFilterCategory,
        objectType: NSNumber?,
        regionSearchParams: [String: AnyObject],
        entryTypes: [NSNumber],
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getAddressGeocoder(forAddress address: String?,
                            location: MDCoords2D?,
                            completionBlock: YREWebServicesCompletionBlock?) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getAddressGeocoder(forAddress address: String?,
                            location: MDCoords2D?,
                            category: UserOffersFilterCategory,
                            completionBlock: YREWebServicesCompletionBlock?) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class SearchWebServiceMock: NSObject, SearchWebServiceProtocol {
    func getClusteredPoints(
        withParameters parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getOffers(
        withParameters parameters: [String: AnyObject]?,
        from: Date?,
        to: Date?,
        countOnly: Bool,
        commutePolygonAndCountOnly: Bool,
        page: Int,
        pageSize: Int,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getOffersNumber(
        withParameters parameters: [String: AnyObject]?,
        from: Date?,
        to: Date?,
        isSearchExtendingEnabled: Bool,
        page: Int,
        pageSize: Int,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getSiteSnippet(
        withParameters parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getVillageSnippet(
        withParameters parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    var offerNotFoundMetaKey: String = ""

    func getOfferCard(
        withID id: String,
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    var siteNotFoundMetaKey: String = ""

    func getSiteCard(
        withID id: String,
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getBoundingBox(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getSimilarOffers(
        offerID: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getSimilarSites(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getOfferPins(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getSitePins(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getVillagePins(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getOfferPlacemarks(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getSitePlacemarks(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getVillagePlacemarks(
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getPhones(
        offerType: SearchWebService.AnyOfferType,
        anyOfferID: String,
        parameters: [String: AnyObject]?,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class UserOffersWebServiceMock: NSObject, UserOffersWebServiceProtocol {
    var offerNotFoundMetaKey: String = ""

    func getUserOffers(
        withParameters: [String: AnyObject]?,
        page: UInt,
        offersPerPage: UInt,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getUserOfferCard(
        withID id: String,
        parameters: [String: AnyObject]?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getViewsStat(
        forUserOfferID userOfferID: String,
        span: Int,
        timeZone: TimeZone,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getUserOfferPreview(
        withID id: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getUserOfferAsEdit(
        withID: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func createDraft(
        withCompletion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func createOffer(
        fromDraftWithID: String,
        offerDetails: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func validateOffer(
        withDetails: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateUserOffer(
        withID: String,
        offerDetails: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func deleteUserOffer(
        withID: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func changeUserOfferStatus(
        forUserOfferID: String,
        status: UserOffersWebServiceUserOfferStatus,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateUserOfferPhotos(
        forUserOfferID id: String,
        photos: [UserOfferPhotoProtocol],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateUserOfferPrice(
        forUserOfferID: String,
        price: YREPrice,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }
}

final class UserOfferProductPurchaseWebServiceMock: NSObject, UserOfferProductPurchaseWebServiceProtocol {
    func createPurchase(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func submitPayment(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getPurchaseState(
        purchaseId: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateProductRenewalStatus(
        forUserOfferID id: String,
        productType: UserOfferProductType,
        turnedOn: Bool,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class AbuseWebServiceMock: NSObject, AbuseWebServiceProtocol {
    func sendAbuse(
        withInfo info: YREAbuseInfo,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class UserWebServiceMock: NSObject, UserWebServiceProtocol {
    func getUser(
        withCompletionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateFields(
        fieldsBitmask: kYREUserField,
        fromUser: YREUserProtocol,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func addUserPhone(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func confirmUserPhone(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getEmail(
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class PhotosWebServiceMock: NSObject, PhotosWebServiceProtocol {
    func uploadPhotoFromFileAtURL(
        fileURL: URL,
        withCompletionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class FavoritesWebServiceMock: NSObject, FavoritesWebServiceProtocol {
    func getFavorites(
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func patchFavorites(
        withAddedOfferIDs: [String]?,
        removedOfferIDs: [String]?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func obtainSharingLink(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class DeviceWebServiceMock: NSObject, DeviceWebServiceProtocol {
    func registerPushToken(
        apnsPushToken: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendHelloParams(
        idfa: String?,
        metricaDeviceId: String?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func fetchRequiredAppFeatures(
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getWebSocketSign(
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getExperiments(
        parameters: [String: Any]?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class DeepLinkWebServiceMock: NSObject, DeepLinkWebServiceProtocol {
    var result: [String: Any]?

    func parseURL(
        deepLinkURL: String,
        withCompletionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        DispatchQueue.main.async {
            withCompletionBlock?(nil, YRETaskState.succeeded(), self.result, nil)
        }
        return YREURLWebTask()
    }
}

final class HeatmapsWebServiceMock: NSObject, HeatmapsWebServiceProtocol {
    func getHeatmapTile(
        heatmapName: String,
        tileX: UInt,
        tileY: UInt,
        tileZ: UInt,
        scale: Float,
        etag: String?,
        withCompletionBlock: YREHeatmapsWebServiceCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getSchoolsHeatmapTile(
        tileX: UInt,
        tileY: UInt,
        tileZ: UInt,
        scale: Float,
        etag: String?,
        withCompletionBlock: YREHeatmapsWebServiceCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class RegionInfoWebServiceMock: NSObject, RegionInfoWebServiceProtocol {
    func getRegionInfo(
        forRegionID regionID: Int,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class SchoolsWebServiceMock: NSObject, SchoolsWebServiceProtocol {
    func getSchoolOnMapList(
        leftLongitude: CGFloat,
        rightLongitude: CGFloat,
        topLatitude: CGFloat,
        bottomLatitude: CGFloat,
        maxItemsInResponse: Int,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class AuthStateSenderWebServiceMock: NSObject, AuthStateSenderWebServiceProtocol {
    func login(
        withCompletionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func logout(
        withCompletionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class ArchivalOffersWebServiceMock: NSObject, ArchivalOffersWebServiceProtocol {
    func getArchivalOffers(
        parameters: [String: AnyObject],
        completion: WebServiceCompletion?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class TagsWebServiceMock: NSObject, TagsWebServiceProtocol {
    func getSuggests(
        withText text: String?,
        action: kYREFilterAction,
        category: kYREFilterCategory,
        resultsCount: UInt,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class DevelopersWebServiceMock: NSObject, DevelopersWebServiceProtocol {
    func getSiteSuggests(
        withText text: String,
        rgid: UInt,
        resultsCount: UInt,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getVillageSuggests(
        withText text: String,
        rgid: UInt,
        resultsCount: UInt,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class NotificationSettingsWebServiceMock: NSObject, NotificationSettingsWebServiceProtocol {
    func getNotificationSettings(
        request: Realty_Api_NotificationConfigurationsRequest,
        completion: @escaping (
            TaskResult<Realty_Api_NotificationConfigurations, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREURLWebTask {
        return YREURLWebTask()
    }

    func setConfiguration(
        request: Realty_Api_NotificationConfigurationPatchRequest,
        completion: @escaping (
            TaskResult<Realty_Api_NotificationConfiguration, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREURLWebTask {
        return YREURLWebTask()
    }
}

final class VillagesWebServiceMock: NSObject, VillagesWebServiceProtocol {
    func getVillageCard(
        identifier: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getVillageOffers(
        identifier: String,
        parameters: [String: AnyObject]?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class ExcerptsWebServiceMock: NSObject, ExcerptsWebServiceProtocol {
    func getOfferFreeExcerpt(offerID: String, completion: YREWebServicesCompletionBlock?) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getPaidExcerptsList(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func makeAddressInfo(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getAddressInfo(
        addressInfoID: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func makePaidExcerpt(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func makeExcerptPurchase(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func submitPayment(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getPurchaseState(
        purchaseId: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class GeoSuggestWebServiceMock: NSObject, GeoSuggestWebServiceProtocol {
    func getGeoSuggest(
        parameters: [String: AnyObject]?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class SavedSearchWebServiceMock: NSObject, SavedSearchWebServiceProtocol {
    func createSavedSearch(
        identifier: String,
        parameters: [String: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func getSavedSearchesList(
        parameters: [String: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func deleteSavedSearch(
        identifier: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func createSubscriptionForSavedSearch(
        identifier: String,
        parameters: [String: Any],
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func removeSubscriptionForSavedSearch(
        identifier: String,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func visitSavedSearch(
        identifier: String,
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func hasNewOffers(
        completionBlock: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class UserBankCardsWebServiceMock: NSObject, UserBankCardsWebServiceProtocol {
    func getCards(
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func setPreferredCard(
        cddPanMask: String,
        paymentGate: String,
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func requestCardBinding(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getCardBindingStatus(
        paymentRequestID: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }

    func unbindCard(
        cddPanMask: String,
        paymentGate: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }
}

final class OfferStatWebServiceMock: NSObject, OfferStatWebServiceProtocol {
    func getSiteOfferStat(
        siteId: String,
        parameters: [String: AnyObject]?,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }
}

final class ChatsWebServiceMock: NSObject, ChatsWebServiceProtocol {
    func getAllRoomsList(
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getRoom(
        roomID: String,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getUserChatRoom(
        offerID: String,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getDevChatRoom(
        offerID: String,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getRoom(
        siteID: String,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getTechSupportRoom(
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getMessages(
        roomID: String,
        parameters: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) { }

    func sendMessage(
        parameters: [AnyHashable: Any],
        headers: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) { }

    func rateTechSupportPoll(
        hash: String,
        rating: Int,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func getImageUploadURL(
        roomID: String,
        parameters: [AnyHashable: Any],
        headers: [AnyHashable: Any],
        completion: YREWebServicesCompletionBlock?
    ) { }

    func uploadImage(
        jpegImage: Data,
        url: URL,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func checkHasUnread(
        completion: YREWebServicesCompletionBlock?
    ) { }

    func markAsRead(
        roomID: String,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func markChatRoom(
        roomID: String,
        action: String,
        completion: YREWebServicesCompletionBlock?
    ) { }

    func sendChatOpened(
        roomID: String,
        completion: @escaping (TaskResult<Realty_Api_RoomOpen, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol? {
        nil
    }
}

final class MortgageWebServiceMock: NSObject, MortgageWebServiceProtocol {
    func getCalculatorParameters(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getMortgagePrograms(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendForm(
        _ request: Realty_Mortgage_MortgageDemand,
        completion: @escaping (TaskResult<Realty_Mortgage_MortgageDemandRespInfo, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendConfirmationCode(
        _ request: Realty_Mortgage_MortgageDemandCommit,
        completion: @escaping (TaskResult<Void, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class BankWebServiceMock: NSObject, BankWebServiceProtocol {
    func getBanks(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class MosRuAuthWebServiceMock: NSObject, MosRuAuthWebServiceProtocol {
    func getAuthURL(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func setAuthResults(
        parameters: [String: AnyObject],
        taskID: String,
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func unlinkAccount(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class EventLogWebServiceMock: NSObject, EventLogWebServiceProtocol {
    func log(
        parameters: [String: Any],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class BlogPostsWebServiceMock: NSObject, BlogPostsWebServiceProtocol {
    func getPosts(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class OfferPlanWebServiceMock: NSObject, OfferPlanWebServiceProtocol {
    func getOfferPlans(
        siteID: String,
        parameters: [String: AnyObject]?,
        completion: @escaping YREWebServicesCompletionBlock
    ) -> YREWebTaskProtocol? {
        return YREURLWebTask()
    }
}

final class ConciergeWebServiceMock: NSObject, ConciergeWebServiceProtocol {
    func request(
        parameters: [String: AnyObject],
        completion: @escaping YREWebServicesCompletionBlock
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class PersonalizationWebServiceMock: NSObject, PersonalizationWebServiceProtocol {
    func upsertNote(
        offerID: String,
        note: String,
        with completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func deleteNote(
        offerID: String,
        with completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func hideOffer(
        _ offerID: String,
        with completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class SiteCallbackWebServiceMock: NSObject, SiteCallbackWebServiceProtocol {
    var conflictMetaKey: String = ""

    func requestSiteCallback(
        parameters: [String: AnyObject],
        completion: @escaping YREWebServicesCompletionBlock
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class RentWebServiceMock: NSObject, RentWebServiceProtocol {
    func isPointInsideRent(
        _ request: Rent_IsPointRent_GetRequest,
        completion: @escaping (TaskResult<Rent_IsPointRent_GetResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getUser(
        _ request: Rent_User_GetRequest,
        completion: @escaping (TaskResult<Rent_User_GetResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateUser(
        _ request: Rent_User_PatchRequest,
        completion: @escaping (TaskResult<Rent_User_PatchResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendNetPromoterScore(
        _ request: Realty_Rent_Api_SendNetPromoterScoreRequest
    ) async -> TaskResult<Void, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func calcRentPrice(
        flatID: String,
        request: Realty_Rent_Api_PostCalcRentPriceHandle.Request
    ) async -> TaskResult<Realty_Rent_Api_PostCalcRentPriceHandle.ResponseData, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func changeRentPrice(
        flatID: String,
        request: Realty_Rent_Api_PutRentPriceHandle.Request
    ) async -> TaskResult<Void, WebServiceError<YREFrontendError>> {
        return .cancelled
    }
}

final class RentFlatsWebServiceMock: NSObject, RentFlatsWebServiceProtocol {
    func getFlat(
        _ request: Rent_User_Flat_GetRequest,
        completion: @escaping (TaskResult<Rent_User_Flat_GetResponse, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getFlats(
        _ request: Rent_User_Flats_GetRequest,
        completion: @escaping (TaskResult<Rent_User_Flats_GetResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getFlatPayment(
        _ request: Rent_User_Flats_Payments_GetRequest,
        completion: @escaping (
            _ result: TaskResult<Rent_User_Flats_Payments_GetResponse, Swift.Error>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func createUserFlatsPayments(
        _ request: Rent_User_Flats_Payments_Init_PostRequest,
        completion: @escaping (
            TaskResult<Rent_User_Flats_Payments_Init_PostResponse, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getFlatDraft(
        completion: @escaping (TaskResult<Realty_Rent_Api_GetFlatDraftResponse, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateFlatDraft(
        _ request: Realty_Rent_Api_UpdateFlatDraftRequest,
        completion: @escaping (TaskResult<Realty_Rent_Api_UpdateFlatDraftResponse, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendSMS(
        flatID: String,
        completion: @escaping (TaskResult<Realty_Rent_Api_SendSmsForFlatConfirmationResponse, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func confirmFlatDraft(
        _ request: Realty_Rent_Api_ConfirmFlatRequest,
        flatID: String,
        completion: @escaping (TaskResult<Void, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func submitFlatPhotos(
        _ request: Realty_Rent_Api_UpdateFlatRequest,
        flatID: String,
        completion: @escaping (
            _ result: TaskResult<Realty_Rent_Api_UpdateFlatResponse, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func uploadFlatImage(
        jpegData: Data,
        url: URL,
        completion: @escaping (TaskResult<Files_Uploader_Image, Swift.Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendSMS(
        completion: @escaping (
            _ result: TaskResult<Realty_Rent_Api_SendSmsForFlatConfirmationResponse, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func confirmFlatDraft(
        _ request: Realty_Rent_Api_ConfirmFlatRequest,
        completion: @escaping (
            _ result: TaskResult<Void, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class RentShowingsWebServiceMock: NSObject, RentShowingsWebServiceProtocol {
    func getUserShowing(
        showingID: String
    ) async -> TaskResult<GetShowingQuery.Data.Showing, GraphQLWebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func getUserShowings() async -> TaskResult<Realty_Rent_Api_UserShowingsResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func fillTenantCheckInDate(
        showingID: String,
        request: Realty_Rent_Api_FillOutTenantCheckInDateRequest
    ) async -> TaskResult<Void, WebServiceError<YREFrontendError>> {
        return .cancelled
    }
}

final class RentCardsWebServiceMock: NSObject, RentCardsWebServiceProtocol {
    func getCards(
        _ request: Rent_User_Cards_GetRequest,
        completion: @escaping (TaskResult<Rent_User_Cards_GetResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func bindCard(
        _ request: Rent_User_Cards_PostRequest,
        completion: @escaping (TaskResult<Rent_User_Cards_PostResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func unbindCard(
        _ request: Rent_User_Cards_DeleteRequest,
        completion: @escaping (TaskResult<Rent_User_Cards_DeleteResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func updateCardsStatus(
        _ request: Rent_User_Cards_UpdateStatus_PostRequest,
        completion: @escaping (TaskResult<Void, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class SearchExtendingWebServiceMock: NSObject, SearchExtendingWebServiceProtocol {
    func getExtendFilters(
        parameters: [String: AnyObject],
        completion: YREWebServicesCompletionBlock?
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}


final class InAppServiceWebServiceMock: NSObject, InAppServiceWebServiceProtocol {
    func getServiceInfo(
        _ request: Service_Info_GetRequest,
        completion: @escaping (TaskResult<Realty_Api_Service_ServiceInfo, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }
}

final class HouseUtilitiesWebServiceMock: NSObject, HouseUtilitiesWebServiceProtocol {
    func legacyGetPeriodWithWithInfo(
        _ request: Rent_HouseServices_Periods_PeriodWithInfo_GetRequest,
        completion: @escaping (TaskResult<Rent_HouseServices_Periods_PeriodWithInfo_GetResponse, Error>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getPeriodWithWithInfo(
        _ request: Rent_HouseServices_Periods_PeriodWithInfo_GetRequest,
        completion: @escaping (TaskResult<Rent_HouseServices_Periods_PeriodWithInfo_GetResponse, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func uploadPhoto(jpegData: Data, url: URL) async -> TaskResult<Files_Uploader_Image, Swift.Error> {
        return .cancelled
    }

    func sendMeterReadings(
        flatID: String,
        periodID: String,
        meterReadingsID: String,
        request: Realty_Rent_Api_SendMeterReadingsRequest,
        completion: @escaping (TaskResult<Realty_Rent_Api_SendMeterReadingsResponsePayload, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendReceiptPhotos(
        flatID: String,
        periodID: String,
        request: Realty_Rent_Api_SendReceiptRequest,
        completion: @escaping (TaskResult<Realty_Rent_Api_SendReceiptResponsePayload, WebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func sendPaymentConfirmationPhotos(
        flatID: String,
        periodID: String,
        request: Realty_Rent_Api_SendPaymentConfirmationRequest,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_SendPaymentConfirmationResponsePayload, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func declineBillPayment(
        flatID: String,
        periodID: String,
        request: Realty_Rent_Api_DeclineBillRequest,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_DeclineBillResponsePayload, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func declineMeterReadings(
        flatID: String,
        periodID: String,
        meterReadingsID: String,
        request: Realty_Rent_Api_DeclineMeterReadingsRequest,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_DeclineMeterReadingsResponsePayload, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func createBillPayment(
        flatID: String,
        periodID: String,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_BillPaymentResponsePayload, Swift.Error>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func createBill(
        flatID: String,
        periodID: String,
        request: Realty_Rent_Api_BillRequest,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_CreateBillResponsePayload, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func declineReceipt(
        flatID: String,
        periodID: String,
        request: Realty_Rent_Api_DeclineReceiptRequest,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_DeclineReceiptResponsePayload, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        YREURLWebTask()
    }

    func declinePaymentConfirmation(
        flatID: String,
        periodID: String,
        request: Realty_Rent_Api_DeclinePaymentConfirmationRequest,
        completion: @escaping (
            TaskResult<Realty_Rent_Api_DeclinePaymentConfirmationResponsePayload, WebServiceError<YREFrontendError>>
        ) -> Void
    ) -> YREWebTaskProtocol {
        YREURLWebTask()
    }
}

final class FilesWebServiceMock: NSObject, FilesWebServiceProtocol {
    func getDownloadURL(
        request: Realty_Rent_Api_GetDownloadUrlRequest
    ) async -> TaskResult<Realty_Rent_Api_GetDownloadUrlResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func getUploadURL(
        request: Realty_Rent_Api_GetUploadUrlRequest,
        completion: @escaping (
            _ result: TaskResult<Realty_Rent_Api_GetUploadUrlResponse, Swift.Error>
        ) -> Void
    ) -> YREWebTaskProtocol {
        return YREURLWebTask()
    }

    func getUploadURL(
        request: Realty_Rent_Api_GetUploadUrlRequest
    ) async -> TaskResult<Realty_Rent_Api_GetUploadUrlResponse, Swift.Error> {
        return .cancelled
    }

    func uploadPhoto(jpegData: Data, url: URL) async -> TaskResult<Files_Uploader_Image, Swift.Error> {
        return .cancelled
    }
}

final class PriceStatisticsWebServiceMock: NSObject, PriceStatisticsWebServiceProtocol {
    func getPriceStatistics(
        siteID: String,
        rooms: [Realty_Site_Response_NewbuildingPriceStatisticsSeries.Rooms]
    ) async -> TaskResult<Realty_Site_Response_NewbuildingPriceStatisticsSeriesList, WebServiceError<YREFrontendError>> {
        return .cancelled
    }
}

final class InventoryWebServiceMock: NSObject, InventoryWebServiceProtocol {
    func getInventory(
        request: Rent_User_Inventory_NotConfirmed_GetRequest
    ) async -> TaskResult<Rent_User_Inventory_NotConfirmed_GetResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func updateInventory(
        request: Rent_User_Inventory_Edit_PostRequest
    ) async -> TaskResult<Realty_Rent_Api_InventoryResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func confirmInventory(
        request: Rent_User_Inventory_ConfirmationCode_Sumbit_PostRequest
    ) async -> TaskResult<Realty_Api_ApiUnit, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func getConfirmationCode(
        request: Rent_User_Inventory_ConfirmationCode_PostRequest
    ) async -> TaskResult<Realty_Rent_Api_SentSmsInfo, WebServiceError<YREFrontendError>> {
        return .cancelled
    }
}

final class RentContractWebServiceMock: NSObject, RentContractWebServiceProtocol {
    func getRentConract(
        with contractID: String
    ) async -> TaskResult<Rent_GetRentContractSummaryResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func askSigningSMSCode(
        for contractID: String
    ) async -> TaskResult<Realty_Rent_Api_AskRentContractSignCodeResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func signContract(
        contractID: String,
        request: Realty_Rent_Api_SignRentContractRequest
    ) async -> TaskResult<Realty_Rent_Api_SignRentContractSuccessResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }

    func requestChanges(
        contractID: String,
        request: Realty_Rent_Api_RentContractChangesRequest
    ) async -> TaskResult<Realty_Api_ApiUnit, WebServiceError<YREFrontendError>> {
        return .cancelled
    }
}

final class SiteReviewsWebServiceMock: SiteReviewsWebServiceProtocol {
    func reviews<Query>(
        _ request: Query,
        completion: @escaping (TaskResult<GetReviewsQuery.Data.OrgReview, GraphQLWebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol where Query: GetReviewsQuery {
        YREURLWebTask()
    }

    func addReview<Mutation>(
        _ mutation: Mutation,
        completion: @escaping (TaskResult<Mutation.Data.AddOrgReview, GraphQLWebServiceError<YREFrontendError>>) -> Void
    ) -> YREWebTaskProtocol where Mutation: AddReviewMutation {
        YREURLWebTask()
    }
}

final class RentDocumentsWebServiceMock: NSObject, RentDocumentsWebServiceProtocol {
    func getDocuments(
        for flatID: String
    ) async -> TaskResult<Realty_Rent_Api_GetAllFlatDocumentsResponse, WebServiceError<YREFrontendError>> {
        return .cancelled
    }
}
