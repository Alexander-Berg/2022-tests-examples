import BeruLegacyNetworking
import LangExtensions
import MarketPayment
import MarketProtocols
import MarketUITestMessaging

final class TestSettings: NSObject {

    override init() {
        let env = ProcessInfo.processInfo.environment

        if let deeplinkUrl = env[TestLaunchEnvironmentKeys.deeplinkUrl],
           let baseURL = URL(string: deeplinkUrl) {
            uiTestMessagingService = UITestMessagingServiceImpl(
                inboxPath: baseURL.appendingPathComponent(UITestMessagingConstants.beruInbox).path,
                outboxPath: baseURL.appendingPathComponent(UITestMessagingConstants.testsInbox).path
            )
        } else {
            uiTestMessagingService = nil
        }

        isRunningTests = env[TestLaunchEnvironmentKeys.insideUITests].ble_asBool()
        waitForExperiments = env[TestLaunchEnvironmentKeys.waitForExperiments].ble_asBool()
        animationsDisabled = env[TestLaunchEnvironmentKeys.animationsDisabled].ble_asBool(fallback: true)
        stubAuthorization = env[TestLaunchEnvironmentKeys.stubAuthorization].ble_asBool()
        hasYPlus = env[TestLaunchEnvironmentKeys.hasYPlus].ble_asBool()

        capiUrl = env[TestLaunchEnvironmentKeys.capiUrl]
        fapiUrl = env[TestLaunchEnvironmentKeys.fapiUrl]
        webViewPagesUrl = env[TestLaunchEnvironmentKeys.webViewPagesUrl]
        paymentUrl = env[TestLaunchEnvironmentKeys.paymentUrl].flatMap(URL.init(string:))

        geoSuggestUrl = env[TestLaunchEnvironmentKeys.geoSuggestUrl]
        iTunesLookupUrl = env[TestLaunchEnvironmentKeys.iTunesLookupUrl]
        trustUrl = env[TestLaunchEnvironmentKeys.trustUrl]
        enabledFeatureToggles = env[TestLaunchEnvironmentKeys.enabledToggles]?.components(separatedBy: ",") ?? []
        disabledFeatureToggles = env[TestLaunchEnvironmentKeys.disabledToggles]?.components(separatedBy: ",") ?? []
        enabledFeatureTogglesInfo = env[TestLaunchEnvironmentKeys.enabledTogglesInfo]?.ybm_asDictionary ?? [:]

        let locationLatitude = env[TestLaunchEnvironmentKeys.locationLatitude].flatMap(Double.init)
        let locationLongitude = env[TestLaunchEnvironmentKeys.locationLongitude].flatMap(Double.init)
        if let latitude = locationLatitude, let longitude = locationLongitude {
            location = CLLocation(latitude: latitude, longitude: longitude)
        }

        let currentTimeString = env[TestLaunchEnvironmentKeys.currentTime]
        currentTime = DateFormatter.hourMinuteSecond.date(from: currentTimeString ?? "")
        isRemoteNotificationsAllowed = env[TestLaunchEnvironmentKeys.isRemoteNotificationsAllowed]
            .ble_asBool(fallback: true)
        isIDFAEnabled = env[TestLaunchEnvironmentKeys.isIDFAEnabled].ble_asBool()
    }

    @objc let uiTestMessagingService: UITestMessagingService?

    @objc var stubAuthorization = false
    var hasYPlus = false
    var capiUrl: String?
    var fapiUrl: String?
    @objc var webViewPagesUrl: String?
    var geoSuggestUrl: String?
    var iTunesLookupUrl: String?
    @objc var trustUrl: String?
    var paymentUrl: URL?
    @objc var deviceToken = "965b351c 6cb1927d e3cb366f dfb16ded e6b9086a 8a3cac9e 5f85d679 376ea37C"
    @objc var uuidForPushes = "e18da448a6a24b568e09466d8990faee"
    var location: CLLocation?
    var isRunningTests = false
    @objc var waitForExperiments = false
    @objc var animationsDisabled = true
    var enabledFeatureToggles: [String] = []
    var disabledFeatureToggles: [String] = []
    var enabledFeatureTogglesInfo: [String: Any] = [:]
    var currentTime: Date?
    @objc var isRemoteNotificationsAllowed = true
    var isIDFAEnabled = false
}

// MARK: - NetworkingTestSettings

extension TestSettings: NetworkingTestSettings {}

// MARK: - PaymentTestSettings

extension TestSettings: PaymentTestSettings {}
