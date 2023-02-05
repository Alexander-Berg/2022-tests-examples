//
//  MainViewController+StartupDebug.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 27/02/2019.
//  Copyright © 2019 Yandex LLC. All rights reserved.
//

import Foundation


extension MainViewController {

    func checkStartupConfig() {
        let startupController = MetroKit.instance.startupController
        startupController.addListener(withStartupListener: self)
        
        if let config = MetroKit.instance.startupController.config {
            MainViewController.print(config)
        }
        
        startupController.updateConfig()
    }
    
    private static func print(_ config: YMLStartupConfig) {
        guard let iosConfig = YMLStartupMetroConfigFactory.makeIosConfig(with: config) else { return }
        
        Swift.print("""
        🚀 \tMetroKit Startup Config: {
            \tminAllowedAppVersion: \(iosConfig.commonConfig.minAllowedAppVersion),
            \tratingPromptProbability: \(iosConfig.commonConfig.ratingPromptProbability)
        \t}
        """)
    }

}

extension MainViewController: YMLStartupControllerListener {
    
    func onConfigUpdate(with config: YMLStartupConfig) {
        MainViewController.print(config)
    }
    
}


