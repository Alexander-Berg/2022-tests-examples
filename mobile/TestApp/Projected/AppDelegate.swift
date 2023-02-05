//
//  AppDelegate.swift
//  GuidanceLibTestApp
//
//  Created by Dmitry Konygin on 7/1/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import AVFoundation
import CarPlay
import UIKit
import YandexMapsMobile
import YandexNaviKit
import YandexNaviStrings
import YandexNaviDayNight
import YandexNaviProjectedUI
import YandexNaviSearch
import YandexNaviProjectedLibDeps
import YandexMapsEventTracker
import YandexMapsMocks
import YandexMapsDeps
import KotlinNative

private class NightModeProviderImpl: NSObject, YNKPlatformNightModeProvider, NightModeProvider {
    private let isNightImpl = false

    override init() {
        super.init()
        makeMainProvider()
    }

    var isNight: Bool { return isNightImpl }

    func bindListener(_ nightModeListener: YNKNativeNightModeListener) {
    }

    func isNightMode() -> NSNumber? {
        return NSNumber(booleanLiteral: isNightImpl)
    }

    func makeMainProvider() {
        DayNight.initialize(self)
        NotificationCenter.default.post(name: DayNight.switchNotification, object: nil)
    }
}

private class SoundMuterImpl: NSObject, YNKSoundMuter {
    func isMuted() -> Bool {
        return false
    }

    func setMuted(_ muted: Bool) {
    }
}

private class CarPlayInitProviderImpl: CarPlaySessionInitProvider {
    let bookmarkProvider: YNKBookmarksProvider = BookmarksProviderStub()
    let placesProvider: ObservablePlacesProvider = PlacesProviderStub()
    let annotationsPlayer: YNKAnnotationsPlayer = AnnotationsPlayerStub()
    let guidance: YNKGuidance
    let searchDepsFactory: CarPlaySearchDependenciesFactory
    let areTrafficJamsVisibleSetting: YNKBooleanSetting = BooleanSettingStub()
    let annotationSetting: AnnotationsSetting = AnnotationsSettingStub()
    let soundVolumeSetting: SoundVolumeSetting? = SoundVolumeSettingStub()
    let availableRoadEventsProvider: YNKAvailableRoadEventsProvider = AvailableRoadEventsProviderStub()
    let cameraTransformStorage: YNKPlatformCameraTransformStorage = CameraTransformStorageStub()
    let microphonePermission: Permission = PermissionStub()

    let reportDelegate: ReportDelegate = {
        class ReportDelegateImpl: ReportDelegate {
            func report(event: String) {
                print(event)
            }

            func report(event: String, params: [String: String]) {
                print("\(event) \(params)")
            }
        }
        return ReportDelegateImpl()
    }()

    let textVocalizer: CarPlayTextVocalizer? = {
        class Impl: CarPlayTextVocalizer {
            let synthesizer = AVSpeechSynthesizer()
            func vocalize(text: String, completion: @escaping (_ success: Bool) -> Void) {
                let utterance = AVSpeechUtterance(string: text)
                synthesizer.speak(utterance)
            }
        }
        return Impl()
    }()

    init(guidance: YNKGuidance, searchDepsFactory: CarPlaySearchDependenciesFactory) {
        self.guidance = guidance
        self.searchDepsFactory = searchDepsFactory
    }
}

@UIApplicationMain
class AppDelegate: UIResponder, CPApplicationDelegate {

    func application(_ application: UIApplication, didConnectCarInterfaceController interfaceController: CPInterfaceController, to window: CPWindow) {
        carPlay.attachToCarPlay(interfaceController: interfaceController, window: window)
    }

    func application(_ application: UIApplication, didDisconnectCarInterfaceController interfaceController: CPInterfaceController, from window: CPWindow) {
        carPlay.detachFromCarPlay()
        nightModeProvider.makeMainProvider()
    }

    static var shared: AppDelegate { return UIApplication.shared.delegate as! AppDelegate }

    var window: UIWindow?
    public lazy var carPlay = CarPlay(
        licenseRestriction: licenseRestriction,
        versionRestriction: versionRestriction,
        permissionRestriction: permissionRestriction,
        forcePhoneUsageRestriction: forcePhoneUsageRestriction,
        generalRestriction: RestrictionProvider<GeneralRestriction>(initialRestriction: .none),
        initProvider: initProvider)

    fileprivate lazy var initProvider = CarPlayInitProviderImpl(
        guidance: guidance,
        searchDepsFactory: searchDepsFactory
    )
    fileprivate let nightModeProvider = NightModeProviderImpl()

    private let lang = YMKAnnotationLanguage.russian
    private let speaker: SpeakerImpl

    private let experimentsExternalStorage: NaviExperimentsExternalStorage = ExperimentsExternalStorageMock()
    private let featureProvder: NaviFeatureProvider = FeatureProviderMock()
    private lazy var naviExperimentsProvider: NaviExperimentsProvider = {
        NaviExperimentsProvider(
            naviExperimentsExternalStorage: experimentsExternalStorage,
            naviFeatureProvder: featureProvder
        )
    }()
    private lazy var searchDepsFactory: CarPlaySearchDependenciesFactory = CarPlaySearchDependenciesFactoryImpl(
        searchHistory: SearchHistoryMock(),
        speechRecognizer: SpeechRecognizerMock(),
        searchIdentifiersProvider: SearchIdentifiersProviderMock(),
        genericEventTracker: FakeGenericEventTracker(),
        cpAppProvider: CPAppProviderImpl(),
        mobmapsProxyHost: MobmapsProxyHost.companion.PROD,
        userAgentInfoProvider: FakeUserAgentInfoProvider(),
        oAuthTokenProvider: FakeRxOAuthTokenProvider(),
        application: KotlinNative.Application.navi,
        customizableAssetsProviderFactory: CustomizableAssetsProviderFactoryImpl.createNaviInstance(
            featureProvider: featureProvder,
            externalStorage: experimentsExternalStorage
        ),
        searchCategoriesExperimentProvider: naviExperimentsProvider
    )

    public lazy var guidance: YNKGuidance = {
        let guidance = YNKNaviKitLibrary.createGuidance(
            with: nightModeProvider,
            freedriveDataProvider: nil,
            soundMuter: SoundMuterImpl(),
            bgGuidanceDelegate: nil,
            bgActivityTracker: nil,
            complexJunctionsConfigDataProvider: nil)
        guidance.configurator().setLocalizedSpeakerWith(speaker, language: lang)
        guidance.configurator().setSpeakerLanguageWith(lang)
        guidance.configurator().setRoadEventsAnnotatedWithOn(true)
        guidance.configurator().setRouteActionsAnnotatedWithOn(true)
        guidance.bgConfigurator().setBackgroundOnRouteEnabledWithOn(true)
        return guidance
    }()

    let licenseRestriction = RestrictionProvider<LicenseRestriction>(initialRestriction: .none)
    let versionRestriction = RestrictionProvider<CarPlayAppVersionRestriction>(initialRestriction: .none)
    let permissionRestriction = RestrictionProvider<PermissionRestriction>(initialRestriction: .none)
    let forcePhoneUsageRestriction = RestrictionProvider<ForcePhoneUsageRestriction>(initialRestriction: .none)

    override init() {
        speaker = SpeakerImpl { _ in }
        speaker.setLanguage(lang)
        super.init()
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        YMKMapKit.setApiKey("36614185-aa6d-4547-8ddf-ef1df302b533")
        YMKMapKit.sharedInstance().styleType = .vNav2

        YNKNaviKitLibrary.initRoutePreprocessing()
        YNKLocalizedString.setResourceBundle(NaviResourceBundleProvider.bundle)

        guidance.bgGuidanceController().setBackgroundNotificationEnabledWithOn(true)

        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.playback, options: [.mixWithOthers])
            try audioSession.setActive(true)
        } catch {
            print("Failed to set audio session category and/or activate")
        }

        let window = UIWindow(frame: UIScreen.main.bounds)

        self.window = window
        window.rootViewController = TestAppMapViewController()
        window.makeKeyAndVisible()

        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        didBecomeActive()
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        didEnterBackground()
    }

    func application(_ application: UIApplication, handleEventsForBackgroundURLSession identifier: String, completionHandler: @escaping () -> Void) {
        YMKMapKit.sharedInstance().setCompletionHandler(completionHandler, forBackgroundURLSession: identifier)
    }

    fileprivate func didBecomeActive() {
        YMKMapKit.sharedInstance().onStart()
        guidance.onStart()
    }

    fileprivate func didEnterBackground() {
        guidance.onPause(false)
    }
}

@available(iOS 13.0, *)
class WindowSceneDelegate: NSObject, UIWindowSceneDelegate {
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = scene as? UIWindowScene, session.configuration.name == "WindowSceneConfiguration" else { return }
        AppDelegate.shared.window?.windowScene = windowScene
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        AppDelegate.shared.didBecomeActive()
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        AppDelegate.shared.didEnterBackground()
    }
}

// MARK: CPTemplateApplicationSceneDelegate
class TemplateApplicationSceneDelegate: NSObject, CPTemplateApplicationSceneDelegate {

    @available(iOS 13.0, *)
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                  didConnect interfaceController: CPInterfaceController, to window: CPWindow) {
        AppDelegate.shared.carPlay.attachToCarPlay(interfaceController: interfaceController, window: window)
    }

    @available(iOS 13.0, *)
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                  didDisconnect interfaceController: CPInterfaceController, from window: CPWindow) {
        let appDelegate = AppDelegate.shared
        appDelegate.carPlay.detachFromCarPlay()
        appDelegate.nightModeProvider.makeMainProvider()
    }
}

@available(iOS 13.4, *)
extension TemplateApplicationSceneDelegate: CPTemplateApplicationDashboardSceneDelegate {

    func templateApplicationDashboardScene(
        _ templateApplicationDashboardScene: CPTemplateApplicationDashboardScene,
        didConnect dashboardController: CPDashboardController,
        to window: UIWindow)
    {
        AppDelegate.shared.carPlay.attachToDashboardCarPlay(with: dashboardController, window: window)
    }

    func templateApplicationDashboardScene(
        _ templateApplicationDashboardScene: CPTemplateApplicationDashboardScene,
        didDisconnect dashboardController: CPDashboardController,
        from window: UIWindow)
    {
        AppDelegate.shared.carPlay.detachFromDashboardCarPlay()
    }
}


class CPAppProviderImpl: CPAppProvider {
    func app() -> CPApp {
        return .navi
    }
}
