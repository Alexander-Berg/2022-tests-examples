//
//  TestIndexItemBuilder.swift
//  YandexDisk
//
//  Created by Mariya Kachalova on 30/11/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

@testable import YandexDisk

final class TestIndexItemBuilder {
    private var item: YDIndexItem

    init(urlString: String) {
        self.item = YDIndexItem()
        item.url = URL(string: urlString)!
    }

    func setOperation(_ operation: YDIndexOperation) -> Self {
        item.operation = operation
        return self
    }

    func setIsDirectory(_ isDirectory: Bool) -> Self {
        item.isDirectory = isDirectory
        return self
    }

    func setMd5(_ md5: String) -> Self {
        item.md5 = md5
        return self
    }

    func setMpfsFileId(_ mpfsFileId: String) -> Self {
        item.mpfsFileId = mpfsFileId
        return self
    }

    func build() -> YDIndexItem {
        return item
    }
}
