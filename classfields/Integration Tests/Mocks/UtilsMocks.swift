//
//  UtilsMocks.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 25.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YRECoreUtils
import YREWeb
import YREFeatures
import YRESettings
import YREModel
import YREModelObjc
import YREServiceLayer

// MARK: - TamperProviderProtocol
final class TamperProviderMock: TamperProviderProtocol {
    func tamperParameterBasedOnGeneratedUUID(_ generatedUUID: UUID?) -> String? {
        nil
    }

    func tamperParameterBasedOnDeviceUUID(_ deviceUUID: String?, urlQueryItems: [URLQueryItem]?) -> String? {
        nil
    }
}

// MARK: - YREHttpAuthorizationProvider
final class HttpAuthorizationProviderMock: YREHttpAuthorizationProvider {
    var token: String? { nil }
    var privateKey: String { "mock" }
    var uuid: String? { nil }

    func refreshToken(_ completion: YREHTTPAuthorizationProviderRefreshTokenCompletionBlock?) {}
}

// MARK: - PaidLoopInfoProvider
final class PaidLoopInfoProviderMock: NSObject, PaidLoopInfoProvider {
    var paidLoopInfo: PaidLoopInfo? { nil }
}

// MARK: - FeatureTogglesReader
final class FeatureTogglesReaderMock: FeatureTogglesReader {
    func objc_isFeatureEnabled(_ feature: Int) -> Bool {
        false
    }

    func objc_featurePayloadIfEnabled(_ feature: Int) -> AnyHashable? {
        nil
    }

    func objc_isFeatureEnabled(_ feature: Int, config: Int) -> Bool {
        false
    }

    func objc_featurePayloadIfEnabled(_ feature: Int, config: Int) -> AnyHashable? {
        nil
    }
}

// MARK: - FallbackRegionProviderProtocol
final class FallbackRegionProviderMock: NSObject, FallbackRegionProviderProtocol {
    var fallbackRegion: YRERegion {
        YRERegion(
            rgid: NSNumber(value: 0),
            plainGeoIntent: .init(
                geoObject: YREGeoObject(
                    name: nil,
                    shortName: nil,
                    address: nil,
                    searchParams: nil,
                    scope: nil,
                    type: .city,
                    center: nil,
                    boundingBox: nil
                )
            )
        )
    }
}

// MARK: - FallbackRegionConfigurationProviderProtocol
final class FallbackRegionConfigurationProviderMock: NSObject, FallbackRegionConfigurationProviderProtocol {
    var fallbackRegionConfiguration: YRERegionConfiguration {
        .init(
            regionID: nil,
            geoID: nil,
            subjectFederationID: nil,
            locativeName: nil,
            availabilityFlags: .init(
                hasCommercialBuildings: .paramBoolTrue,
                hasMetro: .paramBoolTrue,
                hasSites: .paramBoolTrue,
                hasVillages: .paramBoolTrue,
                hasConcierge: .paramBoolTrue,
                hasYandexRent: .paramBoolTrue,
                hasDeveloperLegendaPromo: .paramBoolTrue,
                hasPaidSites: .paramBoolTrue
            ),
            parameterAvailability: nil,
            schoolInfo: .init(
                hasSchoolLayer: false,
                total: nil,
                highRatingColor: nil,
                lowRatingColor: nil
            ),
            heatmapsInfo: [],
            isDirty: false
        )
    }
}

// MARK: - ExpboxesWriter
final class ExpboxesWriterMock: ExpboxesWriter {
    var expboxes: String?
}
