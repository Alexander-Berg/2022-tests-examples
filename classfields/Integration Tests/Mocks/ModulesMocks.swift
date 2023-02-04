//
//  ModulesMocks.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 25.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import UIKit
import YREModuleFactory
import YREServiceLayer
import YREServiceInterfaces
import YREServiceFactory
import YREModel
import YREModelObjc
import YREFiltersModel
import YREAnalytics
import YREDesignKit
import YRESearchComponents
@testable import YREMapScreenModule
import YREMapComponents
import YRECoreUtils
import YRESettings
import YREModelHelpers
import YRESnippets
import YRECardComponents
import YRELegacyFiltersCore
import YREAppState
import YREAnalyticsGateway
import YREAnalyticComponents
import YREFeatures
import YREAdvertisementsGateway
import YRESharedStorage
import YREOfferCardModule
import YREStaticMapModule
import YREStoriesPromoModule
import YREYaRentOwnerLandingModule

private let mapRelatedOffersListDataSourceFactoryMock = MapRelatedOffersListDataSourceFactory( // swiftlint:disable:this identifier_name
    searchService: searchService,
    personalizationService: personalizationServiceMock,
    adsService: AdsServiceMock(),
    authStateObservable: AuthStateObservableMock(),
    analyticsReporter: AnalyticsReporterMock(),
    settings: YREBusinessSettings(),
    filterRootFactory: FilterRootFactory()
)

private let personalizationServiceMock = PersonalizationService(webServices: webServicesFactory)

private var realmDBFilePath: String {
    let path = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
    let businessSettings = YRESettings.shared.business
    let result = (path as NSString).appendingPathComponent(businessSettings.realmDBFileName)
    return result
}

private let realmController: YRERealmController = {
    $0.configure(withDatabaseFilePath: realmDBFilePath, completionBlock: nil)
    return $0
}(YRERealmController(filterRootFactory: FilterRootFactory()))

private let appState = YREAppState()
private let keyStorage = ExternalServicesKeyStorage()
private let authManager: YREAuthManager = {
    let accountManagerSettings: ExternalServicesKeyStorage.AccountManagerSettings
    switch appState.backendEndpointType {
        case .prod:
            accountManagerSettings = keyStorage.productionAccountManagerSettings
        case .dev, .local:
            accountManagerSettings = keyStorage.testingAccountManagerSettings
    }

    return YREAuthManager(
        appState: appState,
        settings: accountManagerSettings
    )
}()

private let dataLayer = YREDataLayer(
    dependencies: .init(
        appState: appState,
        authManager: authManager,
        pushNotificationsGateway: PushNotificationGatewayMock(),
        firebaseGateway: FirebaseGatewayMock(),
        adjustInfoProvider: AdjustInfoProviderMock(),
        yandexAdsGateway: YandexAdsGateway(blockID: ""),
        filterRootFactory: FilterRootFactory(),
        analyticsReporter: AnalyticsReporterMock(),
        featureTogglesStorage: FeatureTogglesStorage(provider: RealmProvider()),
        expboxesWriter: ExpboxesWriterMock()
    )
)

private let dataLayerMock: YREDataLayer = {
    $0.configure(
        with: appState,
        filterRootFactory: FilterRootFactory(),
        resetPersistentStorage: false,
        completionBlock: { _ in }
    )

    RealtyEventLogTracker.shared.objc_configure(
        dataLayer: $0,
        eventLogService: $0.eventLogService,
        deliveryEventQueueDBFileName: "test.db",
        shouldUseBatchSend: false
    )
    RealtyEventLogTracker.shared.flushStoredEvents()
    return $0
}(dataLayer)

private let searchService = SearchService(webServices: webServicesFactory, serviceFactory: ServiceFactoryMock())
private let recentSearchesService = RecentSearchesService(
    searchStateReader: appState,
    recentSearchesStateWriter: appState,
    maxSearchesCount: 0
)

let filterInteractor = YREFilterInteractor(
    searchStateWriter: SearchStateWriterMock(),
    searchService: searchService,
    recentSearchesService: recentSearchesService,
    fallbackRegionProvider: FallbackRegionProviderMock(),
    fallbackRegionConfigurationProvider: FallbackRegionConfigurationProviderMock(),
    businessSettings: YREBusinessSettings()
)

// MARK: - UserNoteModule
final class UserNoteModuleMock: UserNoteModule {
    func present(at viewController: UIViewController) {}
}

// MARK: - VillageCardModule
final class VillageCardModuleMock: VillageCardModule {
    weak var delegate: VillageCardModuleDelegate?

    func present() {}
    func dismissModalScreens(completion: (() -> Void)?) {}
}

// MARK: - ApplicationAppStoreRouterProtocol
struct ApplicationAppStoreRouterMock: ApplicationAppStoreRouterProtocol {
    func openApplicationAppStorePage() {}
}

// MARK: - SiteCardModule
final class SiteCardModuleMock: SiteCardModule {
    weak var delegate: SiteCardModuleDelegate?

    func present(completion: ((Bool) -> Void)?) {}
    func dismissModalScreens(completion: (() -> Void)?) {}
    func callToSiteIfPossible(referrer: ScreenReferrer) {}
}

private let userService = UserService(
    webServices: webServicesFactory,
    userDataWriter: UserDataWriterMock(),
    authStateObservable: AuthStateObservableAgent(authStateReader: AuthStateWriterMock())
)

// MARK: - SnippetAdditionalActionsModule
final class SnippetAdditionalActionsModuleMock: SnippetAdditionalActionsModule {
    func present(at viewController: UIViewController, actionHandler: @escaping SnippetAdditionalActionsHandler) {}
}

// MARK: - SavedSearchParamsModule
final class SavedSearchParamsModuleMock: SavedSearchParamsModule {    
    weak var delegate: SavedSearchParamsModuleDelegate?

    func present(in container: UIViewController) {}
    func dismiss(completion: (() -> Void)?) {}
}

// MARK: - ConciergeModule
final class ConciergeModuleMock: ConciergeModule {
    var viewController: UIViewController { .init() }
    weak var delegate: ConciergeModuleDelegate?
}

// MARK: - FavoritesModule
final class FavoritesModuleMock: FavoritesModule {
    func canAddToFavorites() -> Bool {
        true
    }

    func isInFavorites(_ anyOffer: YREAbstractOffer) -> Bool {
        false
    }

    func addToFavorites(
        _ anyOffer: YREAbstractOffer,
        underlyingViewController: UIViewController,
        source: AnyOfferEventSource,
        referrer: ScreenReferrer
    ) {}

    func addToFavoritesFromOfferCardPriceTrend(
        _ anyOffer: YREAbstractOffer,
        underlyingViewController: UIViewController,
        referrer: ScreenReferrer
    ) {}

    func addToFavorites(_ offer: VillageOffer, village: Village, underlyingViewController: UIViewController, referrer: ScreenReferrer) {}

    func addSharedItemsToFavorites(
        _ items: [String],
        underlyingViewController: UIViewController,
        itemType: FavoritesItemType,
        completion: (TaskResult<Void, Error>) -> Void
    ) {}

    func removeFromFavorites(_ anyOffer: YREAbstractOffer, source: AnyOfferEventSource, referrer: ScreenReferrer) {}

    func removeFromFavorites(_ offer: VillageOffer, village: Village, referrer: ScreenReferrer) {}
}

// MARK: - GeoIntentSearchModule
final class GeoIntentSearchModuleMock: NSObject, GeoIntentSearchModule {
    weak var delegate: GeoIntentSearchModuleDelegate?

    func present() {}
}

// MARK: - LegacyFiltersModule
final class LegacyFiltersModuleMock: LegacyFiltersModule {
    weak var delegate: LegacyFiltersModuleDelegate?

    func dismissModalScreens(completion: (() -> Void)?) {}
    func present(animated: Bool) {}
    func promoteNotGrannysRenovation() {}
    func promoteYandexRent() {}
}

// MARK: - AbuseModule
final class AbuseModuleMock: AbuseModule {
    func showAbuse() {}
}

// MARK: - RateUsModule
final class RateUsModuleMock: RateUsModule {
    func presentIfNeeded() {}
}

// MARK: - PriceHistoryModule
final class PriceHistoryModuleMock: PriceHistoryModule {
    var referrer: ScreenReferrer { nil }
    weak var delegate: PriceHistoryModuleDelegate?
    var viewController: DrawerController { DrawerController() }
}

// MARK: - ExcerptPaymentModule
final class ExcerptPaymentModuleMock: ExcerptPaymentModule {
    weak var delegate: ExcerptPaymentModuleDelegate?
    var viewController: UIViewController { .init() }
}

// MARK: - PaidExcerptsListModule
final class PaidExcerptsListModuleMock: PaidExcerptsListModule {
    weak var delegate: PaidExcerptsListModuleDelegate?

    var viewController: UIViewController { .init() }

    func present() {}
}

// MARK: - PaidExcerptsModule
final class PaidExcerptsModuleMock: PaidExcerptsModule {
    weak var delegate: PaidExcerptsModuleDelegate?

    var viewController: UIViewController { .init() }
}

// MARK: - WrappedBrowserModule
final class WrappedBrowserModuleMock: WrappedBrowserModule {
    weak var delegate: WrappedBrowserModuleDelegate?

    func present(context: WrappedBrowserNavigationContext, animated: Bool, completion: (() -> Void)?) {}
}

// MARK: - WebBasedUserStoryModule
final class WebBasedUserStoryModuleMock: WebBasedUserStoryModule {
    var viewController: UIViewController {
        return UIViewController()
    }

    var onCloseAction: (() -> Void)?
    var onOpenURLAction: ((URL) -> Void)?
    var onReceiveMessageAction: ((String, Any) -> Void)?
}

// MARK: - OfferListModule
final class OfferListModuleMock: OfferListModule {
    var delegate: OfferListModuleDelegate?

    func present() { }
}

// MARK: - OfferCardModuleDeps
final class OfferCardModuleDepsMock: NSObject, OfferCardModuleDeps {
    init(anyOfferSearchService: AnyOfferSearchService) {
        self.anyOfferSearchService = anyOfferSearchService
    }

    func makeAbuseModule(
        offerDescription: YREOfferDescription,
        mode: AbuseMode,
        source: AbuseSource
    ) -> AbuseModule {
        AbuseModuleMock()
    }

    func makeAnyOfferGalleryModule(
        infoProvider: OfferInfoProviderProtocol,
        initialImageIndex: UInt,
        isUserOffer: Bool,
        delegate: OfferGalleryModuleDelegate?,
        referrer: ScreenReferrer
    ) -> AnyOfferGalleryModule? {
        nil
    }

    func makeAnyOfferGalleryModule(
        infoProvider: OfferSnippetInfoProviderProtocol,
        initialImageIndex: UInt,
        isUserOffer: Bool,
        delegate: OfferGalleryModuleDelegate?,
        referrer: ScreenReferrer
    ) -> AnyOfferGalleryModule? {
        nil
    }

    func makeAnyOfferGalleryModule(
        infoProvider: SiteInfoProviderProtocol,
        initialImageIndex: UInt,
        delegate: SiteGalleryModuleDelegate?,
        referrer: ScreenReferrer
    ) -> AnyOfferGalleryModule? {
        nil
    }

    func makeAnyOfferGalleryModule(
        infoProvider: VillageInfoProviderProtocol,
        initialImageIndex: UInt,
        delegate: VillageGalleryModuleDelegate?,
        referrer: ScreenReferrer
    ) -> AnyOfferGalleryModule? {
        nil
    }

    func makeOfferArchiveModule(
        offer: YREOffer,
        parallelNavigationViewController: YREParallelNavigationViewController,
        authorizationParentViewController: UIViewController
    ) -> ArchivalOffersModule? {
        nil
    }

    func makeChatModule(
        moduleData: ChatModuleData,
        navigationContext: ChatNavigationContext
    ) -> ChatModule {
        ChatModuleMock()
    }

    func makeExcerptPaymentModule(
        moduleData: ExcerptPaymentModuleData
    ) -> ExcerptPaymentModule {
        ExcerptPaymentModuleMock()
    }

    func makeFavoritesModule() -> FavoritesModule {
        FavoritesModuleMock()
    }

    func makeOfferCardModule(
        arguments: OfferCardArguments,
        navigationContext: OfferCardNavigationContext,
        referrer: ScreenReferrer?
    ) -> OfferCardModule {
        MapScreenModuleDepsMock(anyOfferSearchService: anyOfferSearchService).makeOfferCardModule(
            arguments: arguments,
            navigationContext: navigationContext,
            referrer: referrer
        )
    }

    func makePaidExcerptsListModule(
        context: PaidExcerptsListNavigationContext,
        openOffersWithExcerpts: @escaping () -> Void
    ) -> PaidExcerptsListModule {
        PaidExcerptsListModuleMock()
    }

    func makePaidExcerptsModule(moduleData: PaidExcerptsModuleData) -> PaidExcerptsModule {
        PaidExcerptsModuleMock()
    }

    func makePriceHistoryModule(_ data: PriceHistoryModuleData) -> PriceHistoryModule? {
        PriceHistoryModuleMock()
    }

    func makeSiteCardModule(arguments: SiteCardModuleArguments, navigationContext: SiteCardNavigationContext) -> SiteCardModule {
        SiteCardModuleMock()
    }

    func makeSnippetAdditionalActionsModule(
        offer: YREOffer,
        actions: [OfferSnippetAdditionalAction],
        source: AdditionalActionsSource,
        referrer: ScreenReferrer
    ) -> SnippetAdditionalActionsModule {
        SnippetAdditionalActionsModuleMock()
    }

    func makeSnippetAdditionalActionsModule(
        offerSnippet: YREOfferSnippet,
        actions: [OfferSnippetAdditionalAction],
        source: AdditionalActionsSource,
        referrer: ScreenReferrer
    ) -> SnippetAdditionalActionsModule {
        SnippetAdditionalActionsModuleMock()
    }

    func makeUserNoteModule(offerSnippet: YREOfferSnippet, source: UserNoteSource, referrer: ScreenReferrer) -> UserNoteModule {
        UserNoteModuleMock()
    }

    func makeUserNoteModule(offer: YREOffer, source: UserNoteSource, referrer: ScreenReferrer) -> UserNoteModule {
        UserNoteModuleMock()
    }

    func makeVillageCardModule(
        arguments: VillageCardModuleArguments,
        navigationContext: VillageCardNavigationContext
    ) -> VillageCardModule {
        VillageCardModuleMock()
    }

    func makeManualWebModule(postURL: URL) -> ManualModule? {
        nil
    }

    func makeDocumentsWebModule(url: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeDeeplinkWebModule(title: String, url: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeFeedbackWebModule() -> WebBasedUserStoryModule? {
        nil
    }

    func makeAdPageWebModule(title: String, url: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeVirtualTourWebModule(url: URL, title: String? = nil) -> WebBasedUserStoryModule? {
        nil
    }

    func makeReviewsWebModule(siteID: String, siteName: String?) -> WebBasedUserStoryModule? {
        nil
    }

    func makePaymentURLConfirmationWebModule(_ url: URL, finalConfirmationURL: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeMortgageCalculatorWebModule(parameters: FrontendURLProvider.MortgageCalculatorFormParameters) -> WebBasedUserStoryModule? {
        nil
    }

    func makeApplicationFormWebModule(
        siteID: String,
        siteName: String?,
        subjectFederationID: String,
        subjectFederationName: String?,
        developerIDs: [String]
    ) -> WebBasedUserStoryModule? {
        nil
    }

    func makeHouseUtilitiesSettingsAcceptanceWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeWorkspaceURLRouter() -> WorkspaceURLRouterProtocol {
        WorkspaceURLRouterMock()
    }

    func makeApplicationSettingsRouter() -> ApplicationSettingsRouterProtocol {
        ApplicationSettingsRouterMock()
    }

    func makeApplicationAppStoreRouter() -> ApplicationAppStoreRouterProtocol {
        ApplicationAppStoreRouterMock()
    }

    func makeWrappedBrowserModule(moduleData: WrappedBrowserModuleData) -> WrappedBrowserModule {
        WrappedBrowserModuleMock()
    }

    func makeTinkoffAddCardWebModule(
        url: URL,
        onBinding: @escaping (Bool) -> Void
    ) -> WebBasedUserStoryModule? {
        nil
    }

    func makeYaRentOwnerLandingModule(
        input: YaRentOwnerLandingModuleInput
    ) -> YaRentOwnerLandingModule? {
        nil
    }

    func makeOwnerHouseUtilitiesEditSettingsWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerRentCandidatesWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerFlatInfoWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerPersonalInformationWebModule() -> WebBasedUserStoryModule? {
        nil
    }

    func makeRoommatesListWebModule(url: URL) -> WebBasedUserStoryModule {
        return WebBasedUserStoryModuleMock()
    }

    func makeShareWithRoommatesFormWebModule(url: URL) -> WebBasedUserStoryModule {
        return WebBasedUserStoryModuleMock()
    }

    func makePhoneCallModule(moduleData: PhoneCallModuleData) -> PhoneCallModule {
        PhoneCallModuleMock()
    }

    func makeAuthModule() -> AuthModule {
        return authManager
    }

    func makeYaRentOfferListModule(
        title: String,
        rgid: Int?,
        referrer: ScreenReferrer,
        navigationContext: OfferListNavigationContext
    ) -> OfferListModule {
        OfferListModuleMock()
    }

    private let anyOfferSearchService: AnyOfferSearchService
}

final class PhoneCallModuleMock: PhoneCallModule {
    var onStateChanged: (((PhoneCallingState)) -> Void)?

    func present(in viewController: UIViewController) {}
}

// MARK: - ChatModule
final class ChatModuleMock: ChatModule {
    weak var delegate: ChatModuleDelegate?

    func present(animated: Bool) {}
    func dismissModalScreens(completion: (() -> Void)?) {}
    func showCurrentChatRecentMessagesIfPossible() {}
}

struct NullDeps: NullModuleDeps {}

let staticMapModuleFactory = StaticMapModuleFactory(
    serviceDeps: .init(httpHeaders: httpHeaders, connectionSettingsReader: ConnectionSettingsMock()),
    moduleDeps: NullDeps()
)

// MARK: - MapScreenModuleDeps

final class MapScreenMapPresenterMock: NSObject, MapScreenMapPresenterProtocol {
    init(filterInteractor: YREFilterInteractor) {
        self.filterInteractor = filterInteractor

        self.pointsLoadingRoutineEnabled = false
        self.canSendSearchShowAnalytics = false
    }

    weak var screenDelegate: MapScreenMapPresenterDelegate?

    private var _screenViewController: YREMapScreenMapViewController?
    var screenViewController: YREMapScreenMapViewController {
        if let value = self._screenViewController {
            return value
        }

        let controlsSheetBuilder = self.makeMapControlsOverlayViewBuilder()

        let mapViewController = YREMapScreenMapViewController(
            interactionMapBuilder: YREInteractionMapBuilder(),
            mapControlsOverlayViewBuilder: controlsSheetBuilder
        )

        self._screenViewController = mapViewController

        return mapViewController
    }

    var pointsLoadingRoutineEnabled: Bool
    var displayingOptions: SearchResultsMapDisplayingOptionsProtocol?
    var canSendSearchShowAnalytics: Bool

    func showHeatmapsSwitcher() {}

    func rentPromoClosed() {}
    func rentPromoFinishedWithYaRentOwnerApplicationForm() {}
    func rentPromoFinishedWithYaRentChoosingAction() {}
    func rentPromoFinishedWithReferralLinkAction() {}
    
    func filtersModuleDidFinish() {}

    func selectAnnotation(for geoPoint: YREGeoPoint) {}
    func deselectAll() {}

    func hideOffer(_ offerID: String) {}

    func correctedViewPort(_ viewPort: YRECoordinateRegion, shouldApplyInsets: Bool) -> YRECoordinateRegion {
        return viewPort
    }

    private let filterInteractor: YREFilterInteractor

    private func makeMapControlsOverlayViewBuilder() -> YREMapControlsOverlayViewBuilder {
        let regionConfiguration = self.filterInteractor.regionInfo.configuration
        let heatmapsInfo = regionConfiguration.heatmapsInfo
        let schoolInfo = regionConfiguration.schoolInfo
        let availableMapControls: kYREMapControlsOptions = []

        let controlsSheetBuilder = YREMapControlsOverlayViewBuilder(
            availableMapControls: availableMapControls,
            heatmapsInfo: heatmapsInfo,
            schoolInfo: schoolInfo
        )

        return controlsSheetBuilder
    }
}

final class MapScreenModuleDepsMock: MapScreenModuleDeps {
    var offerCardViewController: UIViewController?

    init(anyOfferSearchService: AnyOfferSearchService) {
        self.anyOfferSearchService = anyOfferSearchService
    }

    func makeAbuseModule(
        offerDescription: YREOfferDescription,
        mode: AbuseMode,
        source: AbuseSource
    ) -> AbuseModule {
        AbuseModuleMock()
    }

    func makeChatModule(moduleData: ChatModuleData, navigationContext: ChatNavigationContext) -> ChatModule {
        ChatModuleMock()
    }

    func makeConciergeModule(source: ConciergeSource) -> ConciergeModule {
        ConciergeModuleMock()
    }

    func makeFavoritesModule() -> FavoritesModule {
        FavoritesModuleMock()
    }

    func makeGeoIntentSearchModule(
        arguments: GeoIntentSearchModuleArguments,
        navigationContext: GeoIntentSearchNavigationContext
    ) -> GeoIntentSearchModule {
        GeoIntentSearchModuleMock()
    }

    func makeGeoIntentSearchModule(
        initialValue: YREGeoParameters,
        navigationContext: GeoIntentSearchNavigationContext
    ) -> GeoIntentSearchModule {
        GeoIntentSearchModuleMock()
    }

    func makeLegacyFiltersModule(navigationContext: LegacyFiltersNavigationContext) -> LegacyFiltersModule {
        LegacyFiltersModuleMock()
    }

    func makeOfferCardModule(
        arguments: OfferCardArguments,
        navigationContext: OfferCardNavigationContext,
        referrer: ScreenReferrer?
    ) -> OfferCardModule {
        let appState = YREAppState()
        let factory = OfferCardModuleFactory(
            moduleDeps: OfferCardModuleDepsMock(anyOfferSearchService: anyOfferSearchService),
            routerDeps: .init(
                cardDataDeps: .init(
                    services: .init(
                        searchService: anyOfferSearchService,
                        favoriteOffersService: FavoriteOffersServiceMock(),
                        personalizationService: personalizationServiceMock,
                        callHistoryService: CallHistoryServiceMock(),
                        viewedOfferService: ViewedOfferServiceMock(),
                        abuseService: AbuseService(webServices: webServicesFactory),
                        userOfferService: YREUserOfferService(
                            authStateReader: AuthStateReaderMock(),
                            userDataWriter: UserDataWriterMock(),
                            userOfferSettingsWritter: UserOfferSettingsWriterMock(),
                            userService: userService,
                            webServices: webServicesFactory
                        ),
                        geoRoutingService: YREGeoRoutingService(routerSettingsWriter: GeoRouterSettingsWriterMock()),
                        archivalOffersService: ArchivalOffersService(webServices: webServicesFactory),
                        excerptsService: ExcerptsService(webServices: webServicesFactory),
                        mortgageService: MortgageService(
                            webServices: webServicesFactory,
                            geoObjectsService: GeoObjectsService(webServices: webServicesFactory),
                            mortgageRequestFilterWriter: nil
                        ),
                        blogPostsService: BlogPostsService(webServices: webServicesFactory),
                        userService: userService,
                        siteCallbackService: SiteCallbackServiceMock(),
                        rentService: RentService(
                            webServices: webServicesFactory,
                            rentUserDataWriter: appState,
                            authStateObservable: AuthStateObservableAgent(authStateReader: AuthStateWriterMock())
                        ),
                        filterInteractor: filterInteractor
                    ),
                    appState: appState,
                    authStateObservable: AuthStateObservableMock(),
                    businessSettings: YREBusinessSettings(),
                    callActionsCache: .sharedCache,
                    frontedURLProvider: FrontendURLProvider(connectionSettings: ConnectionSettingsMock())
                ),
                cardViewDeps: .init(
                    gallerySlideIndicesCache: .sharedCache,
                    analyticsReporter: AnalyticsReporterMock(),
                    staticMapViewProvider: staticMapModuleFactory.makeStaticMapModule().view
                ),
                authService: AuthManagerService(authManager: authManager)
            ),
            cardCallbackDisclaimerFactory: CardCallbackDisclaimerFactory()
        )
        let module = factory.makeOfferCardModule(
            arguments: arguments,
            navigationContext: navigationContext,
            referrer: referrer
        )
        self.offerCardViewController = module.viewController
        return module
    }

    func makeRateUsModule() -> RateUsModule {
        RateUsModuleMock()
    }

    func makeSavedSearchParamsModule(
        moduleData: SavedSearchParamsModuleData
    ) -> SavedSearchParamsModule {
        SavedSearchParamsModuleMock()
    }

    func makeSiteCardModule(
        arguments: SiteCardModuleArguments,
        navigationContext: SiteCardNavigationContext
    ) -> SiteCardModule {
        SiteCardModuleMock()
    }

    func makeSnippetAdditionalActionsModule(
        offer: YREOffer,
        actions: [OfferSnippetAdditionalAction],
        source: AdditionalActionsSource,
        referrer: ScreenReferrer
    ) -> SnippetAdditionalActionsModule {
        SnippetAdditionalActionsModuleMock()
    }

    func makeSnippetAdditionalActionsModule(
        offerSnippet: YREOfferSnippet,
        actions: [OfferSnippetAdditionalAction],
        source: AdditionalActionsSource,
        referrer: ScreenReferrer
    ) -> SnippetAdditionalActionsModule {
        SnippetAdditionalActionsModuleMock()
    }

    func makeUserNoteModule(
        offerSnippet: YREOfferSnippet,
        source: UserNoteSource,
        referrer: ScreenReferrer
    ) -> UserNoteModule {
        UserNoteModuleMock()
    }

    func makeUserNoteModule(
        offer: YREOffer,
        source: UserNoteSource,
        referrer: ScreenReferrer
    ) -> UserNoteModule {
        UserNoteModuleMock()
    }

    func makeVillageCardModule(
        arguments: VillageCardModuleArguments,
        navigationContext: VillageCardNavigationContext
    ) -> VillageCardModule {
        VillageCardModuleMock()
    }

    func makeManualWebModule(postURL: URL) -> ManualModule? {
        nil
    }

    func makeDocumentsWebModule(url: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeDeeplinkWebModule(title: String, url: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeFeedbackWebModule() -> WebBasedUserStoryModule? {
        nil
    }

    func makeAdPageWebModule(title: String, url: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeVirtualTourWebModule(url: URL, title: String? = nil) -> WebBasedUserStoryModule? {
        nil
    }

    func makeReviewsWebModule(siteID: String, siteName: String?) -> WebBasedUserStoryModule? {
        nil
    }

    func makePaymentURLConfirmationWebModule(_ url: URL, finalConfirmationURL: URL) -> WebBasedUserStoryModule? {
        nil
    }

    func makeMortgageCalculatorWebModule(
        parameters: FrontendURLProvider.MortgageCalculatorFormParameters
    ) -> WebBasedUserStoryModule? {
        nil
    }

    func makeApplicationFormWebModule(
        siteID: String,
        siteName: String?,
        subjectFederationID: String,
        subjectFederationName: String?,
        developerIDs: [String]
    ) -> WebBasedUserStoryModule? {
        nil
    }

    func makeHouseUtilitiesSettingsAcceptanceWebModule(
        flatID: String
    ) -> WebBasedUserStoryModule? {
        nil
    }

    func makeWorkspaceURLRouter() -> WorkspaceURLRouterProtocol {
        WorkspaceURLRouterMock()
    }

    func makeApplicationSettingsRouter() -> ApplicationSettingsRouterProtocol {
        ApplicationSettingsRouterMock()
    }

    func makeApplicationAppStoreRouter() -> ApplicationAppStoreRouterProtocol {
        ApplicationAppStoreRouterMock()
    }

    func makeTinkoffAddCardWebModule(
        url: URL,
        onBinding: @escaping (Bool) -> Void
    ) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerHouseUtilitiesEditSettingsWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerRentCandidatesWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerFlatInfoWebModule(flatID: String) -> WebBasedUserStoryModule? {
        nil
    }

    func makeOwnerPersonalInformationWebModule() -> WebBasedUserStoryModule? {
        nil
    }

    func makeRoommatesListWebModule(url: URL) -> WebBasedUserStoryModule {
        return WebBasedUserStoryModuleMock()
    }

    func makeShareWithRoommatesFormWebModule(url: URL) -> WebBasedUserStoryModule {
        return WebBasedUserStoryModuleMock()
    }

    func makePhoneCallModule(moduleData: PhoneCallModuleData) -> PhoneCallModule {
        PhoneCallModuleMock()
    }

    func makeRecentSearchParametersModule(
        search: YRESearchProtocol,
        delegate: RecentSearchParametersViewControllerDelegate
    ) -> UIViewController {
        UIViewController()
    }
    
    func makeHouseUtilitiesStoriesPromoModule(
        userRole: HouseUtilitiesUserRole,
        onClose: @escaping () -> Void
    ) -> StoriesPromoModule {
        StoriesPromoModuleMock()
    }
    
    func makeInventoryStoriesPromoModule(
        userRole: YaRentInventoryUserRole,
        onClose: @escaping () -> Void,
        fillOwnerInventory: @escaping () -> Void,
        confirmTenantInventory: @escaping () -> Void
    ) -> StoriesPromoModule {
        StoriesPromoModuleMock()
    }
    
    func makeYaRentOnMapStoriesPromoModule(
        onClose: @escaping () -> Void,
        openOwnerLanding: @escaping () -> Void,
        openRentSearch: @escaping () -> Void,
        openReferralPage: @escaping () -> Void
    ) -> StoriesPromoModule {
        StoriesPromoModuleMock()
    }
    
    func makeYaRentOwnerLandingModule(
        input: YaRentOwnerLandingModuleInput
    ) -> YaRentOwnerLandingModule? {
        nil
    }

    private let anyOfferSearchService: AnyOfferSearchService
}

// MARK: - MapScreenRouterPartsFactoryProtocol
struct RouterPartsFactoryMock: MapScreenRouterPartsFactoryProtocol {
    func makeMapScreenNavigationPresenter() -> YREMapScreenNavigationPresenter {
        .init(navigationStateReader: NavigationStateWriterMock(), filterInteractor: filterInteractor)
    }

    func makeOffersInteractor() -> YREOffersInteractor {
        .init(
            filterInteractor: filterInteractor,
            offerListStateWriter: OfferListStateWriterMock(),
            togglesReader: FeatureTogglesReaderMock(),
            searchService: searchService,
            favoriteOffersService: FavoriteOffersServiceMock(),
            personalizationService: personalizationServiceMock,
            listDataSourceFactory: mapRelatedOffersListDataSourceFactoryMock
        )
    }

    func makeFilterPromoAnalyticsReporter() -> FilterPromoAnalyticsReporter {
        .init(analyticsReporter: AnalyticsReporterMock())
    }

    func makeMainListPresenter(with offersInteractor: YREOffersInteractor) -> MainOfferListPresenter {
        .init(
            offersInteractor: self.makeOffersInteractor(),
            filterInteractor: filterInteractor,
            callHistoryService: CallHistoryServiceMock(),
            viewedOfferService: ViewedOfferServiceMock(),
            navigationStateWriter: NavigationStateWriterMock()
        )
    }

    func makeMapScreenMapPresenter() -> MapScreenMapPresenterProtocol {
        // @coreshock: Leave logic as-it-was.
        MapScreenMapPresenterMock(
            filterInteractor: filterInteractor
        )
    }

    func makeSchoolOnMapSnippetPresenter(for school: YRESchoolOnMap) -> SchoolOnMapSnippetPresenter {
        .init(school: school)
    }

    func makeHeatmapDescriptionOnMapSnippetPresenter(for heatmapType: HeatmapType) -> HeatmapDescriptionOnMapSnippetPresenter {
        .init(heatmapType: heatmapType)
    }

    func makeMultihouseOfferListPresenter(
        geoPoint: YREMultihouseGeoPoint,
        offersInteractor: YREOffersInteractor,
        mapScreenAnalyticsReporter: MapScreenAnalyticsReporter
    ) -> YREMultihouseOfferListPresenter? {
        nil
    }

    func makeOfferListSortOptionsPresenter(sortOptions: [NSNumber], selectedSortType: SortType) -> OfferListSortOptionsPresenter {
        .init(sortTypeOptions: [], selectedSortType: .byAreaAsc)
    }

    func makeOfferSnippetInteractor(offerData: OfferSnippetInteractorArguments.OfferData) -> OfferSnippetInteractor {
        .init(
            arguments: .init(offerData: offerData),
            dataLayerDeps: .init(
                searchService: searchService,
                personalizationService: personalizationServiceMock,
                callHistoryService: CallHistoryServiceMock(),
                searchStateReader: SearchStateWriterMock(),
                viewedOfferService: ViewedOfferServiceMock(),
                togglesReader: FeatureTogglesReaderMock()
            )
        )
    }

    func makeOfferSnippetPresenter(
        interactor: OfferSnippetInteractor,
        favoritesModule: FavoritesModule
    ) -> OfferSnippetPresenter {
        .init(
            interactor: interactor,
            viewController: YREOfferSnippetViewController(),
            deps: .init(
                callActionsCache: .sharedCache,
                favoritesModule: FavoritesModuleMock(),
                callHistoryService: CallHistoryServiceMock(),
                analyticsReporter: AnalyticsReporterMock()
            )
        )
    }

    func makeSiteSnippetInteractor(siteData: SiteSnippetInteractorArguments.SiteData) -> SiteSnippetInteractor {
        .init(
            arguments: .init(siteData: siteData),
            dataLayerDeps: .init(
                searchService: searchService,
                callHistoryService: CallHistoryServiceMock(),
                viewedOfferService: ViewedOfferServiceMock(),
                togglesReader: FeatureTogglesReaderMock()
            )
        )
    }

    func makeSiteSnippetPresenter(interactor: SiteSnippetInteractor, favoritesModule: FavoritesModule) -> SiteSnippetPresenter {
        .init(
            interactor: interactor,
            viewController: YRESiteSnippetViewController(),
            deps: .init(
                callActionsCache: .sharedCache,
                favoritesModule: FavoritesModuleMock(),
                callHistoryService: CallHistoryServiceMock(),
                analyticsReporter: AnalyticsReporterMock()
            )
        )
    }

    func makeVillageSnippetInteractor(villageData: VillageSnippetInteractorArguments.VillageData) -> VillageSnippetInteractor {
        .init(
            arguments: .init(villageData: villageData),
            dataLayerDeps: .init(
                searchService: searchService,
                callHistoryService: CallHistoryServiceMock(),
                viewedOfferService: ViewedOfferServiceMock(),
                togglesReader: FeatureTogglesReaderMock()
            )
        )
    }

    func makeVillageSnippetPresenter(
        interactor: VillageSnippetInteractor,
        favoritesModule: FavoritesModule
    ) -> VillageSnippetPresenter {
        .init(
            interactor: interactor,
            viewController: YREVillageSnippetViewController(),
            deps: .init(
                callActionsCache: .sharedCache,
                favoritesModule: FavoritesModuleMock(),
                callHistoryService: CallHistoryServiceMock()
            )
        )
    }
}

final class MapScreenInteractorMock: MapScreenInteractorProtocol {
    func shouldPresentPaidSites() -> Bool {
        return false
    }

    func markPaidSitesShown() {}

    func makeSiteFilterTransformer() -> FilterTransformerProtocol {
        let filterRoot = FilterRootFactory().makeFilterRoot(with: .buy, category: .apartment)! // swiftlint:disable:this force_unwrapping
        let transformer = FilterTransformer(transformBlock: { _ in
            FilterTransformResult.withoutGeoIntent(filterRoot: filterRoot, shouldResetGeoIntent: false)
        })
        return transformer
    }

    func makeRecentSearchParametersModule(
        search: YRESearchProtocol,
        delegate: RecentSearchParametersViewControllerDelegate
    ) -> UIViewController {
        UIViewController()
    }
}

// MARK: - WorkspaceURLRouterProtocol
struct WorkspaceURLRouterMock: WorkspaceURLRouterProtocol {
    func canOpenURL(_ url: URL) -> Bool {
        false
    }

    func open(url: URL, completion: ((Bool) -> Void)?) {}
}

// MARK: - ApplicationSettingsRouterProtocol
struct ApplicationSettingsRouterMock: ApplicationSettingsRouterProtocol {
    func openApplicationSettings() {}
}

// MARK: - StoriesPromoModule
final class StoriesPromoModuleMock: StoriesPromoModule {
    var viewController: UIViewController {
        UIViewController()
    }
}
