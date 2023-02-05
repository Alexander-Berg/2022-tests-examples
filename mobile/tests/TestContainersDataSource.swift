//
//  TestContainersDataSource.swift
//  FiltersTests
//
//  Created by Timur Turaev on 15.11.2021.
//

import Foundation
import Utils
@testable import Filters

internal final class TestContainersDataSource: ContainersDataSource {
    func folderNameByID(_ id: YOIDType) -> String? {
        return "\(id)"
    }

    func labelNameByID(_ id: YOIDType) -> String? {
        return "\(id)"
    }
}
