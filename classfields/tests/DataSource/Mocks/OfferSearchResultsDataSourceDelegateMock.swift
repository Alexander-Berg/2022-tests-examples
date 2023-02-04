//
//  OfferSearchResultsDataSourceDelegateMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Pavel Zhuravlev on 06.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRECoreUtils
import YREServiceInterfaces
@testable import YREServiceLayer

final class OfferSearchResultsDataSourceDelegateMock: NSObject {
    var onDidStartObtaining: XCTestExpectation = .init(description: "Did start obtaining")

    var onDidFinishObtaining: XCTestExpectation = .init(description: "Did finish obtaining")
    var onDidFinishObtainingWithTaskState: XCTestExpectation = .init(description: "Did finish obtaining with task state")

    var onDidFailObtaining: XCTestExpectation = .init(description: "Did fail obtaining")
    var onDidCancelObtaining: XCTestExpectation = .init(description: "Did cancel obtaining")

    var onDidUpdateInternalState: XCTestExpectation = .init(description: "Did update internal state")

    var onDidStartRefreshing: XCTestExpectation = .init(description: "Did start refreshing")
    var onDidFinishRefreshing: XCTestExpectation = .init(description: "Did finish refreshing")
}

extension OfferSearchResultsDataSourceDelegateMock: YREListDataSourceDelegate {
    func listDataSource(_ dataSource: ListDataSourceProtocol, didStartObtainingDataWithDetails details: Any?) {
        self.onDidStartObtaining.fulfill()
    }

    func listDataSource(_ dataSource: ListDataSourceProtocol,
                        didFinishObtainingDataWithDetails details: Any?) {
        self.onDidFinishObtaining.fulfill()
    }

    func listDataSource(_ dataSource: ListDataSourceProtocol,
                        didFinishObtainingDataWithDetails details: Any?,
                        taskState: YRETaskStateProtocol) {
        self.onDidFinishObtainingWithTaskState.fulfill()
    }

    func listDataSource(_ dataSource: ListDataSourceProtocol, didFailToObtainDataWithDetails details: Any?, error: Error?) {
        self.onDidFailObtaining.fulfill()
    }

    func listDataSource(_ dataSource: ListDataSourceProtocol, didUpdateInternalStateWith updates: ListUpdatesContainerProtocol) {
        self.onDidUpdateInternalState.fulfill()
    }

    func listDataSourceDidCancelObtainingData(_ dataSource: ListDataSourceProtocol) {
        self.onDidCancelObtaining.fulfill()
    }

    func listDataSourceDidStartRefreshing(_ dataSource: ListDataSourceProtocol) {
        self.onDidStartRefreshing.fulfill()
    }

    func listDataSourceDidFinishRefreshing(_ dataSource: ListDataSourceProtocol) {
        self.onDidFinishRefreshing.fulfill()
    }
}
