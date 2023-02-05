import BeruCore
import BeruServices
import Foundation
import MarketProtocols
import XCTest
@testable import Beru

final class DependencyProvider {

    var internalApiClient: YMTAPIClient {
        let apiClient = YMTAPIClient(baseURL: YMTAPIClientMobileContentBaseURL(YMTAPIVersion.YMTAPIVersion1))
        YMTAPIClient.setSharedInstance(apiClient)
        apiClient.uuid = "11111111111111111"
        return apiClient
    }

    var apiClient: APIClient {
        APIClient(apiClient: internalApiClient)
    }

    var marketUIDService: MarketUIDService {
        MarketUIDServiceImpl(apiClient: apiClient)
    }

    var modelService: ModelService {
        ModelServiceImpl(apiClient: apiClient)
    }

    var offersService: ProductOffersService {
        ProductOffersServiceImpl(apiClient: apiClient)
    }

    var marketUIDHelper: Beru.MarketUIDHelper {
        MarketUIDHelper(marketUIDSerive: marketUIDService, userDefaults: .standard)
    }

    var yandexUIDHelper: YMTYandexUIDHelper {
        YMTYandexUIDHelper(profileService: profileService)
    }

    var settings: YMTSettings {
        let settings = YMTSettings.sharedInstance()
        settings.selectedRegion = YMTRegion(id: 213, name: "Москва", type: .city)
        return settings
    }

    var flagStorage: FlagStorage {
        FlagStorage.shared
    }

    var paymentService: PaymentService {
        PaymentServiceImpl(apiClient: apiClient, rgbColors: nil)
    }

    var profileService: ProfileService {
        ProfileServiceImpl(apiClient: apiClient)
    }

    var startupClientIdentifierProvider: YMTStartupClientIdentifierProvider {
        YMTStartupClientIdentifierProvider(
            notificationCenter: .default,
            apiClient: internalApiClient,
            marketUIDHelper: marketUIDHelper,
            yandexUIDHelper: yandexUIDHelper,
            settings: settings
        )
    }

    var accountManager: YMTAccountManager {
        YMTAccountManager.configureApplication(with: startupClientIdentifierProvider)
        return YMTAccountManager()
    }

    var appRouter: AppRouter {
        AppRouter(with: YBMDeferredObject(provider: { YMTRoutes() }))
    }

    var userNotificationsManager: UserNotificationsManager {
        UserNotificationsManager(
            apiClient: apiClient,
            settings: settings,
            flagStorage: flagStorage,
            pushNotificationsService: PushNotificationsServiceStub(),
            testSettings: testSettings,
            scip: startupClientIdentifierProvider,
            notificationsManager: remoteNotificationManager
        )
    }

    var storage: Storage {
        .init(userDefaults: .standard)
    }

    var eventDispatcher: EventDispatcher {
        EventDispatcher()
    }

    var productCardViewControllerFactory: ProductCardViewControllerFactoryWrapper {
        ProductCardViewControllerFactoryWrapper(factory: nil)
    }

    var testSettings: TestSettings {
        TestSettings()
    }

    var remoteNotificationManager: RemoteNotificationManager {
        RemoteNotificationManager(
            appRouter: appRouter,
            deeplinkProcessorTracker: DeeplinkProccessorTracker(),
            chatKitManager: .init { self.chatKitManager }
        )
    }

    var chatKitManager: ChatKitManager {
        ChatKitManager(
            authenticator: ChatKitAuthenticator(accountManager: accountManager),
            persistentStorage: SyncPersistentStorageStub(),
            chatManagerSettingsStorage: ChatManagerSettingsStorage.shared,
            notificationPermissionAuthority: chatKitNotificationAuthorityManager,
            notificationCenter: .default
        )
    }

    var chatKitNotificationAuthorityManager: ChatKitNotificationAuthorityManager {
        ChatKitNotificationAuthorityManager(
            notificationsManager: userNotificationsManager,
            settings: settings
        )
    }
}
