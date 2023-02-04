//
//  OfferSnippetAddressTests.swift
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
    func testLongAddress() {
        let address = "Очень длинный адрес, который точно не должен поместиться в отведенную для него область на сниппете"
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(
                subjectFederationID: .moscow,
                address: address
            )
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel!)

        self.assertSnapshot(view)
    }

    func testSiteDisplayNameInAddress() {
        let building = Self.makeBuilding(
            siteID: "1234",
            siteDisplayName: "ЖК Тест"
        )
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(
                subjectFederationID: .moscow,
                address: nil
            ),
            building: building
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel!)

        self.assertSnapshot(view)
    }

    func testTimeAndTransportInMetro() {
        let metro = YREMetro(
            name: "Метро",
            metroTransport: .transport,
            timeToMetro: 10,
            metroGeoID: .init(value: 0),
            coordinate: nil,
            lineColors: []
        )
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(
                subjectFederationID: .moscow
            ),
            metro: metro
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel!)

        self.assertSnapshot(view)
    }
}
