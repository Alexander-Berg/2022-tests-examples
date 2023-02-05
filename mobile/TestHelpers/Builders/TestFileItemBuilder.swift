//
//  TestFileItemBuilder.swift
//  YandexDisk
//
//  Created by Mariya Kachalova on 22/11/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

@testable import YandexDisk

class TestFileItemBuilder {
    fileprivate var item: YOFileItem

    init(urlString: String) {
        self.item = YOFileItem(url: URL(string: urlString)!)
    }

    func setPublicUrlString(_ urlString: String) -> Self {
        item.publicURL = URL(string: urlString)
        return self
    }

    func setLastModified(_ lastModified: Date) -> Self {
        item.lastModified = lastModified
        return self
    }

    func setReadonly(_ readonly: Bool) -> Self {
        item.readonly = readonly
        return self
    }

    func setOfflineStatus(_ status: YDOfflineStatus) -> Self {
        item.offlineStatus = status
        return self
    }

    func build() -> YOFileItem {
        return item
    }
}

final class TestFileBuilder: TestFileItemBuilder {
    override init(urlString: String) {
        super.init(urlString: urlString)
        item = YOFile(url: URL(string: urlString)!)
    }

    func setEtag(_ etag: String) -> Self {
        file.etag = etag
        return self
    }

    func setSize(_ size: UInt64) -> Self {
        file.size = size
        return self
    }

    func setETime(_ eTime: Date) -> Self {
        file.eTime = eTime
        return self
    }

    func buildFile() -> YOFile {
        return file
    }

    private var file: YOFile {
        return item as! YOFile
    }
}

final class TestDirectoryBuilder: TestFileItemBuilder {
    override init(urlString: String) {
        super.init(urlString: urlString)
        item = YODirectory(url: URL(string: urlString)!)
    }

    func buildDirectory() -> YODirectory {
        return directory
    }

    private var directory: YODirectory {
        return item as! YODirectory
    }
}
