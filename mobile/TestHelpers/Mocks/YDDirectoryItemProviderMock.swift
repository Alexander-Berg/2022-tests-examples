//
//  YDDirectoryItemProviderMock.swift
//  YandexDiskTests
//
//  Created by Denis Kharitonov on 19.02.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
#if !DEV_TEST
@testable import YandexDisk
#endif

class YDDirectoryItemProviderMock: NSObject, FileItemProviderStorageProtocol {
    var shoudldNotFindDirectory = false
    var rootURL = URL(string: "https://webdav.yandex.ru/disk/")!

    func fileItemAtURL(_ url: URL, useCache _: Bool) -> YOFileItem? {
        return shoudldNotFindDirectory ? nil : YODirectory(url: url)
    }

    func fileItems(at urls: Set<URL>!, useCache _: Bool) -> Set<YOFileItem>! {
        let dirs = urls.map { YODirectory(url: $0) }
        return Set(dirs)
    }
}
