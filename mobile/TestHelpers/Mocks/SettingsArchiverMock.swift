//
//  SettingsArchiverMock.swift
//  YandexDiskTests
//
//  Created by Valeriy Popov on 24/12/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

@testable import YandexDisk

final class SettingsArchiverMock<S>: SettingsArchiver {
    var didSetValue: (() -> Void)?
    var didResetValue: (() -> Void)?

    var value: S?

    func archive<T>(_: SettingKey<T>, value: T?) {
        self.value = value as? S
        didSetValue?()
    }

    func unarchive<T>(_: SettingKey<T>) -> T? {
        return value as? T
    }

    func reset<T>(_: SettingKey<T>) {
        value = nil
        didResetValue?()
    }
}
