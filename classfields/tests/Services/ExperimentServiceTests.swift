//
//  ExperimentServiceTests.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Aleksey Gotyanov on 11/6/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import YREServiceLayer
import YREFeatures
import struct YREModel.ExperimentSet
import struct YREModel.ExperimentBox
import protocol YREAppState.YREExperimentsWriter

class ExperimentServiceTests: XCTestCase {
    private var writer: FeatureTogglesWriterMock!
    private var provider: ExperimentProviderMock!
    private var expboxesWriter: ExpboxesMockWriter!
    private var parser = ExperimentBoxMockParser()
    private var service: ExperimentService!
    private var experimentsWriter: ExperimentsWriterMock!

    override func setUp() {
        super.setUp()

        self.writer = FeatureTogglesWriterMock()
        self.provider = ExperimentProviderMock()
        self.expboxesWriter = ExpboxesMockWriter()
        self.experimentsWriter = ExperimentsWriterMock()

        let reader = AuthStateReaderMock()
        let authStateObservable = AuthStateObservableAgent(authStateReader: reader)

        self.service = ExperimentService(
            writer: self.writer,
            experimentBoxProvider: self.provider,
            expboxesWriter: self.expboxesWriter,
            parser: self.parser,
            experimentsReader: experimentsWriter,
            authStateObservable: authStateObservable
        )
    }

    func testSingleBox() {
        let experimentSet = ExperimentSet(
            boxes: [
                ExperimentBox(id: 1, parameters: ["flags": ["testA:1"]], triple: "1,0,0")
            ],
            configVersion: "1"
        )

        let featureA = TestFeatureA()

        self.performTest(with: experimentSet) { _ in
            XCTAssertEqual("1", self.writer.featurePayloadIfEnabled(featureA))
        }
    }

    func testMultipleBoxes() {
        let experimentSet = ExperimentSet(
            boxes: [
                ExperimentBox(id: 1, parameters: ["flags": ["testA:1"]], triple: "1,0,0"),
                ExperimentBox(id: 2, parameters: ["flags": ["testB:2"]], triple: "2,0,0")
            ],
            configVersion: "1"
        )

        let featureA = TestFeatureA()
        let featureB = TestFeatureB()

        self.performTest(with: experimentSet) { _ in
            XCTAssertEqual("1", self.writer.featurePayloadIfEnabled(featureA))
            XCTAssertEqual("2", self.writer.featurePayloadIfEnabled(featureB))
        }
    }

    func testSingleBoxWithMultipleFeatures() {
        let experimentSet = ExperimentSet(
            boxes: [
                ExperimentBox(id: 1, parameters: ["flags": ["testA:1", "testB:2"]], triple: "1,0,0")
            ],
            configVersion: "1"
        )

        let featureA = TestFeatureA()
        let featureB = TestFeatureB()

        self.performTest(with: experimentSet) { _ in
            XCTAssertEqual("1", self.writer.featurePayloadIfEnabled(featureA))
            XCTAssertEqual("2", self.writer.featurePayloadIfEnabled(featureB))
        }
    }

    func testThatNotPresentedFeaturesAreNotTouched() {
        let experimentSet = ExperimentSet(
            boxes: [
                ExperimentBox(id: 1, parameters: ["flags": ["testA:1"]], triple: "1,0,0")
            ],
            configVersion: "1"
        )

        let featureA = TestFeatureA()
        let featureB = TestFeatureB()

        self.writer.performUpdate(config: .remote) {
            $0.enableFeature(featureB, withPayload: "value")
        }

        self.performTest(with: experimentSet) { _ in
            XCTAssertTrue(self.writer.isFeatureEnabled(featureA.id))
            XCTAssertTrue(self.writer.isFeatureEnabled(featureB.id))
        }
    }

    func testThatSameFeatureWithDifferentValuesFromBoxesIsIgnored() {
        let experimentSet = ExperimentSet(
            boxes: [
                ExperimentBox(id: 1, parameters: ["flags": ["testA:1"]], triple: "1,0,0"),
                ExperimentBox(id: 2, parameters: ["flags": ["testA:2"]], triple: "2,0,0")
            ],
            configVersion: "1"
        )

        self.performTest(with: experimentSet) { storage in
            XCTAssertTrue(storage.isEmpty)
        }
    }

    func testThatFeatureThatEnabledInOneBoxAndDisabledInAnotherOneIsIgnored() {
        let experimentSet = ExperimentSet(
            boxes: [
                ExperimentBox(id: 1, parameters: ["flags": ["testA:1"]], triple: "1,0,0"),
                ExperimentBox(id: 2, parameters: ["flags": ["testA:disabled"]], triple: "2,0,0")
            ],
            configVersion: "1"
        )

        self.performTest(with: experimentSet) { storage in
            XCTAssertTrue(storage.isEmpty)
        }
    }

    private func performTest(
        with experiments: ExperimentSet,
        validateSavedSettings: @escaping ([FeatureTogglesWriterMock.Key: FeatureTogglesWriterMock.Setting]) -> Void
    ) {
        self.provider.result = .success(experiments)

        let expectation = XCTestExpectation()

        writer.performUpdateCompleted = {
            expectation.fulfill()
        }

        self.service.synchronizeIfNeeded()

        wait(for: [expectation], timeout: 10)

        validateSavedSettings(self.writer.storage)
    }
}

private final class FeatureTogglesWriterMock: FeatureTogglesWriter {
    var storage: [Key: Setting] = [:]

    var activeConfig: FeatureConfig = .remote

    var performUpdateCompleted: (() -> Void)?

    func performUpdate(config: FeatureConfig, update: (FeatureTogglesWriterContext) -> Void) {
        let context = Context(storage: self.storage, config: config.rawValue)
        update(context)
        self.storage = context.storage
        self.performUpdateCompleted?()
    }

    func objc_isFeatureEnabled(_ feature: Int) -> Bool {
        self.objc_isFeatureEnabled(feature, config: self.activeConfig.rawValue)
    }

    func objc_featurePayloadIfEnabled(_ feature: Int) -> AnyHashable? {
        self.objc_featurePayloadIfEnabled(feature, config: self.activeConfig.rawValue)
    }

    func objc_isFeatureEnabled(_ feature: Int, config: Int) -> Bool {
        self.objc_featurePayloadIfEnabled(feature, config: config) != nil
    }

    func objc_featurePayloadIfEnabled(_ feature: Int, config: Int) -> AnyHashable? {
        self.storage[Key(feature: feature, config: config)]?.valueIfEnabled
    }

    struct Key: Hashable {
        let feature: Int
        let config: Int
    }

    struct Setting {
        var valueIfEnabled: AnyHashable?
    }

    final class Context: FeatureTogglesWriterContext {
        var storage: [FeatureTogglesWriterMock.Key: Setting]
        var config: Int

        init(storage: [FeatureTogglesWriterMock.Key: Setting], config: Int) {
            self.storage = storage
            self.config = config
        }

        func enableFeature<Feature>(_ feature: Feature, withPayload payload: Feature.Payload) where Feature: FeatureType {
            self.storage[.init(feature: feature.id.rawValue, config: self.config)] = Setting(valueIfEnabled: payload)
        }

        func disableFeature(_ feature: FeatureID) {
            self.storage[.init(feature: feature.rawValue, config: self.config)] = Setting()
        }

        func removeSetting(for feature: FeatureID) {
            self.storage[.init(feature: feature.rawValue, config: self.config)] = nil
        }
    }
}

private final class ExperimentProviderMock: ExperimentProvider {
    var result: Result<ExperimentSet, Error>? = nil

    func getExperimentSet(parameters: [String: Any]?, completion: @escaping (Result<ExperimentSet, Error>) -> Void) {
        guard let result = self.result else { return }

        completion(result)
    }
}

private final class ExperimentBoxMockParser: ExperimentBoxParserType {
    func getFeatures(from box: ExperimentBox) -> ExperimentBoxParseResult {
        guard let flags = box.parameters["flags"] as? [String] else {
            preconditionFailure("check parameters")
        }

        var enabledFeaturePayloads: [FeatureID: AnyHashable] = [:]
        var disabledFeatures: Set<FeatureID> = []

        for flag in flags {
            let nameAndValue = flag.split(separator: ":").map(String.init)
            let isDisabled = nameAndValue[1] == "disabled"
            let featureID: FeatureID
            switch nameAndValue[0] {
                case "testA":
                    featureID = TestFeatureA().id

                case "testB":
                    featureID = TestFeatureB().id

                default:
                    fatalError("unknown")
            }

            if isDisabled {
                disabledFeatures.insert(featureID)
            }
            else {
                enabledFeaturePayloads[featureID] = nameAndValue[1]
            }
        }

        return ExperimentBoxParseResult(
            enabledFeaturePayloads: enabledFeaturePayloads,
            disabledFeatures: disabledFeatures,
            featuresWithUnknownPayloads: []
        )
    }
}

private final class ExpboxesMockWriter: ExpboxesWriter {
    var expboxes: String?
}

private struct TestFeatureA: FeatureType {
    var id: FeatureID { .test }
    var defaultPayload: String { "" }
    var payloadVersion: Int { 1 }
    var title: String { "test A" }
}

private struct TestFeatureB: FeatureType {
    var id: FeatureID { .adsInListing } // can be used any id except .test
    var defaultPayload: String { "" }
    var payloadVersion: Int { 1 }
    var title: String { "test B" }
}

private final class ExperimentsWriterMock: NSObject, YREExperimentsWriter {
    func updateExperimentIDs(_ experimentIDs: [String: NSNumber]?) {
        self.experimentIDs = experimentIDs
    }

    var experimentIDs: [String: NSNumber]?
}
