//
//  MetroKit.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 27/02/2019.
//  Copyright © 2019 Yandex LLC. All rights reserved.
//

import Foundation

class MetroKit {

    static let instance: YMLMetroKit = {
        let builder = YMLMetroKitFactory.builder()
        builder.setAppNameWithAppName("testapp")
        builder.setAppVersionWithAppVersion("100")
        return builder.build(
            with: YMLLanguage(value: YXPlatformCurrentState.currentLanguage()),
            countryCode: YMLCountryCode(value: YXPlatformCurrentState.countryCode() ?? NSLocale.current.regionCode ?? "US")
        )
    }()

    private init() {
    }

}
