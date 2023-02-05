//
//  PhotoMemoriesManagerTest.swift
//  Taverna-Unit-Tests
//
//  Created by Timur Turaev on 27.02.2022.
//

import XCTest
import NetworkLayer
import TestUtils
@testable import Taverna

internal final class PhotoMemoriesManagerTest: XCTestCase {
    private var manager: PhotoMemoriesManager!
    private var storage: Storage!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName,
                                            token: "~",
                                            networkOperationDelay: 200)
        let httpClient = builder.buildTestClient(kind: kind)

        self.storage = Storage()
        self.manager = PhotoMemoriesManager(httpClientProvider: MockHTTPClientProvider(httpClient: httpClient),
                                            locale: "en",
                                            storageProvider: { _ in self.storage })
    }

    func testSynchronization() throws {
        self.manager.performSynchronization(for: "login")

        self.wait(for: { !self.manager.activeSynchronizers.isEmpty }, timeout: 1)
        XCTAssertEqual(self.manager.activeSynchronizers.count, 1)
        weak var synchronizer = self.manager.activeSynchronizers.values.first
        XCTAssertNotNil(synchronizer)

        self.wait(for: { self.manager.activeSynchronizers.isEmpty }, timeout: 1)
        XCTAssertNil(synchronizer)

        XCTAssertEqual(self.storage.usageCounter, 4)
        XCTAssertTrue(self.storage.hasNewMemoriesForBadge)
        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertNotEqual(self.storage.lastPhotoMemoriesModel, .empty)
    }

    func testPhotoMemoryModelCodable() throws {
        func check(model: PhotoMemoriesVariantModel) throws {
            let encoded = try PropertyListEncoder().encode(model)
            let decoded = try PropertyListDecoder().decode(PhotoMemoriesVariantModel.self, from: encoded)

            XCTAssertEqual(model, decoded)
        }

        try check(model: .empty)
        try check(model: .exist(model: .init(allPhotosLink: "a", galleryTailLink: "b", memories: [
            .init(id: "id", link: "link", imageUrl: "imageURL", title: "title", subtitle: "subtitle")
        ])))
    }
}

private final class MockHTTPClientProvider: NSObject, HTTPClientProviderProtocol {
    private let httpClient: HTTPClientProtocol

    init(httpClient: HTTPClientProtocol) {
        self.httpClient = httpClient
    }

    func httpClient(withLogin login: String) -> HTTPClientProtocol? {
        return self.httpClient
    }
}

private final class Storage: PhotoMemoriesStoring {
    @Atomic var usageCounter: Int = 0

    var lastPhotoMemoriesModel: PhotoMemoriesVariantModel? {
        didSet {
            usageCounter += 1
        }
    }

    var hasNewMemoriesForBadge = false {
        didSet {
            usageCounter += 1
        }
    }

    var hasNewMemoriesForPromo = false {
        didSet {
            usageCounter += 1
        }
    }

    var lastUpdateTime: Date? {
        didSet {
            usageCounter += 1
        }
    }

    static func clearAll() {
        // do nothing
    }
}
