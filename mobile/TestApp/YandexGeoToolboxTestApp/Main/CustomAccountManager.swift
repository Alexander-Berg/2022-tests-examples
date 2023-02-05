//
//  CustomAccountManager.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class CustomAccountManager: DefaultAccountManager {
    
    override class func config() -> Config {
        return Config(
            URLScheme: "yandexmaps",
            keyValueStorageScope: "AM",
            OAuthClientID: "0BzjHNKT48mDW5a7hymOrXuNn/oyi7zzSb9/Szs4K3BP8gko7kcw/zX+1Q73pDGO",
            OAuthClientSecret: "0BiyTYeW45yAUMbrh33c/K1sIrjHqR8+C2l9jcKTXunMVqnSab/rouTb0VE/oJoC")
    }
    
}