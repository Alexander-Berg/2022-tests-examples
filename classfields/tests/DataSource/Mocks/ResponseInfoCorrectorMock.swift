//
//  ResponseInfoCorrectorMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 13.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModel
@testable import YREServiceLayer

final class ResponseInfoCorrectorMock: ResponseInfoCorrector {
    let correct: XCTestExpectation = .init(description: "Did correct")

    func correct(_ responseInfo: ResponseInfo) -> ResponseInfo {
        self.correct.fulfill()
        return .init(
            searchQuery: .init(logQueryId: "", logQueryText: "", sort: nil),
            pager: .init(page: 0, pageSize: 0, totalItems: 0, totalPages: 0)
        )
    }
}
