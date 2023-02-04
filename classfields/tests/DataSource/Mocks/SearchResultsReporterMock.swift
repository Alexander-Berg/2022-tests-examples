//
//  SearchResultsReporterMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 09.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModel
import YREServiceLayer

final class SearchResultsReporterMock: SearchResultsReporterProtocol {
    let report: XCTestExpectation = .init(description: "Did report")

    func report(_ responseInfo: ResponseInfo) {
        self.report.fulfill()
    }
}
