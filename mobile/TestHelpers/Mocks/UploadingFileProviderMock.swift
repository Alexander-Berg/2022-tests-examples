//
//  UploadingFileProviderMock.swift
//  YandexDiskTests
//
//  Created by Denis Kharitonov on 26.02.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
#if !DEV_TEST
@testable import YandexDisk
#endif

/// pretty dumb and straightforward uploading provider mock. If url starts with "upload" - it's uploading
final class UploadingFileProviderMock: UploadingFileItemProviderProtocol {
    func uploadFileAtURL(_ url: URL, useCache _: Bool) throws -> YOUploadFile {
        if url.absoluteString.hasSuffix("upload") {
            return YOUploadFile(url: url, type: .default, resourceType: .default)
        } else {
            throw NSError(domain: "test", code: 0, userInfo: nil)
        }
    }
}
