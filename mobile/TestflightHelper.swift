//
//  TestflightHelper.swift
//  YandexMobileMail
//
//  Created by Nikita Ermolenko on 25/06/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation

internal func isTestflight() -> Bool {
    #if APPSTORE
        guard let appStoreReceiptURL = Bundle.main.appStoreReceiptURL else {
            return false
        }
        return appStoreReceiptURL.lastPathComponent == "sandboxReceipt"
    #else
        return false
    #endif
}
