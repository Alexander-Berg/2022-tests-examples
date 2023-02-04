//
//  HouseUtilitiesModelsGenerator.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 15.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREModel

final class HouseUtilitiesModelsGenerator {
    static func meterReadings(
        status: HouseUtilitiesMeterReadings.Status = .notSent,
        meterType: HouseUtilitiesMeter.CounterType = .hotWater,
        tariff: HouseUtilitiesMeter.Tariff = .single,
        meterInstalledPlace: String = "SomePlace"
    ) -> HouseUtilitiesMeterReadings {
        .init(
            id: "meterReadingsID",
            status: status,
            meter: Self.meter(type: meterType, tariff: tariff, installedPlace: meterInstalledPlace),
            readings: Self.readings(for: tariff, readingValue: Self.readingValue),
            previousReadings: Self.readings(for: tariff, readingValue: Self.previousReadingValue)
        )
    }

    private static func meter(
        type: HouseUtilitiesMeter.CounterType,
        tariff: HouseUtilitiesMeter.Tariff,
        installedPlace: String
    ) -> HouseUtilitiesMeter {
        .init(
            number: "12345",
            installedPlace: installedPlace,
            type: type,
            tariff: tariff,
            deliverFromDay: 1,
            deliverToDay: 30,
            initialReadings: Self.readings(for: tariff, readingValue: Self.initialReadingValue)
        )
    }

    private static func readings(
        for tariff: HouseUtilitiesMeter.Tariff,
        readingValue: HouseUtilitiesMeterReadingValue
    ) -> [HouseUtilitiesMeterReadingValue] {
        let repeatingCount: Int
        switch tariff {
            case .single:
                repeatingCount = 1
            case .double:
                repeatingCount = 2
            case .triple:
                repeatingCount = 3
        }
        return .init(repeating: readingValue, count: repeatingCount)
    }

    private static let readingValue: HouseUtilitiesMeterReadingValue = .init(
        value: 123.45,
        photo: YaRentImage(namespace: "namespace", groupID: 0, name: "name", urls: [:])
    )

    private static let previousReadingValue: HouseUtilitiesMeterReadingValue = .init(
        value: 100,
        photo: nil
    )

    private static let initialReadingValue: HouseUtilitiesMeterReadingValue = .init(
        value: 0,
        photo: nil
    )
}
