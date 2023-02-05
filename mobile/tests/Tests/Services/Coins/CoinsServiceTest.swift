import MarketProtocols
import OHHTTPStubs
import XCTest

@testable import BeruServices

class CoinsServiceTest: NetworkingTestCase {

    var coinsService: CoinsService!
    var isAuthenticated: Bool!

    override func setUp() {
        super.setUp()

        isAuthenticated = true

        let dependencyProvider = DependencyProvider()
        let isAuthenticated: () -> Bool = {
            self.isAuthenticated
        }

        coinsService = CoinsServiceImpl(
            apiClient: dependencyProvider.apiClient,
            flagStorage: dependencyProvider.flagStorage,
            isAuthenticated: isAuthenticated,
            notificationCenter: .default
        )
    }

    override func tearDown() {
        coinsService = nil
        isAuthenticated = nil
        super.tearDown()
    }
}

// MARK: - Nested types

extension CoinsServiceTest {
    struct Bonus {
        let id: String
        let title: String?
        let subtitle: String?
        let bindingStatus: CoinBindingStatus?
        let nominal: NSNumber?
        let isHiddenUntilBound: Bool
        let isForPlus: Bool

        init(
            id: String,
            title: String?,
            subtitle: String?,
            bindingStatus: CoinBindingStatus?,
            nominal: NSNumber?,
            isHiddenUntilBound: Bool = false,
            isForPlus: Bool = false
        ) {
            self.id = id
            self.title = title
            self.subtitle = subtitle
            self.bindingStatus = bindingStatus
            self.nominal = nominal
            self.isHiddenUntilBound = isHiddenUntilBound
            self.isForPlus = isForPlus
        }
    }
}
