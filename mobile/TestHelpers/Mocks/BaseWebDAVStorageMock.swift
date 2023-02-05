//
//  BaseWebDAVStorageMock.swift
//  25.02.2022
//

import Foundation
@testable import YandexDisk

class BaseWebDAVStorageMock: YOWebDAVStorage {
    override init(
        filename: String = "WebDAVStorage_mock.sqlite",
        inFolder: URL = PhotosliceTestServices.behavior.storageFolderURL,
        fileStorage: YDFileStorage = FileStorageMock()
    ) {
        super.init(
            filename: filename,
            inFolder: inFolder,
            fileStorage: fileStorage
        )
    }
}
