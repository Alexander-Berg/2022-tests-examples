//
//  Bootstrapper.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 10/09/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import UIKit

class Bootstrapper {

    private var launchScreen: LaunchScreenController? = nil
    private var rootViewController: UIViewController
    
    private let appDelegate: AppDelegate
    private var appCtx: DefaultAppContext? = nil
    private var startupClientIdProvider: YXStartupClientIdentifierProvider? = nil
    
    private var window: UIWindow? = nil
    
    private var launchScreenContainment: ViewControllerContainmentHelper? = nil
    private var mainViewController: MainViewController? = nil
    private var mainContainment: ViewControllerContainmentHelper? = nil
    
    init(appDelegate: AppDelegate, rootViewController: UIViewController = UIViewController(),
         launchScreen: LaunchScreenController? = nil)
    {
        self.appDelegate = appDelegate
        self.rootViewController = rootViewController
        self.launchScreen = launchScreen
    }
    
    func load(completion: (() -> Void)? = nil) {
        setupWindowAndRootViewController()
        
        setLaunchScreenVisibleIfNeeded(visible: true, animated: false) { [weak self] _ in
            self?.loadServices { [weak self] in
                self?.loadUI { [weak self] in
                    self?.setLaunchScreenVisibleIfNeeded(visible: false, animated: true) { _ in
                        completion?()
                    }
                }
            }
        }
    }
    
    private func setupWindowAndRootViewController() {
        assert(window == nil)
        
        window = UIWindow(frame: UIScreen.main.bounds)
        appDelegate.window = window
        
        window?.rootViewController = rootViewController
        window?.makeKeyAndVisible()
    }
    
    private func setLaunchScreenVisibleIfNeeded(
        visible: Bool, animated: Bool = true, completion: ((Bool) -> Void)? = nil)
    {
        guard let launchScreen = self.launchScreen  else {
            dispatch(async: .main) { completion?(false) }
            return
        }
        let root = rootViewController
        
        launchScreenContainment = ViewControllerContainmentHelper(
            target: launchScreen.vc, parent: root, visible: visible)
        
        launchScreenContainment?.animationOptions = UIViewAnimationOptions.curveEaseIn
        launchScreenContainment?.animationDuration = 0.2
        
        if !visible {
            launchScreen.dismissAnimation()
        }
        
        if visible {
            launchScreenContainment?.add(animated: animated, completion: completion)
        }
        else {
            dispatch(after: 0.2) { [weak self] in
                self?.launchScreenContainment?.remove(animated: animated, completion: completion)
            }
        }
    }
    
    private func loadUI(completion: @escaping () -> Void) {
        guard let appCtx = self.appCtx else { assert(false); completion(); return }
        let root = rootViewController
        
        let main = MainViewController.makeInNavigation(usingAppCtx: appCtx)
        mainViewController = main.vc
        
        mainContainment = ViewControllerContainmentHelper(target: main.nav, parent: root, visible: false)
        mainContainment?.addView = { vc, parent in
            parent.view.insertSubview(vc.view, at: 0)
            vc.view.frame = parent.view.bounds
            vc.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        }
        mainContainment?.add(animated: false, completion: { _ in completion() })
    }

    private func loadServices(completion: @escaping () -> Void) {
        let root = rootViewController
        let mapKit = YMK.MapKit.sharedInstance
        let databaseManager = YDSDatabaseManagerInstance.value()
        let masstransitServicesFactory = DefaultMasstransitServicesFactory.instance
        
        startupClientIdProvider = YXMetricaStartupClientIdentifierProvider()
        startupClientIdProvider?.requestStartupClientIdentifier { [weak self] _cid, error in
            guard let cid = _cid else {
                assert(false)
                completion()
                return
            }
            
            mapKit.initialize(cid.uuid, deviceId: cid.deviceID)
            databaseManager.initialize(withUuid: cid.uuid, deviceId: cid.deviceID)
            
            masstransitServicesFactory.initialize(
                uuid: cid.uuid, deviceId: cid.deviceID, origin: "mobile.geotoolbox.ios")

            self?.appCtx = DefaultAppContext.Deps(
                startupClientID: cid, mapKit: Computed(mapKit),
                accountManager: Computed(CustomAccountManager.sharedInstance()),
                locationManager: Computed(mapKit.locationManager), rootViewController: root,
                databaseManager: databaseManager,
                masstransitServicesFactory: masstransitServicesFactory)
                .make()
            
            completion()
        }
    }
    
}
