//
//  RegionConfigurationCodingAgentTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 30.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc
import YREFoundationUtils

class RegionConfigurationCodingAgentTests: XCTestCase {
    private lazy var originalConfiguration: YRERegionConfiguration = {
        let regionID: String = "123"
        let geoID: NSNumber = 321
        let subjectFederationID: NSNumber = 1
        let locativeName = "в Москве и МО"
        let availabilityFlags = RegionConfigurationAvailabilityFlags(
            hasCommercialBuildings: .paramBoolTrue,
            hasMetro: .paramBoolTrue,
            hasSites: .paramBoolTrue,
            hasVillages: .paramBoolTrue,
            hasConcierge: .paramBoolTrue,
            hasYandexRent: .paramBoolUnknown,
            hasDeveloperLegendaPromo: .paramBoolUnknown,
            hasPaidSites: .paramBoolTrue
        )
        let parameterAvailability = [YREParametersConfiguration]()
        let schoolInfo = YRESchoolInfo(hasSchoolLayer: true,
                                       total: 12,
                                       highRatingColor: "ff0000", // UIColor.red
                                       lowRatingColor: "0000ff")  // UIColor.blue
        let heatmapsInfo = DefaultHeatmapsInfoProvider.fallbackHeatmapsInfo()

        let configuration = YRERegionConfiguration(
            regionID: regionID,
            geoID: geoID,
            subjectFederationID: subjectFederationID,
            locativeName: locativeName,
            availabilityFlags: availabilityFlags,
            parameterAvailability: parameterAvailability,
            schoolInfo: schoolInfo,
            heatmapsInfo: heatmapsInfo,
            isDirty: false
        )
        return configuration
    }()
    
    func testExplicitEncodeDecode() {
        let encoder = NSKeyedArchiver(requiringSecureCoding: false)

        RegionConfigurationCodingAgent.encode(regionConfiguration: self.originalConfiguration, withCoder: encoder)

        guard
            let decoder = DeprecatedNSKeyedUnarchiver.unarchiverForReading(with: encoder.encodedData),
            let decodedConfiguration = RegionConfigurationCodingAgent.decode(withDecoder: decoder)
        else {
            XCTFail("Couldn't decode RegionConfiguration")
            return
        }

        XCTAssertEqual(self.originalConfiguration, decodedConfiguration)
    }
    
    func testImplicitEncodeDecode() {
        do {
            let encodedData = try NSKeyedArchiver.archivedData(withRootObject: self.originalConfiguration, requiringSecureCoding: false)

            guard
                let decodedConfiguration = DeprecatedNSKeyedUnarchiver.unarchiveObject(with: encodedData) as? YRERegionConfiguration
            else {
                XCTFail("Couldn't decode RegionConfiguration")
                return
            }

            XCTAssertEqual(originalConfiguration, decodedConfiguration)
        }
        catch {
            XCTFail("Failed with error \(error)")
        }
    }
    
    // Fallback for regions with legacy heatmaps availability info -
    // - we provide HeatmapsInfo in new format according to saved legacy values and mark configuration as dirty
    func testUnspecifiedHeatmapsInfoWithLegacyHeatmapsAvailability() {
        let encoder = NSKeyedArchiver(requiringSecureCoding: false)

        let legacyInfo = YREPlainHeatmapAvailabilityInfo(hasTransportLayer: true,
                                                         hasInfrastructureLayer: true,
                                                         hasEcologyLayer: false)

        // Encode manually
        encoder.encode(self.originalConfiguration.parameterAvailability,
                       forKey: RegionConfigurationCodingAgent.parameterAvailabilityKey)
        encoder.encode(self.originalConfiguration.schoolInfo,
                       forKey: RegionConfigurationCodingAgent.schoolInfoKey)
        encoder.encode(nil,
                       forKey: RegionConfigurationCodingAgent.heatmapsInfoKey)
        encoder.encode(false,
                       forKey: RegionConfigurationCodingAgent.isDirtyKey)
        encoder.encode(legacyInfo,
                       forKey: RegionConfigurationCodingAgent.legacyHeatmapAvailabilityKey)


        guard
            let decoder = DeprecatedNSKeyedUnarchiver.unarchiverForReading(with: encoder.encodedData),
            let decodedConfiguration = RegionConfigurationCodingAgent.decode(withDecoder: decoder)
        else {
            XCTFail("Couldn't decode RegionConfiguration")
            return
        }

        let fallbackHeatmapsInfo = DefaultHeatmapsInfoProvider.fallbackHeatmapsInfo(withLegacyInfo: legacyInfo)

        XCTAssertEqual(decodedConfiguration.heatmapsInfo, fallbackHeatmapsInfo)
        XCTAssertTrue(decodedConfiguration.isDirty)
    }
    
    // Fallback^2 for regions without legacy heatmaps availability info (almost impossible case) -
    // - we provide empty HeatmapsInfo and mark configuration as dirty
    func testUnspecifiedHeatmapsInfoWithoutLegacyHeatmapsAvailability() {
        let encoder = NSKeyedArchiver(requiringSecureCoding: false)

        // Encode manually
        encoder.encode(self.originalConfiguration.parameterAvailability,
                       forKey: RegionConfigurationCodingAgent.parameterAvailabilityKey)
        encoder.encode(self.originalConfiguration.schoolInfo,
                       forKey: RegionConfigurationCodingAgent.schoolInfoKey)
        encoder.encode(nil,
                       forKey: RegionConfigurationCodingAgent.heatmapsInfoKey)
        encoder.encode(false,
                       forKey: RegionConfigurationCodingAgent.isDirtyKey)

        guard
            let decoder = DeprecatedNSKeyedUnarchiver.unarchiverForReading(with: encoder.encodedData),
            let decodedConfiguration = RegionConfigurationCodingAgent.decode(withDecoder: decoder)
        else {
            XCTFail("Couldn't decode RegionConfiguration")
            return
        }

        XCTAssertTrue(decodedConfiguration.heatmapsInfo.isEmpty)
        XCTAssertTrue(decodedConfiguration.isDirty)
    }
}
