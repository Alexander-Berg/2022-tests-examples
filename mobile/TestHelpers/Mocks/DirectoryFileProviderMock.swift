//
//  DirectoryFileProviderMock.swift
//  YandexDiskTests
//
//  Created by Denis Kharitonov on 19.02.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
#if !DEV_TEST
@testable import YandexDisk
#endif

class DirectoryFileProviderMock: YDComposedFileItemProviderProtocol {
    var shouldNotFindItem = false
    var shouldReturnDirectory = false

    func file(for url: URL) -> YOFileItem? {
        if shouldNotFindItem == false {
            return shouldReturnDirectory ? YODirectory(url: url) : YOFile(url: url)
        } else {
            return nil
        }
    }
}
