//
//  OfferCardAuthorTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 08.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc
import YREDesignKit
@testable import YREOfferCardModule

final class OfferCardAuthorTests: XCTestCase {
    func testOfferCardIsOnlineAvailableView() {
        let author = Self.makeAuthor(category: .developer,
                                     name: "ГРУППА КОМПАНИЙ \"МИЦ\"",
                                     organization: "ГРУППА КОМПАНИЙ \"МИЦ\"")
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: nil,
            supportsOnlineView: .paramBoolTrue,
            isTrustedOwner: false,
            shouldForceOwnersTrust: false,
            isOfferOfCurrentUser: true,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardAnonymAuthorView() {
        let author = Self.makeAuthor(category: .owner)
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: "4060534229",
            supportsOnlineView: .paramBoolTrue,
            isTrustedOwner: false,
            shouldForceOwnersTrust: true,
            isOfferOfCurrentUser: false,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardOwnerAuthorView() {
        let author = Self.makeAuthor(category: .owner, name: "OwnerName")
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: "4060534229",
            supportsOnlineView: .paramBoolFalse,
            isTrustedOwner: false,
            shouldForceOwnersTrust: false,
            isOfferOfCurrentUser: false,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardCurrentUserOwnerAuthorView() {
        let author = Self.makeAuthor(category: .owner, name: "OwnerName")
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: "4060534229",
            supportsOnlineView: .paramBoolFalse,
            isTrustedOwner: false,
            shouldForceOwnersTrust: true,
            isOfferOfCurrentUser: true,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardCurrentUserTrustedOwnerAuthorView() {
        let author = Self.makeAuthor(category: .owner, name: "OwnerName")
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: "4060534229",
            supportsOnlineView: .paramBoolTrue,
            isTrustedOwner: true,
            shouldForceOwnersTrust: true,
            isOfferOfCurrentUser: true,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardTrustedOwnerAuthorView() {
        let author = Self.makeAuthor(category: .owner, name: "OwnerName")
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: "4060534229",
            supportsOnlineView: .paramBoolTrue,
            isTrustedOwner: true,
            shouldForceOwnersTrust: true,
            isOfferOfCurrentUser: false,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardYandexRentAuthorView() {
        let author = Self.makeAuthor(category: .unknown)
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: nil,
            supportsOnlineView: .paramBoolTrue,
            isTrustedOwner: false,
            shouldForceOwnersTrust: false,
            isOfferOfCurrentUser: true,
            isYandexRent: true
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    func testOfferCardAgencyAuthorView() {
        let author = Self.makeAuthor(category: .agency, name: "AgencyName", organization: "AgencyOrganization")
        let viewModel = OfferAuthorNodeViewModelGenerator.makeViewModel(
            author: author,
            uid: nil,
            supportsOnlineView: .paramBoolTrue,
            isTrustedOwner: true,
            shouldForceOwnersTrust: false,
            isOfferOfCurrentUser: true,
            isYandexRent: false
        )
        self.assertOfferCardAuthorViewSnapshot(with: viewModel)
    }

    private func assertOfferCardAuthorViewSnapshot(
        with viewModel: OfferAuthorNodeViewModel?,
        function: String = #function
    ) {
        guard let viewModel = viewModel else { XCTFail("viewModel cannot be nil"); return }
        let view = OfferCardAuthorView()
        view.configure(viewModel: viewModel)

        let containerView = UIView()
        containerView.backgroundColor = ColorScheme.Background.primary
        containerView.addSubview(view)
        view.yre_edgesToSuperview()

        containerView.frame = Self.frame { OfferCardAuthorView.height(viewModel: viewModel, width: $0) }

        self.assertSnapshot(containerView, function: function)
    }

    // swiftlint:disable:next force_unwrapping
    private static let someURL: URL = URL(string: "https://realty.yandex.ru")!

    private static func makeAuthor(
        category: AuthorCategory,
        agentName: String? = nil,
        name: String? = nil,
        organization: String? = nil,
        hasPhoto: Bool = false
    ) -> YREAuthor {
        let author = YREAuthor(
            authorID: nil,
            category: category,
            agentName: agentName,
            organization: organization,
            name: name,
            phones: nil,
            authorPhones: nil,
            photoURL: hasPhoto ? self.someURL : nil,
            humanPhotoURL: nil,
            profile: nil,
            communicationChannelType: .onlyPhone
        )
        return author
    }
}
