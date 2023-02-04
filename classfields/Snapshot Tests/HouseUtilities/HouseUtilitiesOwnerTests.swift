//
//  HouseUtilitiesOwnerTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 15.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREComponents
import YREModel

@testable import YREHouseUtilitiesMetersModule

final class HouseUtilitiesOwnerTests: XCTestCase {
    func testNoMeterReadingsPopup() {
        let presenter = NoMeterReadingsPopupPresenter()
        self.assertSnapshot(presenter.viewController.view)
    }

    func testMeterReadingsCellWithStatuses() {
        for status in Self.meterReadingsStatuses {
            let meterReadings = HouseUtilitiesModelsGenerator.meterReadings(status: status)
            let viewModel = Self.meterReadingsCellViewModel(meterReadings: meterReadings)
            self.testCell(with: viewModel, function: #function + "_\(status)")
        }
    }

    func testMeterReadingsDetails() {
        let presenter = MeterReadingsDetailsPresenter(
            userRole: .owner,
            meterReadingsForPeriod: .init(
                periodDate: .init(month: 3, year: 2021),
                meterReadings: HouseUtilitiesModelsGenerator.meterReadings(status: .sent, tariff: .double)
            ),
            viewController: .init(),
            analyticsReporter: HouseUtilitiesMetersAnalyticsReporter(analyticsReporter: AnalyticsReporterMock())
        )
        self.assertSnapshot(presenter.viewController.view)
    }

    func testMeterReadingsDeclinedPopup() {
        let presenter = MeterReadingsDeclinedPopupPresenter()
        self.assertSnapshot(presenter.viewController.view)
    }

    private static let meterReadingsStatuses: [HouseUtilitiesMeterReadings.Status] = [
        .notSent,
        .sending,
        .shouldBeSent,
        .sent,
        .declined(reasonForDecline: "Some reason"),
        .expired,
    ]

    private static func meterReadingsCellViewModel(
        meterReadings: HouseUtilitiesMeterReadings,
        periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 3, year: 2021)
    ) -> MeterReadingsCell.ViewModel {
        return MeterReadingsCellViewModelGenerator.viewModel(
            for: meterReadings,
            periodDate: periodDate,
            userRole: .owner
        )
    }

    private func testCell(with viewModel: MeterReadingsCell.ViewModel, function: String = #function) {
        let cell = MeterReadingsCell()
        cell.frame = Self.frame(by: { _ in MeterReadingsCell.height })
        cell.configure(with: viewModel)
        self.assertSnapshot(cell, function: function)
    }
}
