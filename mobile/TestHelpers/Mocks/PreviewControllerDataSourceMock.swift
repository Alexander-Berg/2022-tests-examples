//
//  PreviewControllerDataSourceMock.swift
//  YandexDiskTests
//
//  Created by Mariya Kachalova on 14/02/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

@testable import YandexDisk

final class PreviewControllerDataSourceMock: YDPreviewControllerDataSource {
    var currentPreviewItemIndex: Int
    let numberOfPreviewItems: Int
    private let items: [String]

    init(items: [String], currentIndex: Int = 0) {
        self.items = items
        self.currentPreviewItemIndex = currentIndex
        self.numberOfPreviewItems = items.count
    }

    func previewItem(at index: Int) -> YDPreviewItem? {
        return YDFilePreviewItem(file: YOFile(url: URL(string: items[index])!))
    }

    func indexOf(previewItem item: YDPreviewItem) -> Int {
        return items.firstIndex(of: item.remoteURL!.path) ?? NSNotFound
    }

    func updateCurrentPreviewItemIndex(_ index: Int, isInitial _: Bool) {
        currentPreviewItemIndex = index
    }
}
