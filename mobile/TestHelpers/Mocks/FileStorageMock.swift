//
//  FileStorageMock.swift
//  16.02.2022
//

import Foundation
@testable import YandexDisk

class FileStorageMock: YDFileStorage {
    private var downloadedFiles = Set<YOFile>()

    init() {
        let config = UserStorageConfig.createDefault(urlSettings: BaseUrlSettings())
        super.init(storageConfig: config)
    }

    func markFileDownloaded(_ file: YOFile) {
        downloadedFiles.insert(file)
    }

    override func isDownloaded(_ file: YOFile) -> Bool {
        return downloadedFiles.contains(file)
    }
}
