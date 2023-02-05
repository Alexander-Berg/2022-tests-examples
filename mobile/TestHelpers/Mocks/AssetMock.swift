//
//  AssetMock.swift
//  YandexDiskTests
//
//  Created by Mariya Kachalova on 31.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

final class AssetMock: PHAsset {
    private let id: String
    private let isLivephoto: Bool
    private let testModificationDate: Date?

    init(id: String, isLivephoto: Bool = false, modificationDate: Date? = nil) {
        self.id = id
        self.isLivephoto = isLivephoto
        self.testModificationDate = modificationDate
    }

    override var localIdentifier: String {
        return id
    }

    override var mediaSubtypes: PHAssetMediaSubtype {
        return isLivephoto ? .photoLive : []
    }

    override var modificationDate: Date? {
        return testModificationDate
    }
}
