//
//  OfferSnippetBadgeTests.swift
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
    func testAllBadgesWithMoscowNewFlatSell() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.allInMoscowNewFlat))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNewFlatBadge() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.newFlat))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testTrustedOwnerInSellBadge() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.owner(.trusted)))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testTrustedOwnerInRentBadge() {
        let viewModel = Self.makeViewModelToCheckBadges(.rent(.trustedOwner))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNonTrustedOwnerInMoscowBadge() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.owner(.nonTrustedInMoscow)))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testNonTrustedOwnerInAnyRegionBadge() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.owner(.nonTrustedInAnyRegion)))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testBadgeEGRN() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.egrn))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testYandexRentBadge() {
        let viewModel = Self.makeViewModelToCheckBadges(.rent(.yandexRent))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testBadgeOutdated() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.outdated))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testVirtualTourBadgeSell() {
        let viewModel = Self.makeViewModelToCheckBadges(.sell(.virtualTour))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    func testVirtualTourBadgeRent() {
        let viewModel = Self.makeViewModelToCheckBadges(.rent(.virtualTour))
        let view = Self.makeView(with: viewModel!)
        self.assertSnapshot(view)
    }

    // MARK: Private

    enum Badge {
        enum Sell {
            enum Owner {
                case trusted
                case nonTrustedInMoscow
                case nonTrustedInAnyRegion
            }

            case allInMoscowNewFlat
            case egrn
            case newFlat
            case owner(Owner)
            case outdated
            case virtualTour
        }

        enum Rent {
            case yandexRent
            case trustedOwner
            case virtualTour
        }

        case sell(Sell)
        case rent(Rent)
    }

    private static func makeViewModelToCheckBadges(_ badge: Badge) -> OfferSnippetViewModel? {
        let snippet: YREOfferSnippet
        switch badge {
            case let .sell(kind):
                snippet = Self.makeSellOfferSnippet(
                    location: Self.makeLocation(with: kind),
                    author: Self.makeAuthor(),
                    isNewFlatSale: Self.makeIsNewFlatSale(with: kind),
                    hasEGRNReport: Self.makeHasEGRNReport(with: kind),
                    isFullTrustedOwner: Self.makeIsFullTrustedOwner(with: kind),
                    isOutdated: Self.makeIsOutdated(with: kind),
                    virtualTours: Self.makeVirtualTours(with: kind)
                )
            case let .rent(kind):
                snippet = Self.makeRentOfferSnippet(
                    location: Self.makeLocation(subjectFederationID: .moscow),
                    author: Self.makeAuthor(),
                    isYandexRent: Self.makeIsYandexRent(with: kind),
                    isFullTrustedOwner: Self.makeIsFullTrustedOwner(with: kind),
                    virtualTours: Self.makeVirtualTours(with: kind)
                )
        }
        return Self.makeViewModel(with: snippet)
    }

    private static func makeLocation(with badge: Badge.Sell) -> YRELocation {
        let subjectFederationID: SubjectFederationID
        switch badge {
            case let .owner(owner):
                switch owner {
                    case .trusted, .nonTrustedInMoscow:
                        subjectFederationID = .moscow
                    case .nonTrustedInAnyRegion:
                        subjectFederationID = .saintPetersburg
                }
            case .allInMoscowNewFlat, .egrn, .newFlat, .outdated, .virtualTour:
                subjectFederationID = .moscow
        }
        return Self.makeLocation(subjectFederationID: subjectFederationID)
    }

    private static func makeIsOutdated(with badge: Badge.Sell) -> Bool {
        let result: Bool
        switch badge {
            case .owner, .egrn, .newFlat, .virtualTour:
                result = false
            case .outdated, .allInMoscowNewFlat:
                result = true
        }
        return result
    }

    private static func makeIsNewFlatSale(with badge: Badge.Sell) -> Bool {
        let result: Bool
        switch badge {
            case .egrn, .owner, .outdated, .virtualTour:
                result = false
            case .newFlat, .allInMoscowNewFlat:
                result = true
        }
        return result
    }

    private static func makeHasEGRNReport(with badge: Badge.Sell) -> Bool {
        let result: Bool
        switch badge {
            case .newFlat, .owner, .outdated, .virtualTour:
                result = false
            case .egrn, .allInMoscowNewFlat:
                result = true
        }
        return result
    }

    private static func makeIsFullTrustedOwner(with badge: Badge.Sell) -> Bool {
        let result: Bool
        switch badge {
            case let .owner(kind):
                switch kind {
                    case .trusted:
                        result = true
                    case .nonTrustedInMoscow, .nonTrustedInAnyRegion:
                        result = false
                }

            case .allInMoscowNewFlat:
                result = true
            case .egrn, .newFlat, .outdated, .virtualTour:
                result = false
        }
        return result
    }

    private static func makeVirtualTours(with badge: Badge.Sell) -> [VirtualTour]? {
        let result: [VirtualTour]?
        switch badge {
            case .newFlat, .owner, .outdated, .allInMoscowNewFlat, .egrn:
                result = nil
            case .virtualTour:
                result = Self.makeVirtualTours()
        }
        return result
    }

    private static func makeIsYandexRent(with badge: Badge.Rent) -> Bool {
        let result: Bool
        switch badge {
            case .yandexRent:
                result = true
            case .trustedOwner, .virtualTour:
                result = false
        }
        return result
    }

    private static func makeIsFullTrustedOwner(with badge: Badge.Rent) -> Bool {
        let result: Bool
        switch badge {
            case .yandexRent, .virtualTour:
                result = false
            case .trustedOwner:
                result = true
        }
        return result
    }

    private static func makeVirtualTours(with badge: Badge.Rent) -> [VirtualTour]? {
        let result: [VirtualTour]?
        switch badge {
            case .virtualTour:
                result = Self.makeVirtualTours()
            case .yandexRent, .trustedOwner:
                result = nil
        }
        return result
    }

    private static func makeVirtualTours() -> [VirtualTour] {
        let url = URL(string: "https://yandex.ru")!
        let matterportTour = MatterportTour(url: url, previewImage: nil)
        return [VirtualTour(tour: .matterport(matterportTour))]
    }

    private static func makeAuthor() -> YREAuthor {
        let author = YREAuthor(
            authorID: nil,
            category: .owner,
            agentName: nil,
            organization: nil,
            name: nil,
            phones: nil,
            authorPhones: nil,
            photoURL: nil,
            humanPhotoURL: nil,
            profile: nil,
            communicationChannelType: .onlyPhone
        )
        return author
    }
}
