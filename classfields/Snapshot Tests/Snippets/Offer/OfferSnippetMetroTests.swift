//
//  OfferSnippetMetroTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 02.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable force_unwrapping

import XCTest
import YREModel
import YREModelObjc
import YREModelHelpers
import YRESnippets

extension OfferSnippetTests {
    func testMetroIconMoscow() {
        let view = Self.makeViewToCheckMetro(locatedIn: .moscow)
        self.assertSnapshot(view)
    }

    func testMetroIconSaintPetersburg() {
        let view = Self.makeViewToCheckMetro(locatedIn: .saintPetersburg)
        self.assertSnapshot(view)
    }

    func testMetroIconEkaterinburg() {
        let view = Self.makeViewToCheckMetro(locatedIn: .ekaterinburg)
        self.assertSnapshot(view)
    }

    func testMetroIconKazan() {
        let view = Self.makeViewToCheckMetro(locatedIn: .kazan)
        self.assertSnapshot(view)
    }

    func testMetroIconSamara() {
        let view = Self.makeViewToCheckMetro(locatedIn: .samara)
        self.assertSnapshot(view)
    }

    func testMetroIconNovosibirsk() {
        let view = Self.makeViewToCheckMetro(locatedIn: .novosibirsk)
        self.assertSnapshot(view)
    }

    func testMetroIconNizhnyNovgorod() {
        let view = Self.makeViewToCheckMetro(locatedIn: .nizhnyNovgorod)
        self.assertSnapshot(view)
    }

    func testNoMetro() {
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(subjectFederationID: .moscow),
            metro: nil
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel!)

        self.assertSnapshot(view)
    }

    func testMultipleMetroIconsMoscow() {
        let metroLines: [Color] = [
            Color(rgbHexString: "FF3333")!,
            Color(rgbHexString: "33FF33")!,
            Color(rgbHexString: "3333FF")!,
        ]
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(subjectFederationID: .moscow),
            metro: Self.makeMetro(lines: metroLines)
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel!)

        self.assertSnapshot(view)
    }

    // MARK: Private

    private static func makeViewToCheckMetro(
        locatedIn region: SubjectFederationID
    ) -> OfferSnippetView {
        let viewModel = Self.makeViewModelToCheckMetro(locatedIn: region)!
        let view = Self.makeView(with: viewModel)
        return view
    }

    private static func makeViewModelToCheckMetro(
        locatedIn region: SubjectFederationID
    ) -> OfferSnippetViewModel? {
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(subjectFederationID: region),
            metro: Self.makeMetro()
        )
        return Self.makeViewModel(with: snippet)
    }

    private static func makeMetro(
        lines: [Color] = [Color(rgbHexString: "FF3333")!]
    ) -> YREMetro {
        let metro = YREMetro(
            name: "Пятницкое шоссе",
            metroTransport: .foot,
            timeToMetro: 19,
            metroGeoID: 114837,
            coordinate: MDCoords2D(lat: 55.832_382, lon: 37.395_103),
            lineColors: lines
        )
        return metro
    }
}
