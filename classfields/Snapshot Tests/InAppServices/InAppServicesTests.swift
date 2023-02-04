//
//  InAppServicesTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 27.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREModel

import class YRECoreUtils.YREError
import class YREComponents.ErrorTextProvider
import YREServiceLayer

@testable import YREInAppServicesModule

final class InAppServicesTests: XCTestCase {
    func testImageRentBannerPromo() {
        let view = Self.rentBannerView(with: .promo, type: .image)
        self.assertSnapshot(view)
    }

    func testImageRentBannerAccount() {
        let view = Self.rentBannerView(with: .rentService, type: .image)
        self.assertSnapshot(view)
    }

    func testTextRentBannerPromo() {
        let view = Self.rentBannerView(with: .promo, type: .text)
        self.assertSnapshot(view)
    }

    func testTextRentBannerAccount() {
        let view = Self.rentBannerView(with: .rentService, type: .text)
        self.assertSnapshot(view)
    }

    // MARK: Common

    func testErrorLocal() {
        let error: Error = YREError.noInternet(underlying: nil)
        let errorViewModel = ErrorTextProvider.makeRentCommonViewModel(with: error)
        let configuration = InAppServiceNotificationViewModelGenerator.makeConfiguration(for: errorViewModel)
        let view = Self.notificationView(with: configuration)
        self.assertSnapshot(view)
    }

    func testErrorServer() {
        let errorViewModel = ErrorTextProvider.makeRentCommonViewModel(with: nil)
        let configuration = InAppServiceNotificationViewModelGenerator.makeConfiguration(for: errorViewModel)
        let view = Self.notificationView(with: configuration)
        self.assertSnapshot(view)
    }

    func testBannedInRealty() {
        let configuration = InAppServiceNotificationViewModelGenerator.makeBannedUserConfiguration()
        let view = Self.notificationView(with: configuration)
        self.assertSnapshot(view)
    }

    // MARK: - Region

    func testRegionView() {
        let view = Self.regionView(with: .init(title: "Санкт-Петербург и ЛО"))
        self.assertSnapshot(view)
    }

    func testRegionViewLongTitle() {
        let view = Self.regionView(with: .init(title: "Очень очень очень сильно большое название региона"))
        self.assertSnapshot(view)
    }
}

extension InAppServicesTests {
    private static func notificationView(with configuration: InAppServiceNotificationConfiguration) -> UIView {
        let cell = InAppServiceNotificationCell()
        cell.configure(viewConfiguration: configuration)
        let view: UIView = cell.contentView
        view.frame = Self.frame(
            by: { InAppServiceNotificationCell.size(width: $0, viewConfiguration: configuration).height }
        )
        return view
    }

    private static func rentBannerView(
        with state: InAppServicesRentPromoViewState,
        type: InAppServicesRentPromoViewCell.PromoType
    ) -> UIView {
        let promoDeps = InAppServicesRentPromoDeps(
            searchService: SearchService(webServices: webServicesFactory, serviceFactory: ServiceFactoryMock()),
            analyticsReporter: AnalyticsReporterMock()
        )
        let view: InAppServicesRentPromoView
        let frame: CGRect
        switch type {
            case .image:
                view = InAppServicesImageRentPromoView(promoDeps: promoDeps)
                frame = Self.frame(by: { InAppServicesImageRentPromoView.height(for: $0, state: state) })
            case .text:
                view = InAppServicesTextRentPromoView(promoDeps: promoDeps)
                frame = Self.frame(by: { InAppServicesTextRentPromoView.height(for: $0, state: state) })
        }
        view.configure(with: state, geoIntent: nil)
        view.frame = frame

        return view
    }

    private static func regionView(with model: InAppServicesRegionView.Model) -> UIView {
        let view = InAppServicesRegionView()
        view.model = model
        view.frame = Self.frame(by: { InAppServicesRegionView.size(for: $0, model: model).height })

        return view
    }
}
