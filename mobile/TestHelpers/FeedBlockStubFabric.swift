//
//  FeedBlockStubFactory.swift
//  YandexDisk
//
//  Created by Denis Kharitonov on 31.07.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import Foundation
#if !DEV_TEST
@testable import YandexDisk
#endif

class FeedBlockStubFactory {
    static func makeContentFeedBlockStub(
        id: String? = nil,
        status: FeedBlockStatus? = nil,
        area: FeedBlockArea? = nil,
        date: Date? = Date(),
        filesCount: Int = 1
    ) -> FeedContentBlock {
        let block = FeedContentBlock(
            id: id ?? UUID().uuidString,
            type: .contentBlock,
            revision: 1,
            status: status ?? .new,
            collectionId: UUID().uuidString,
            order: 1,
            groupKey: UUID().uuidString,
            mtill: Date(),
            mfrom: date,
            mtime: nil,
            filesCount: filesCount,
            mediaType: MediaType.image.rawValue,
            action: nil,
            modifierUid: UUID().uuidString,
            modifierLogin: nil,
            folderId: UUID().uuidString,
            area: area
        )
        return block
    }

    static func makeCollectionStub(id: String? = nil, nextId: String? = nil) -> FeedCollectionProtocol {
        return FeedCollectionMock(collectionId: id ?? UUID().uuidString, nextCollectionId: nextId ?? UUID().uuidString)
    }

    static func makePhotoSelectionBlockStub(
        platforms: String? = nil,
        titles: [String: String] = ["title_en": "en"]
    ) -> FeedPhotoSelectionBlock {
        return FeedPhotoSelectionBlock(
            id: "id",
            type: .photoSelectionBlock,
            revision: 1,
            status: .new,
            collectionId: "1",
            order: 0,
            groupKey: "",
            mtill: Date(),
            mfrom: Date(),
            mtime: Date(),
            intervalStart: Date(),
            intervalEnd: Date(),
            photosliceDate: nil,
            resourceIds: ["1"],
            filesCount: 1,
            titles: titles,
            iconTypeRaw: nil,
            enabledPlatforms: platforms,
            subtype: nil
        )
    }
}

private struct FeedCollectionMock: FeedCollectionProtocol {
    let collectionId: String
    let nextCollectionId: String?
}
