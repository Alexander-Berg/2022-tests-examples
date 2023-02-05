//
//  AppDelegate.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 11/04/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?
    private(set) var bootstrapper: Bootstrapper? = nil


    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]? = nil) -> Bool
    {
        bootstrapper = Bootstrapper(
            appDelegate: self,
            rootViewController: RootViewController(),
            launchScreen: LaunchScreenController.makeFromStoryboard())
        
        bootstrapper?.load()
        
        CustomAccountManager.setupDefaultPresenter()
        return true
    }

}
