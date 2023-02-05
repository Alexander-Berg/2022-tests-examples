//
//  PhotoMemoriesSynchronizerTest.swift
//  Taverna-Unit-Tests
//
//  Created by Timur Turaev on 27.02.2022.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
import Combine
@testable import Taverna

internal final class PhotoMemoriesSynchronizerTest: XCTestCase {
    private var loader: Loader!
    private var storage: Storage!
    private var synchronizer: PhotoMemoriesSynchronizer!

    override func setUpWithError() throws {
        try super.setUpWithError()

        self.loader = Loader()
        self.storage = Storage()
        self.synchronizer = PhotoMemoriesSynchronizer(loader: self.loader, storage: self.storage)
    }

    func testNormalUpdating() throws {
        self.storage.lastPhotoMemoriesModel = .exist(model: .init(allPhotosLink: "", galleryTailLink: "", memories: [
            .init(id: "1", link: "", imageUrl: "", title: "", subtitle: "")
        ]))

        let newModel = PhotoMemoriesVariantModel.exist(model: .init(allPhotosLink: "", galleryTailLink: "", memories: [
            .init(id: "2", link: "", imageUrl: "", title: "", subtitle: "")
        ]))
        self.loader.result = .success(newModel)
        XCTAssertEqual(self.storage.usageCounter, 1)
        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 5)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertTrue(self.storage.hasNewMemoriesForBadge)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, newModel)
    }

    func testSecondIdenticalUpdateDoesNotResetFlag() throws {
        self.storage.lastPhotoMemoriesModel = .exist(model: .init(allPhotosLink: "", galleryTailLink: "", memories: [
            .init(id: "1", link: "", imageUrl: "", title: "", subtitle: "")
        ]))

        let newModel = PhotoMemoriesVariantModel.exist(model: .init(allPhotosLink: "", galleryTailLink: "", memories: [
            .init(id: "1", link: "", imageUrl: "", title: "", subtitle: "")
        ]))
        self.storage.hasNewMemoriesForBadge = true
        self.loader.result = .success(newModel)
        XCTAssertEqual(self.storage.usageCounter, 2)
        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 6)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertTrue(self.storage.hasNewMemoriesForBadge)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, newModel)
    }

    func testUpdatingExistedModelByOtherModelBothMaybeEmpty() throws {
        func sync(oldModels: Int, newModels: Int, expectedNewFlag: Bool) throws {
            try self.setUpWithError()

            self.storage.lastPhotoMemoriesModel = .testModel(items: oldModels)
            self.loader.result = .success(.testModel(items: newModels))
            XCTAssertEqual(self.storage.usageCounter, 1)
            try self.makeSynchronizationAndWait()

            XCTAssertEqual(self.storage.usageCounter, 5)
            XCTAssertEqual(self.loader.usageCounter, 1)

            XCTAssertNotNil(self.storage.lastUpdateTime)
            XCTAssertEqual(self.storage.hasNewMemoriesForBadge, expectedNewFlag)
            XCTAssertEqual(self.storage.lastPhotoMemoriesModel, .testModel(items: newModels))
        }

        try sync(oldModels: 0, newModels: 2, expectedNewFlag: true)
        try sync(oldModels: 0, newModels: 0, expectedNewFlag: false)
        try sync(oldModels: 2, newModels: 0, expectedNewFlag: false)
    }

    func testDoNotPerformFrequentLoading() throws {
        self.storage.lastUpdateTime = Date().newDate(diff: -3, granularity: .hour)
        XCTAssertEqual(self.storage.usageCounter, 1)

        try self.makeSynchronizationAndWait()

        // no changes
        XCTAssertEqual(self.storage.usageCounter, 1)
        XCTAssertEqual(self.loader.usageCounter, 0)
    }

    func testResettingNewFlag() throws {
        self.storage.hasNewMemoriesForBadge = true
        self.storage.lastPhotoMemoriesModel = .testModel()
        self.loader.result = .success(.empty)
        XCTAssertEqual(self.storage.usageCounter, 2)
        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 6)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertFalse(self.storage.hasNewMemoriesForBadge)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, .empty)
    }

    func testReplacingEmptyModel() throws {
        self.storage.hasNewMemoriesForBadge = false
        self.storage.lastPhotoMemoriesModel = .empty
        self.loader.result = .success(.testModel())
        XCTAssertEqual(self.storage.usageCounter, 2)
        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 6)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertTrue(self.storage.hasNewMemoriesForBadge)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, .testModel())
    }

    func testReplacingEmptyModelWithOtherEmptyModel() throws {
        self.storage.hasNewMemoriesForBadge = true
        self.storage.lastPhotoMemoriesModel = .empty
        self.loader.result = .success(.testModel(items: 0))
        XCTAssertEqual(self.storage.usageCounter, 2)
        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 6)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertFalse(self.storage.hasNewMemoriesForBadge)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, .testModel(items: 0))
    }

    func testReplacingNilModel() throws {
        self.storage.hasNewMemoriesForBadge = false
        self.loader.result = .success(.testModel())
        XCTAssertEqual(self.storage.usageCounter, 1)
        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 5)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertTrue(self.storage.hasNewMemoriesForBadge)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, .testModel())
    }

    func testUpdatingLastTimeButNotModelWhenLoadingFailed() throws {
        XCTAssertEqual(self.storage.usageCounter, 0)
        self.loader.result = .failure(TestError.someError)

        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.storage.usageCounter, 1)
        XCTAssertEqual(self.loader.usageCounter, 1)

        XCTAssertNotNil(self.storage.lastUpdateTime)
    }

    func testFirstTimeSynchronization() throws {
        XCTAssertEqual(self.storage.usageCounter, 0)

        try self.makeSynchronizationAndWait()

        XCTAssertEqual(self.loader.usageCounter, 1)
        XCTAssertEqual(self.storage.usageCounter, 4)

        XCTAssertNotNil(self.storage.lastUpdateTime)
        XCTAssertEqual(self.storage.lastPhotoMemoriesModel, .empty)
        XCTAssertFalse(self.storage.hasNewMemoriesForBadge)
    }

    private func makeSynchronizationAndWait(file: StaticString = #file, line: UInt = #line) throws {
        let syncronizationFuture = Future<Void, Never> { promise in
            self.synchronizer.performSynchronizationInNeeded {
                promise(.success(()))
            }
        }
        try self.waitFor(syncronizationFuture, timeout: 1, file: file, line: line)
    }
}

private final class Loader: PhotoMemoriesLoading {
    @Atomic var usageCounter: Int = 0
    var result: Result<PhotoMemoriesVariantModel> = .success(.empty)

    func loadPhotoMemories(completion: @escaping (Result<PhotoMemoriesVariantModel>) -> Void) {
        self.usageCounter += 1
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(20)) { [weak self] in
            guard let self = self else { return }
            completion(self.result)
        }
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

private extension PhotoMemoriesVariantModel {
    static func testModel(items: Int = 2) -> PhotoMemoriesVariantModel {
        let memories: [PhotoMemoryModel] = {
            guard items >= 1 else { return .empty }

            return (1...items).map {
                PhotoMemoryModel(id: "id_\($0)", link: "1", imageUrl: "2", title: "3", subtitle: "4")
            }
        }()

        return .exist(model: .init(allPhotosLink: "1", galleryTailLink: "2", memories: memories))
    }
}
