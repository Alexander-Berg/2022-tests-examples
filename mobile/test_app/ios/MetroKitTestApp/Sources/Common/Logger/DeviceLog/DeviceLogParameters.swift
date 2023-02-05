//
//  DeviceLogParameters.swift
//  YandexMaps
//
//  Created by Alexander Goremykin on 09.03.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import CoreGraphics

extension DeviceLog {

    struct Parameters {
        static let logHistorySize = 200
        static let emptyScopeName = "Undetermined"

        static let contentMargin: CGFloat = 8
        static let buttonsSpacing: CGFloat = 16
        
        static let numberOfPages = 2
        static let logWindowHeightFraction: CGFloat = 0.5
        static let activeStateAlpha: CGFloat = 0.95
        static let transparentStateAlpha: CGFloat = 0.4
    }

}
