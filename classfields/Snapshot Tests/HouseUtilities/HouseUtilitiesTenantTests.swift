//
//  HouseUtilitiesTenantTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREComponents
import YREModel

@testable import YREHouseUtilitiesMetersModule

final class HouseUtilitiesTenantTests: XCTestCase {
    func testEditReadingsTimeoutPopup() {
        let presenter = EditReadingsTimeoutPopupPresenter()
        self.assertSnapshot(presenter.viewController.view)
    }

    func testNotTodayPopup() {
        let analyticsReporter = HouseUtilitiesMetersAnalyticsReporter(analyticsReporter: AnalyticsReporterMock())
        let presenter = NotTodayPopupPresenter(
            analyticsReporter: analyticsReporter,
            meterReadings: HouseUtilitiesModelsGenerator.meterReadings()
        )
        self.assertSnapshot(presenter.viewController.view)
    }

    func testMeterReadingsCellWithStatuses() {
        for status in Self.meterReadingsStatuses {
            let meterReadings = HouseUtilitiesModelsGenerator.meterReadings(status: status)
            let viewModel = Self.meterReadingsCellViewModel(meterReadings: meterReadings)
            self.testCell(with: viewModel, function: #function + "_\(status)")
        }
    }

    func testMeterReadingsCellWithTypes() {
        for meterType in Self.meterTypes {
            let meterReadings = HouseUtilitiesModelsGenerator.meterReadings(meterType: meterType)
            let viewModel = Self.meterReadingsCellViewModel(meterReadings: meterReadings)
            self.testCell(with: viewModel, function: #function + "_\(meterType)")
        }
    }

    func testMeterReadingsCellWithTariff() {
        for tariff in Self.meterTariffs {
            let meterReadings = HouseUtilitiesModelsGenerator.meterReadings(status: .sent, tariff: tariff)
            let viewModel = Self.meterReadingsCellViewModel(meterReadings: meterReadings)
            self.testCell(with: viewModel, function: #function + "_\(tariff)")
        }
    }

    func testMeterReadingsCellWithLongInstalledPlace() {
        let meterReadings = HouseUtilitiesModelsGenerator
            .meterReadings(meterInstalledPlace: "длинное название того места, где установлен счётчик")
        let viewModel = Self.meterReadingsCellViewModel(meterReadings: meterReadings)
        self.testCell(with: viewModel)
    }

    func testMeterReadingsContainer() {
        let viewController = MeterReadingsContainerViewController()
        let presenter = MeterReadingsContainerPresenter(
            meterReadings: HouseUtilitiesModelsGenerator.meterReadings(),
            viewController: viewController
        )
        self.assertSnapshot(presenter.viewController.view)
    }

    func testMeterReadingsDetails() {
        let presenter = MeterReadingsDetailsPresenter(
            userRole: .tenant,
            meterReadingsForPeriod: .init(
                periodDate: .init(month: 3, year: 2021),
                meterReadings: HouseUtilitiesModelsGenerator.meterReadings(status: .sending, tariff: .double)
            ),
            viewController: .init(),
            analyticsReporter: HouseUtilitiesMetersAnalyticsReporter(analyticsReporter: AnalyticsReporterMock())
        )
        self.assertSnapshot(presenter.viewController.view)
    }

    private func testCell(with viewModel: MeterReadingsCell.ViewModel, function: String = #function) {
        let cell = MeterReadingsCell()
        cell.frame = Self.frame(by: { _ in MeterReadingsCell.height })
        cell.configure(with: viewModel)
        self.assertSnapshot(cell, function: function)
    }

    private static let meterReadingsStatuses: [HouseUtilitiesMeterReadings.Status] = [
        .notSent,
        .sending,
        .shouldBeSent,
        .sent,
        .declined(reasonForDecline: "Some reason"),
        .expired,
    ]

    private static let meterTypes: [HouseUtilitiesMeter.CounterType] = [
        .hotWater,
        .coldWater,
        .gas,
        .power,
        .heating,
    ]

    private static let meterTariffs: [HouseUtilitiesMeter.Tariff] = [
        .single,
        .double,
        .triple,
    ]

    private static func meterReadingsCellViewModel(
        meterReadings: HouseUtilitiesMeterReadings,
        periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 3, year: 2021)
    ) -> MeterReadingsCell.ViewModel {
        return MeterReadingsCellViewModelGenerator.viewModel(
            for: meterReadings,
            periodDate: periodDate,
            userRole: .tenant
        )
    }
}
