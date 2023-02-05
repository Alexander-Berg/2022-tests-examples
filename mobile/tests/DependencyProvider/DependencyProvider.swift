import BeruCore
import BeruLegacyNetworking
import MarketProtocols

final class DependencyProvider {

    var legacyAPIClient: YMTAPIClient {
        // Перед созданием YMTAPIClient, передаем моки зависимостей в модуль BeruLegacyNetworking,
        // чтобы не срабатывали NSAssert
        NetworkingDependencyProvider.shared.settings = SettingsMock()
        NetworkingDependencyProvider.shared.networkingSettings = NetworkingSettingsMock()
        NetworkingDependencyProvider.shared.testSettings = TestSettingsMock()
        NetworkingDependencyProvider.shared.rearrFactorsProvider = RearrFactorsProviderMock()
        NetworkingDependencyProvider.shared.hyperlocalParamsProvider = NetworkingHyperlocalParamsProviderMock()
        NetworkingDependencyProvider.shared.jwsProvider = NetworkingJWSProviderMock()

        let apiClient = YMTAPIClient(
            baseURL: YMTAPIClientMobileContentBaseURL(YMTAPIVersion.YMTAPIVersion1)
        )
        YMTAPIClient.setSharedInstance(apiClient)
        apiClient.uuid = UUID().uuidString

        return apiClient
    }

    var apiClient: APIClient {
        APIClient(apiClient: legacyAPIClient)
    }

    var flagStorage: ServicesFlagStorageProtocol {
        FlagStorageMock()
    }

    var storage: Storage {
        Storage(userDefaults: .standard)
    }
}

// MARK: - Mocks

private extension DependencyProvider {
    final class SettingsMock: NetworkingLayerSettings {

        var allowDebugMode: Bool {
            false
        }

        var currentUserLogin: String? {
            nil
        }

        var selectedRegionID: NSNumber? {
            nil
        }
    }

    final class NetworkingSettingsMock: NetworkingLayerNetworkingSettings {

        var productionWhiteFapiEndpoint: Bool {
            true
        }

        var testingWhiteFapiEndpoint: Bool {
            false
        }

        var useLocalCAPI: Bool {
            false
        }

        var useLocalMAPI: Bool {
            false
        }

        var productionEndpoint: Bool {
            true
        }

        var prestableEndpoint: Bool {
            false
        }

        var testingEndpoint: Bool {
            false
        }

        var productionFapiEndpoint: Bool {
            true
        }

        var testingFapiEndpoint: Bool {
            false
        }

        var FAPIEndpointURL: URL? {
            nil
        }

        var whiteFAPIEndpointURL: URL? {
            nil
        }

        var disableRequestsCache: Bool {
            false
        }

        var verifySSLCerts: Bool {
            false
        }

    }

    final class TestSettingsMock: NetworkingTestSettings {
        var isRunningTests: Bool {
            false
        }

        var fapiUrl: String? {
            nil
        }

        var capiUrl: String? {
            nil
        }

        var trustUrl: String? {
            nil
        }
    }

    final class RearrFactorsProviderMock: NetworkingRearrFactorsProvider {
        var rearrFactors: String {
            ""
        }
    }

    final class NetworkingHyperlocalParamsProviderMock: NetworkingHyperlocalParamsProvider {
        let coordinate: String? = nil
        let coordinateCanonical: String? = nil
    }

    final class NetworkingJWSProviderMock: NetworkingJWSProvider {
        let jwsHeader: String? = nil
    }

    final class FlagStorageMock: ServicesFlagStorageProtocol {
        var welcomeOnboardingShown = true
        var activationCount = 1
        var userHasDeclinedSubscription = false
        var wishlistVisited = false
        var wishlistHintShown = false
        var lastBonusCount: Int = 0
        var seenBonusCount: Int = 0
        var isLavkaNewBadgeShown = false
        var lavkaVisitedTime = 0.0
    }
}
