//
//  ListDataSourceMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 09.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREServiceInterfaces
import YREServiceLayer

final class ListDataSourceMock: NSObject, ListDataSourceProtocol {
    weak var delegate: YREListDataSourceDelegate?

    var chunkState: YREListChunkStateProtocol = YREListChunkState(asChunkedWithFirstChunk: 0)
    var isObtainingData: Bool = false

    func obtainDataChunk(withOptions options: Any?) {
    }

    func refreshIfNeeded() {
    }

    func reset() {
    }

    func cancelObtainingData() {
    }

    func numberOfSections() -> UInt {
        0
    }

    func numberOfItems(forSection section: UInt) -> UInt {
        0
    }

    func sectionInfo(forSection section: UInt) -> YREListDataSourceSectionInfo? {
        nil
    }

    func numberOfItems() -> UInt {
        0
    }

    func items(forSection section: UInt) -> [Any] {
        []
    }

    func item(at indexPath: IndexPath) -> Any? {
        nil
    }
}
