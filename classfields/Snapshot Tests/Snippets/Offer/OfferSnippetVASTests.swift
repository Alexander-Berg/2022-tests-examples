//
//  OfferSnippetVASTests.swift
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
    func testAllVAS() {
        let viewModel = Self.makeViewModelToCheckVAS([.premium, .promotion, .raising])
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testOneVAS() {
        let viewModel = Self.makeViewModelToCheckVAS([.premium])
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testTwoVAS() {
        let viewModel = Self.makeViewModelToCheckVAS([.raising, .promotion])
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testHiddenVAS() {
        let viewModel = Self.makeViewModelToCheckVAS([.premium, .promotion, .raising], shouldHideVASIcons: true)
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    // MARK: Private

    private enum VASKind {
        case premium
        case promotion
        case raising
    }

    private static func makeViewModelToCheckVAS(
        _ vasSet: Set<VASKind>,
        shouldHideVASIcons: Bool = false
    ) -> OfferSnippetViewModel? {
        let offerVAS = OfferVAS(
            isPremium: vasSet.contains(.premium),
            isRaised: vasSet.contains(.raising),
            isPromoted: vasSet.contains(.promotion)
        )
        let snippet = Self.makeSellOfferSnippet(
            location: Self.makeLocation(subjectFederationID: .moscow),
            vas: offerVAS
        )
        let provider = YREAbstractOfferInfoProvider(
            offer: snippet,
            inFavorites: false,
            inCallHistory: false,
            isViewed: false,
            requestingPhones: false
        )
        guard let snippetProvider = provider.asOfferSnippetProvider() else {
            XCTFail("Unable to obtain offer snippet provider")
            return nil
        }
        let viewModel = OfferSnippetViewModelGenerator.makeViewModel(
            viewMode: .list,
            offerSnippetInfoProvider: snippetProvider,
            restrictedActions: [],
            selectedImageIndex: 0,
            enforceWholePrice: false,
            hideVASIcons: shouldHideVASIcons
        )
        return viewModel
    }
}
