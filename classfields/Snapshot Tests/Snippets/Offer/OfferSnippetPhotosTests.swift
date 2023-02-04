//
//  OfferSnippetPhotosTests.swift
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
    func testNoPhotoInApartment() {
        let viewModel = Self.makeViewModelToCheckPhotos(.apartment)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNoPhotoInNewFlat() {
        let viewModel = Self.makeViewModelToCheckPhotos(.apartment, isNewFlatSale: true)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNoPhotoInRoom() {
        let viewModel = Self.makeViewModelToCheckPhotos(.room)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNoPhotoInHouse() {
        let viewModel = Self.makeViewModelToCheckPhotos(.house)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNoPhotoInLot() {
        let viewModel = Self.makeViewModelToCheckPhotos(.lot)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNoPhotoInGarage() {
        let viewModel = Self.makeViewModelToCheckPhotos(.garage)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNoPhotoInCommercial() {
        let viewModel = Self.makeViewModelToCheckPhotos(.commercial)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    // MARK: Private

    private static func makeViewModelToCheckPhotos(
        _ category: kYREOfferCategory,
        isNewFlatSale: Bool = false
    ) -> OfferSnippetViewModel? {
        let snippet = Self.makeSellOfferSnippet(
            category: category,
            location: Self.makeLocation(subjectFederationID: .moscow),
            isNewFlatSale: isNewFlatSale
        )
        return Self.makeViewModel(with: snippet)
    }
}
