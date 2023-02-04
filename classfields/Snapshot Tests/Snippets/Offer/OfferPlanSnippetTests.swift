//
//  OfferPlanSnippetTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 10.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc
@testable import YRESiteCardModule

final class OfferPlanSnippetTests: XCTestCase {
    func testWhenOneOfferByPlan() {
        let priceInfo = Self.makePriceInfo(
            from: Self.makePrice(7_272_460)
        )
        let content = Self.makeModel(
            priceInfo: priceInfo
        )
        let view = Self.makeView(with: content)
        self.assertSnapshot(view)
    }

    func testWhenManyOffersByPlan() {
        let priceInfo = Self.makePriceInfo(
            from: Self.makePrice(7_272_460)
        )
        let content = Self.makeModel(
            priceInfo: priceInfo,
            apartmentsCount: 2
        )
        let view = Self.makeView(with: content)
        self.assertSnapshot(view)
    }

    func testWhenOverflowingOffersByPlan() {
        let priceInfo = Self.makePriceInfo(
            from: Self.makePrice(7_272_460_000),
            to: Self.makePrice(9_223_372_036_854)
        )
        let content = Self.makeModel(
            priceInfo: priceInfo,
            apartmentsCount: 12,
            floors: [1, 3, 5, 7, 9, 12, 15, 17, 19, 23, 31, 43]
        )
        let view = Self.makeView(with: content)
        self.assertSnapshot(view)
    }

    func testWithVirtualToursOffersByPlan() {
        let priceInfo = Self.makePriceInfo(
            from: Self.makePrice(7_272_460)
        )
        let content = Self.makeModel(
            priceInfo: priceInfo,
            hasVirtualTours: true
        )
        let view = Self.makeView(with: content)
        self.assertSnapshot(view)
    }
}

// MARK: - View Utils

extension OfferPlanSnippetTests {
    private static func makeView(
        with snippet: OfferPlanSnippet
    ) -> OfferPlanSnippetView {
        let viewModel = OfferPlanSnippetViewModelGenerator.make(
            offerPlan: snippet,
            onTap: {}
        )
        let view = OfferPlanSnippetView()
        view.viewModel = viewModel
        view.frame = Self.makeFrame(for: view)
        return view
    }

    private static func makeFrame(for view: OfferPlanSnippetView) -> CGRect {
        let width = UIScreen.main.bounds.width
        let height = OfferPlanSnippetView.staticHeight(
            width: width,
            layout: view.layout,
            layoutStyle: view.layoutStyle
        )
        let result = CGRect(
            origin: .zero,
            size: CGSize(width: width, height: height)
        )
        return result
    }
}

// MARK: - Model Utils

extension OfferPlanSnippetTests {
    private static func makePrice(_ value: Int) -> YREPrice {
        return YREPrice(
            currency: .RUB,
            value: NSNumber(value: value),
            unit: .perOffer,
            period: .wholeLife
        )
    }

    private static func makePriceInfo(
        from: YREPrice,
        to: YREPrice? = nil
    ) -> SitePriceInfo {
        let priceRangePerOffer = YREPriceRange(
            from: from,
            to: to ?? from,
            averagePrice: nil
        )
        return SitePriceInfo(
            priceRangePerOffer: priceRangePerOffer,
            priceRangePerMeter: nil
        )
    }

    private static func makeModel(
        priceInfo: SitePriceInfo,
        apartmentsCount: UInt = 1,
        floors: [UInt] = [2],
        hasVirtualTours: Bool = false
    ) -> OfferPlanSnippet {
        let content = OfferPlanContent(
            clusterID: "1111",
            roomsType: SiteRoomsType.rooms(1),
            wholeArea: nil,
            kitchenArea: nil,
            livingArea: nil,
            renderablePlanView: []
        )
        let statistic = SiteStatisticItem(
            apartmentsCount: apartmentsCount,
            offersCount: 1,
            flatPlansCount: nil,
            areaRange: nil,
            priceInfo: priceInfo,
            houseId: nil,
            floor: floors,
            commissioningDate: [],
            hasVirtualTours: hasVirtualTours
        )
        return OfferPlanSnippet(offerID: "1234", content: content, statistic: statistic)
    }
}
