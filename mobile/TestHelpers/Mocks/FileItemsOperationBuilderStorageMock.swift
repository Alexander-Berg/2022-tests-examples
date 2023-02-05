//
//  FileItemsOperationBuilderStorageMock.swift
//  YandexDiskTests
//
//  Created by Mariya Kachalova on 13/03/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

@testable import YandexDisk

final class FileItemsOperationBuilderStorageMock: FileItemsOperationBuilderStorageProtocol {
    private(set) var renameFileItemCalled = false

    func deletePhotosliceItemsAndDropEmptyClusters(paths _: [String]) {}

    private(set) var deletionPaths: [String]?
    func updateFeedBlocksForItemsDeletion(paths: [String]) {
        deletionPaths = paths
    }

    private(set) var moves: [URL: URL]?
    func updateFeedBlocksForItemsMove(_ migrations: [URL: URL]) {
        moves = migrations
    }

    func renameFileItem(_: YOFileItem, with _: String) {
        renameFileItemCalled = true
    }

    func feedIDsForFiles(at _: [URL]) -> [String] {
        return []
    }

    private(set) var updateItemsCalled = false
    func updateItems(with _: Set<URL>) {
        updateItemsCalled = true
    }

    private(set) var updatePublicUrlCalled = false
    func updatePublicUrl(_: URL?, forItemAt _: URL) {
        updatePublicUrlCalled = true
    }

    func albumItemIds(albumId _: String, paths _: [String]) -> [String] {
        return []
    }
}
