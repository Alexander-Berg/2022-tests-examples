//
//  IndexStorageMock.swift
//  14.03.2022
//

import Foundation
@testable import YandexDisk

final class IndexStorageMock: YDIndexStorage {
    private(set) var savedItem: YDIndexItem?
    private(set) var cleanedDirectoryUrl: URL?

    init(urlSettings: UrlSettings = BaseUrlSettings()) {
        let name = UserStorageConfig.createDefault(urlSettings: urlSettings).offlineIndexDBFileName
        let folderUrl = urlSettings.sqlStorageFolderURL
        super.init(filename: name, inFolder: folderUrl)
    }

    override func save(_ indexItem: YDIndexItem!) {
        savedItem = indexItem
    }

    override func cleanContentForDirectory(at url: URL!) {
        cleanedDirectoryUrl = url
    }
}
