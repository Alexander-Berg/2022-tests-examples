//
//  AppDelegate.swift
//  GuidanceLibTestApp
//
//  Created by Dmitry Konygin on 7/1/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import UIKit
import YandexMapsMobile
import YandexNaviKit
import YandexNaviStrings
import AVFoundation
import YandexNaviDayNight

private class NightModeProviderImpl: NSObject, YNKPlatformNightModeProvider, NightModeProvider {
    private let isNightImpl = false

    override init() {
        super.init()
        DayNight.initialize(self)
    }

    var isNight: Bool { return isNightImpl }

    func bindListener(_ nightModeListener: YNKNativeNightModeListener) {
    }

    func isNightMode() -> NSNumber? {
        return NSNumber(booleanLiteral: isNightImpl)
    }
}

private class SoundMuterImpl: NSObject, YNKSoundMuter {
    func isMuted() -> Bool {
        return false
    }

    func setMuted(_ muted: Bool) {
    }
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    static var shared: AppDelegate { return UIApplication.shared.delegate as! AppDelegate }

    var window: UIWindow?

    private let nightModeProvider = NightModeProviderImpl()

    private let lang = YMKAnnotationLanguage.russian
    private let speaker: SpeakerImpl

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

        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        YMKMapKit.sharedInstance().onStart()
        guidance.onStart()
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        guidance.onPause(true)
    }

    func application(_ application: UIApplication, handleEventsForBackgroundURLSession identifier: String, completionHandler: @escaping () -> Void) {
        YMKMapKit.sharedInstance().setCompletionHandler(completionHandler, forBackgroundURLSession: identifier)
    }
}
