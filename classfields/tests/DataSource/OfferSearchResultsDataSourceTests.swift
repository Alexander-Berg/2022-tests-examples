//
//  OfferSearchResultsDataSourceTests.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Pavel Zhuravlev on 05.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModel
import YREModelObjc
import YREServiceInterfaces
@testable import YREServiceLayer

final class OfferSearchResultsDataSourceTests: XCTestCase {
    static let pageSize: Int = 20
    static let promotedOffersCount: Int = 3
    static let timeout: TimeInterval = 0.1
}

// MARK: - Common Data Management

extension OfferSearchResultsDataSourceTests {
    func testCommonPagination() {
        let firstPageSize = Self.pageSize + Self.promotedOffersCount
        let secondPageSize = Self.pageSize - 5

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: firstPageSize + secondPageSize,
            pageSize: Self.pageSize,
            promotedOffersCount: Self.promotedOffersCount
        )

        // Load first page
        let delegateMockPage1 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage1
        dataSource.obtainDataChunk(withOptions: nil)

        XCTAssertTrue(dataSource.isObtainingData)

        // We can move forward - check data source state or call other methods - only after `waitForCommonCallbacks` is completed
        self.waitForCommonCallbacks(using: delegateMockPage1)

        XCTAssertFalse(dataSource.isObtainingData)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: firstPageSize)

        // Load second page
        // We have to create a new instance of DelegateMock because some of its XCTestExpectations have been already used
        let delegateMockPage2 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage2
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockPage2)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: firstPageSize + secondPageSize)

        // Load next (empty) page
        let delegateMockPage3 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage3
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockPage3, invertExpectations: true) // No callbacks on empty page
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: firstPageSize + secondPageSize)
    }

    func testPaginationWithError() {
        let dataSource = Self.makeOfferSnippetDataSource(
            specificError: nil
        )
        let delegateMock = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMock

        let expectedHandlers: [XCTestExpectation] = [
            delegateMock.onDidStartObtaining,
            delegateMock.onDidFailObtaining,
            delegateMock.onDidFinishObtainingWithTaskState,
        ]
        let unexpectedHandlers: [XCTestExpectation] = [
            delegateMock.onDidFinishObtaining,
            delegateMock.onDidUpdateInternalState,
            delegateMock.onDidCancelObtaining,
        ]

        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForExpectations(expectedHandlers)
        self.waitForExpectations(unexpectedHandlers, invertExpectations: true)

        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: 0)
    }

    func testLoadingCancelation() {
        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: Self.pageSize * 2,
            pageSize: Self.pageSize,
            promotedOffersCount: 0
        )

        // Load first page
        let delegateMockPage1 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage1
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockPage1)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: Self.pageSize)

        // Load second page
        let delegateMockPage2 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage2
        dataSource.obtainDataChunk(withOptions: nil)
        dataSource.cancelObtainingData()

        let expectedHandlers: [XCTestExpectation] = [
            delegateMockPage2.onDidStartObtaining,
            delegateMockPage2.onDidCancelObtaining,
            delegateMockPage2.onDidFinishObtainingWithTaskState,
        ]
        let unexpectedHandlers: [XCTestExpectation] = [
            delegateMockPage2.onDidFailObtaining,
            delegateMockPage2.onDidFinishObtaining,
            delegateMockPage2.onDidUpdateInternalState,
        ]

        self.waitForExpectations(expectedHandlers)
        self.waitForExpectations(unexpectedHandlers, invertExpectations: true)

        // The same items count as before
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: Self.pageSize)
    }

    func testDataSourceResetting() {
        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: Self.pageSize * 2,
            pageSize: Self.pageSize,
            promotedOffersCount: 0
        )

        // Load first page
        let delegateMockPage1 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage1
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockPage1)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: Self.pageSize)

        // Set callbacks handler and reset the inner state
        let delegateMock = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMock

        dataSource.reset()

        let expectedHandlers: [XCTestExpectation] = [
            delegateMock.onDidUpdateInternalState,
        ]
        let unexpectedHandlers: [XCTestExpectation] = [
            delegateMock.onDidStartObtaining,
            delegateMock.onDidCancelObtaining,
            delegateMock.onDidFailObtaining,
            delegateMock.onDidFinishObtainingWithTaskState,
            delegateMock.onDidFinishObtaining,
        ]

        self.waitForExpectations(expectedHandlers)
        self.waitForExpectations(unexpectedHandlers, invertExpectations: true)

        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: 0)
    }

    func testInvalidationOnAuthStateChanged() {
        let authStateObservable = AuthStateObservableMock()
        authStateObservable.authState.isAuthorized = false

        let firstPageSize = Self.pageSize + Self.promotedOffersCount
        let secondPageSize = Self.pageSize - 5

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: firstPageSize + secondPageSize,
            pageSize: Self.pageSize,
            promotedOffersCount: Self.promotedOffersCount,
            authStateObservable: authStateObservable
        )

        // Load first page
        let delegateMockPage1 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage1
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockPage1)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: firstPageSize)

        // Load second page
        // We have to create a new instance of DelegateMock because some of its XCTestExpectations have been already used
        let delegateMockPage2 = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockPage2
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockPage2)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: firstPageSize + secondPageSize)

        // Set callbacks handler and reset the inner state
        let delegateMock = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMock

        // Change auth state
        authStateObservable.authState.isAuthorized = true
        authStateObservable.notifyObserver()

        let expectedHandlers: [XCTestExpectation] = [
            delegateMock.onDidUpdateInternalState,
            delegateMock.onDidStartObtaining,
            delegateMock.onDidFinishObtaining,
            delegateMock.onDidFinishObtainingWithTaskState,
        ]

        let unexpectedHandlers: [XCTestExpectation] = [
            delegateMock.onDidCancelObtaining,
            delegateMock.onDidFailObtaining,
        ]

        self.waitForExpectations(expectedHandlers)
        self.waitForExpectations(unexpectedHandlers, invertExpectations: true)

        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: firstPageSize)
    }
}

// MARK: - Offers Hiding

extension OfferSearchResultsDataSourceTests {
    func testOfferHidingViaObservation() {
        let offersCount = Self.pageSize
        let personalizationService = PersonalizationServiceMock(predefinedResult: .succeeded(()))

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: offersCount,
            pageSize: offersCount,
            promotedOffersCount: Self.promotedOffersCount,
            personalizationService: personalizationService
        )

        self.hideOffer(using: dataSource, offersCount: offersCount, byAction: { firstSnippet in
            personalizationService.hideOffer(firstSnippet.identifier, completion: { _ in })
        })
    }

    func testManualOfferHidingByID() {
        let offersCount = Self.pageSize

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: offersCount,
            pageSize: offersCount,
            promotedOffersCount: Self.promotedOffersCount
        )

        self.hideOffer(using: dataSource, offersCount: offersCount, byAction: { firstSnippet in
            dataSource.hideItem(withID: firstSnippet.identifier)
        })
    }

    func testManualOfferHidingByIndex() {
        let offersCount = Self.pageSize

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: offersCount,
            pageSize: offersCount,
            promotedOffersCount: Self.promotedOffersCount
        )

        self.hideOffer(using: dataSource, offersCount: offersCount, byAction: { _ in
            dataSource.hideItem(at: IndexPath(row: 0, section: 0))
        })
    }

    private func hideOffer(
        using dataSource: OfferSearchResultsDataSource,
        offersCount: Int,
        byAction action: (_ firstSnippet: YREOfferSnippet) -> Void
    ) {
        // Load items

        let delegateMockOnLoading = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnLoading
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockOnLoading)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: offersCount)

        // Hide an Offer

        let delegateMockOnHiding = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnHiding

        let expectedHandlers: [XCTestExpectation] = [
            delegateMockOnHiding.onDidUpdateInternalState,
        ]
        let unexpectedHandlers: [XCTestExpectation] = [
            delegateMockOnHiding.onDidStartObtaining,
            delegateMockOnHiding.onDidFinishObtainingWithTaskState,
            delegateMockOnHiding.onDidFinishObtaining,
        ]

        guard let firstOffer = Self.getSnippet(from: dataSource, at: 0) else { return }
        guard let secondOffer = Self.getSnippet(from: dataSource, at: 1) else { return }
        XCTAssertNotEqual(firstOffer.identifier, secondOffer.identifier)

        action(firstOffer)

        self.waitForExpectations(expectedHandlers)
        self.waitForExpectations(unexpectedHandlers, invertExpectations: true)

        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: offersCount - 1)

        guard let newFirstOffer = Self.getSnippet(from: dataSource, at: 0) else { return }
        XCTAssertEqual(newFirstOffer.identifier, secondOffer.identifier)
    }

    private static func getSnippet(
        from dataSource: OfferSearchResultsDataSource,
        at index: Int
    ) -> YREOfferSnippet? {
        guard let offer = dataSource.item(at: IndexPath(row: index, section: 0)) as? YREOfferSnippet else {
            XCTFail("Item at index \(index) is missing or has wrong type")
            return nil
        }
        return offer
    }
}

// MARK: - Advertising

extension OfferSearchResultsDataSourceTests { }

// MARK: - Special Cases

extension OfferSearchResultsDataSourceTests {
    func testConciergePosition() {
        let offersCount = 4
        let conciergePosition = 3
        let totalItems = offersCount + 1

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: offersCount,
            pageSize: Self.pageSize,
            promotedOffersCount: Self.promotedOffersCount,
            conciergePosition: conciergePosition
        )

        // Load items

        let delegateMockOnLoading = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnLoading
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockOnLoading)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: totalItems)

        // Ensure we have the Concierge item at correct position
        
        for i in 0 ..< totalItems {
            guard let item = dataSource.item(at: IndexPath(row: i, section: 0)) else {
                XCTFail("Item at index \(i) is missing")
                break
            }

            if i == conciergePosition {
                XCTAssertTrue(item is ConciergeIncutItem)
            }
            else {
                XCTAssertTrue(item is YREOfferSnippet)
            }
        }
    }

    func testConciergeDisplatingAfterNonSingleOfferHiding() {
        let offersCount = 2
        let totalItems = offersCount + 1

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: offersCount,
            pageSize: Self.pageSize,
            promotedOffersCount: Self.promotedOffersCount,
            conciergePosition: 2
        )

        // Load items

        let delegateMockOnLoading = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnLoading
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockOnLoading)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: totalItems)

        // Hide an Offer

        let delegateMockOnHiding = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnHiding

        let expectedHandlers: [XCTestExpectation] = [
            delegateMockOnHiding.onDidUpdateInternalState,
        ]

        guard let singleOffer = Self.getSnippet(from: dataSource, at: 0) else { return }
        dataSource.hideItem(withID: singleOffer.identifier)

        self.waitForExpectations(expectedHandlers)

        // Concierge item is visible - there's still one Offer in the List
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: totalItems - 1)
    }

    func testConciergeDismissalAfterSingleOfferHiding() {
        let offersCount = 1

        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: offersCount,
            pageSize: Self.pageSize,
            promotedOffersCount: Self.promotedOffersCount,
            conciergePosition: offersCount + 1
        )

        // Load items

        let delegateMockOnLoading = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnLoading
        dataSource.obtainDataChunk(withOptions: nil)

        self.waitForCommonCallbacks(using: delegateMockOnLoading)
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: offersCount + 1)

        // Hide an Offer

        let delegateMockOnHiding = OfferSearchResultsDataSourceDelegateMock()
        dataSource.delegate = delegateMockOnHiding

        let expectedHandlers: [XCTestExpectation] = [
            delegateMockOnHiding.onDidUpdateInternalState,
        ]

        guard let singleOffer = Self.getSnippet(from: dataSource, at: 0) else { return }
        dataSource.hideItem(withID: singleOffer.identifier)

        self.waitForExpectations(expectedHandlers)

        // Hide the Concierge item if there're no more Offers in the List
        Self.ensureDataSourceInIdleState(dataSource, haveItemsCount: 0)
    }
}

// MARK: - SearchResultsReporter

extension OfferSearchResultsDataSourceTests {
    func testReportingSearchResults() {
        // given
        let mockSearchResultsReporter = SearchResultsReporterMock()
        let dataSource = Self.makeOfferSnippetDataSource(
            offersCount: 0,
            pageSize: Self.pageSize,
            promotedOffersCount: 0,
            searchResultsReporter: mockSearchResultsReporter
        )

        // when
        dataSource.obtainDataChunk(withOptions: nil)

        // then
        let expectedHandlers: [XCTestExpectation] = [
            mockSearchResultsReporter.report,
        ]
        self.wait(for: expectedHandlers, timeout: Self.timeout)
    }
}

// MARK: - Private Data Source Factory

extension OfferSearchResultsDataSourceTests {
    private static func makeOfferSnippetDataSource(
        offersCount: Int,
        pageSize: Int,
        promotedOffersCount: Int,
        personalizationService: PersonalizationServiceMock? = nil,
        conciergePosition: Int? = nil,
        authStateObservable: AuthStateObservable? = nil,
        searchResultsReporter: SearchResultsReporterProtocol = SearchResultsMockReporter()
    ) -> OfferSearchResultsDataSource {
        let generator = OfferSnippetSearchResultsGenerator(
            itemsCount: offersCount
        )
        let searchServiceMock = AnyOffersListSearchServiceMock(
            itemsGenerator: generator,
            pageSize: pageSize,
            promotedOffersCount: promotedOffersCount
        )
        let dataSource = Self.makeOfferSnippetSimpleDataSource(
            searchService: searchServiceMock,
            personalizationStateService: personalizationService,
            conciergePosition: conciergePosition,
            authStateObservable: authStateObservable,
            searchResultsReporter: searchResultsReporter
        )
        return dataSource
    }

    private static func makeOfferSnippetDataSource(
        specificError: Error?,
        searchResultsReporter: SearchResultsReporterProtocol = SearchResultsMockReporter()
    ) -> OfferSearchResultsDataSource {
        let searchServiceMock = AnyOffersListSearchServiceMock(
            error: specificError
        )
        let dataSource = Self.makeOfferSnippetSimpleDataSource(
            searchService: searchServiceMock,
            searchResultsReporter: searchResultsReporter
        )
        return dataSource
    }

    private static func makeOfferSnippetSimpleDataSource(
        searchService: AnyOffersListSearchService,
        personalizationStateService: PersonalizationStateObservationService? = nil,
        conciergePosition: Int? = nil,
        authStateObservable: AuthStateObservable? = nil,
        searchResultsReporter: SearchResultsReporterProtocol
    ) -> OfferSearchResultsDataSource {
        let adReporter = SearchResultsWithAdListDataSourceMockReporter()
        let analyticsReporters: OfferSearchResultsDataSource.AnalyticsReporters = .init(
            adReporter: adReporter,
            searchResultsReporter: searchResultsReporter
        )
        let adItemsProvider = AdItemsMockProvider()

        let data = OfferSearchResultsData(
            offersPerAd: 0,
            conciergePosition: conciergePosition
        )
        let searchResultsProvider = AnyOfferSearchResultsProvider(
            parameters: [:],
            searchService: searchService
        )

        let dataSource = OfferSearchResultsDataSource(
            listProvider: searchResultsProvider,
            personalizationStateService: personalizationStateService,
            adItemsProvider: adItemsProvider,
            authStateObservable: authStateObservable ?? AuthStateObservableMock(),
            analyticsReporters: analyticsReporters,
            data: data
        )
        return dataSource
    }
}

// MARK: - Private Testing Helpers

extension OfferSearchResultsDataSourceTests {
    private func waitForCommonCallbacks(
        using delegateMock: OfferSearchResultsDataSourceDelegateMock,
        invertExpectations: Bool = false
    ) {
        let list: [XCTestExpectation] = [
            delegateMock.onDidStartObtaining,
            delegateMock.onDidFinishObtaining,
            delegateMock.onDidFinishObtainingWithTaskState,
            delegateMock.onDidUpdateInternalState
        ]
        self.waitForExpectations(list, invertExpectations: invertExpectations)
    }

    private func waitForExpectations(
        _ list: [XCTestExpectation],
        invertExpectations: Bool = false,
        enforceOrder: Bool = true
    ) {
        if invertExpectations {
            list.forEach { $0.isInverted = true }
        }

        self.wait(
            for: list,
            timeout: Self.timeout,
            enforceOrder: enforceOrder
        )
    }

    private static func ensureDataSourceInIdleState(
        _ dataSource: OfferSearchResultsDataSource,
        haveItemsCount itemsCount: Int
    ) {
        XCTAssertFalse(dataSource.isObtainingData)

        XCTAssertEqual(dataSource.numberOfItems(), UInt(itemsCount))
        XCTAssertEqual(dataSource.numberOfItems(forSection: 0), UInt(itemsCount))
        XCTAssertEqual(dataSource.numberOfSections(), 1)
    }
}
