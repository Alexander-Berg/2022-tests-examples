//
//  Mocks.swift
//  YandexDiskTests
//
//  Created by Denis Kharitonov on 19.02.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
#if !DEV_TEST
@testable import YandexDisk
#endif

class FileProviderMock: NSObject, FileProviderProtocol {
    func file(for _: URL) -> YOFile {
        return YOFile()
    }

    func prefetchFile(with _: URL, completion _: ((Bool) -> Void)?) {}

    func resetCachedFiles() {}

    func isFileDownloaded(_: YOFile) -> Bool {
        return false
    }

    func localUrl(for file: YOFile) -> URL? {
        return file.url
    }
}
