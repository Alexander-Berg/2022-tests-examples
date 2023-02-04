//
//  YaRentBannerViewTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREComponents

@testable import YREHouseUtilitiesMetersModule

final class YaRentBannerViewTests: XCTestCase {
    private func testView(with viewModel: YaRentBannerView.ViewModel, function: String = #function) {
        let view = YaRentBannerView(viewModel: viewModel, contentInsets: .zero)
        view.frame = Self.frame(by: { view.height(preferredWidth: $0) })
        self.assertSnapshot(view, function: function)
    }
}

// MARK: - HouseUtilitiesMetersBannerViewModelGenerator

extension YaRentBannerViewTests {
    func testMeterReadingsReadyToSend() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingsReadyToSendViewModel()
        self.testView(with: viewModel)
    }

    func testMeterReadingsDeclined() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingsDeclinedViewModel()
        self.testView(with: viewModel)
    }

    func testMeterReadingsSent() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingsSentViewModel()
        self.testView(with: viewModel)
    }

    func testMeterReadingsExpired() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingsExpiredViewModel()
        self.testView(with: viewModel)
    }

    func testMeterReadingsWillBeSent() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingsWillBeSentViewModel()
        self.testView(with: viewModel)
    }

    func testMeterReadingExpired() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingExpiredViewModel()
        self.testView(with: viewModel)
    }

    func testMeterReadingTooEarlyToSend() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Tenant.meterReadingTooEarlyToSendViewModel()
        self.testView(with: viewModel)
    }

    func testOwnerMeterReadingsSent() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Owner.meterReadingsSentViewModel()
        self.testView(with: viewModel)
    }

    func testOwnerMomeMeterReadingsNotSent() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Owner.someMeterReadingsNotSentViewModel()
        self.testView(with: viewModel)
    }

    func testOwnerMeterReadingsDeclined() {
        let viewModel = HouseUtilitiesMetersBannerViewModelGenerator.Owner.meterReadingsDeclinedViewModel()
        self.testView(with: viewModel)
    }
}
