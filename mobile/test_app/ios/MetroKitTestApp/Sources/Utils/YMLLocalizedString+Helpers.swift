//
//  YMLLocalizedString+Helpers.swift
//  MetroKitTestApp
//
//  Created by Konstantin Kiselev on 12.03.18.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation


extension YMLLocalizedString {

    private func valueForLanguage(_ l: YMLLanguage) -> String? {
        return YMLL10nManager.getStringWith(self, language: l)
    }

    func getStringUsingSystemLanguage() -> String {
        return valueForLanguage(YMLLanguage(value: YXPlatformCurrentState.currentLanguage() ?? "en"))
            ?? map?.defaultValue
            ?? nonlocalizedValue!
    }

}
