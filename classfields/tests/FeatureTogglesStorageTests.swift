//
//  FeatureTogglesStorageTests.swift
//  YREFeatures-Unit-Tests
//
//  Created by Aleksey Gotyanov on 11/5/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import RealmSwift
import YRESharedStorage
@testable import YREFeatures
import XCTest

class FeatureTogglesStorageTests: XCTestCase {
    var dbProvider: MockDbProvider!
    var storage: FeatureTogglesStorage!

    override func setUp() {
        super.setUp()
        
        self.dbProvider = MockDbProvider()
        self.storage = FeatureTogglesStorage(provider: self.dbProvider)
        Features.lookup[.test] = AnyFeature(Features.TestFeature())
    }

    func testThatStorageRespectsActiveConfig() {
        let testFeature = TestFeature()
        Features.lookup[.test] = AnyFeature(testFeature)

        self.storage.performUpdate(config: .test) {
            $0.enableFeature(testFeature, withPayload: "test")
        }

        // by default active config – release, so feature should be disabled
        XCTAssertEqual(false, self.storage.isFeatureEnabled(testFeature.id))
        XCTAssertEqual(nil, self.storage.featurePayloadIfEnabled(testFeature))

        self.storage.performUpdate(config: .remote) {
            $0.enableFeature(testFeature, withPayload: "remote")
        }

        XCTAssertEqual(true, self.storage.isFeatureEnabled(testFeature.id))
        XCTAssertEqual("remote", self.storage.featurePayloadIfEnabled(testFeature))

        self.storage.activeConfig = .test

        XCTAssertEqual(true, self.storage.isFeatureEnabled(testFeature.id))
        XCTAssertEqual("test", self.storage.featurePayloadIfEnabled(testFeature))
    }

    func testRemoveFeatureSetting() {
        let testFeature = TestFeature()
        Features.lookup[.test] = AnyFeature(testFeature)

        self.storage.performUpdate(config: .test) {
            $0.enableFeature(testFeature, withPayload: "test")
        }

        XCTAssertTrue(self.dbProvider.realm.objects(FeatureSetting.self).isEmpty == false)

        self.storage.performUpdate(config: .remote) {
            $0.removeSetting(for: testFeature.id)
        }

        XCTAssertTrue(self.dbProvider.realm.objects(FeatureSetting.self).isEmpty == false)

        self.storage.performUpdate(config: .test) {
            $0.removeSetting(for: testFeature.id)
        }

        XCTAssertTrue(self.dbProvider.realm.objects(FeatureSetting.self).isEmpty)
    }

    func testPayloadMigration() {
        let featureV1 = FeatureV1()
        let featureV2 = FeatureV2()

        Features.lookup[.test] = AnyFeature(featureV1)

        self.storage.performUpdate(config: .test) {
            $0.enableFeature(featureV1, withPayload: Payload1(value: "cat"))
        }

        Features.lookup[.test] = AnyFeature(featureV2)

        guard let payload2 = self.storage.featurePayloadIfEnabled(featureV2, config: .test) else {
            XCTFail("payload2 should not be a nil")
            return
        }

        XCTAssertEqual("cat", payload2.value)
    }
}

struct TestFeature: FeatureType {
    var id: FeatureID { .test }

    var defaultPayload: String { "" }
    var payloadVersion: Int { 1 }

    var title: String { "test" }
}

struct FeatureV1: FeatureType {
    var id: FeatureID { .test }

    var defaultPayload: Payload1 { Payload1(value: "") }
    var payloadVersion: Int { 1 }

    var title: String { "test" }
}

struct FeatureV2: FeatureType {
    var id: FeatureID { .test }

    var defaultPayload: Payload2 { Payload2(value: "", newValue: 0) }
    var payloadVersion: Int { 2 }

    var title: String { "test" }

    func decodePayload(from data: Data, payloadVersion: Int) throws -> Payload2 {
        if self.payloadVersion == payloadVersion {
            return try JSONDecoder().decode(Payload2.self, from: data)
        }

        assert(payloadVersion == 1)
        let payload1 = try JSONDecoder().decode(Payload1.self, from: data)
        return Payload2(value: payload1.value, newValue: 42)
    }
}

struct Payload1: Hashable, Codable {
    let value: String
}

struct Payload2: Hashable, Codable {
    let value: String
    let newValue: Int
}
