//
//  UserOffersAPIStubConfigurator.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 19.03.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class UserOffersAPIStubConfigurator {
    static let userOfferID: String = "4954951532541678849"

    enum StubKind {
        enum List {
            enum Common: String {
                case common = "userOffersList-banned.debug"
                case empty = "userOffersList-empty.debug"
                case fromFeed = "userOffersList-feedOffer-banned.debug"
                case failure = "commonError.debug"
            }

            enum Banned: String {
                case common = "userOffersList-banned.debug"
                case fromFeed = "userOffersList-feedOffer-banned.debug"
            }

            enum OnModeration: String {
                case free = "userOffersList-moderation-free.debug"
                case paid = "userOffersList-moderation-paid.debug"
                case unpaid = "userOffersList-moderation-unpaid.debug"
                case fromFeed = "userOffersList-feedOffer-moderation.debug"

                case legalEntityNotEnoughFunds = "legalEntity-userOffersList-moderation-unpaid-notEnoughFunds.debug"
                case legalEntityInProgress = "legalEntity-userOffersList-moderation-unpaid-inProgress.debug"
            }

            enum Published: String {
                case free = "userOffersList-published-free.debug"
                case paid = "userOffersList-published-paid.debug"
                case unpaid = "userOffersList-published-unpaid.debug"
                case fromFeed = "userOffersList-feedOffer-published.debug"
            }

            enum Unpublished: String {
                case free = "userOffersList-unpublished-free.debug"
                case paid = "userOffersList-unpublished-paid.debug"
                case unpaid = "userOffersList-unpublished-unpaid.debug"
                case fromFeed = "userOffersList-feedOffer-unpublished.debug"

                case unpaidWithPendingPlacement = "userOffersList-unpublished-unpaid-with-pending-placement.debug"

                case legalEntityNotEnoughFunds = "legalEntity-userOffersList-unpublished-unpaid-notEnoughFunds.debug"
                case legalEntityInProgress = "legalEntity-userOffersList-unpublished-unpaid-inProgress.debug"
            }

            // swiftlint:disable:next type_name
            enum VAS: String {
                case common = "userOffersList-vas-common.debug"
                case discount = "userOffersList-vas-discount.debug"
                case free = "userOffersList-vas-free.debug"
                case activatedTurboDiscount = "userOffersList-vas-activatedTurbo-discount.debug"
                case activatedTurboFree = "userOffersList-vas-activatedTurbo-free.debug"
                case renewalTurboDiscount = "userOffersList-vas-renewalTurbo-discount.debug"
                case renewalTurboFree = "userOffersList-vas-renewalTurbo-free.debug"
                case allInactivate = "userOffersList-vas-all-inactivate.debug"
            }

            enum VASAutoPurchase: String {
                case raisingActivate = "vas-raising-activate.debug"
                case premiumActivate = "vas-premium-activate.debug"
                case promotionActivate = "vas-promotion-activate.debug"
                case allInactivate = "userOffersList-vas-all-inactivate.debug"
            }
        }

        enum Products: String {
            case turbo = "userOffersList-products-turbo.debug"
            case premium = "userOffersList-products-premium.debug"
            case raising = "userOffersList-products-raising.debug"
            case promotion = "userOffersList-products-promotion.debug"
        }

        enum Card {
            enum Banned: String {
                case common = "userOffersCard-banned.debug"
                case fromFeed = "userOffersCard-feedOffer-banned.debug"
            }

            enum OnModeration: String {
                case free = "userOffersCard-moderation-free.debug"
                case paid = "userOffersCard-moderation-paid.debug"
                case unpaid = "userOffersCard-moderation-unpaid.debug"
                case fromFeed = "userOffersCard-feedOffer-moderation.debug"
            }

            enum Published: String {
                case free = "userOffersCard-published-free.debug"
                case paid = "userOffersCard-published-paid.debug"
                case withoutShareLink = "userOffersCard-published-withoutShareLink.debug"
                case unpaid = "userOffersCard-published-unpaid.debug"
                case fromFeed = "userOffersCard-feedOffer-published.debug"
            }

            enum Unpublished: String {
                case free = "userOffersCard-unpublished-free.debug"
                case paid = "userOffersCard-unpublished-paid.debug"
                case unpaid = "userOffersCard-unpublished-unpaid.debug"
                case fromFeed = "userOffersCard-feedOffer-unpublished.debug"

                case unpaidWithPendingPlacement = "userOffersCard-unpublished-unpaid-with-pending-placement.debug"
            }

            enum MosRu: String {
                case offerBanned = "userOffersCard-mosRu-offerBanned.debug"
                case offerOnModeration = "userOffersCard-mosRu-offerOnModeration.debug"
                case offerRequiresPayment = "userOffersCard-mosRu-offerRequiresPayment.debug"

                case shortRentOffer = "userOffersCard-mosRu-shortRentOffer.debug"
                case longRentOffer = "userOffersCard-mosRu-longRentOffer.debug"

                case internalError = "userOffersCard-mosRuInternalError.debug"
                case notLinked = "userOffersCard-mosRuNotLinked.debug"
                case notOwner = "userOffersCard-mosRuNotOwner.debug"
                case notTrusted = "userOffersCard-mosRuNotTrusted.debug"
                case reportNotReceived = "userOffersCard-mosRuReportNotReceived.debug"
                case trustedMatchedFlat = "userOffersCard-mosRuTrusted-matchedFlat.debug"
                case trustedMatchedRentFlat = "userOffersCard-mosRuTrusted-matchedFlat-rent.debug"
                case trustedNoFlat = "userOffersCard-mosRuTrusted-noFlat.debug"
                case trustedNoRentFlat = "userOffersCard-mosRuTrusted-noFlat-rent.debug"
                case waitForCheckFlatExists = "userOffersCard-mosRuWaitForCheck-flatExists.debug"
                case waitForCheckNoFlat = "userOffersCard-mosRuWaitForCheck-noFlat.debug"
            }

			// swiftlint:disable:next type_name
			enum VAS: String {
				case common = "userOffersCard-vas-common.debug"
			}
        }
    }

    enum Paths {
        enum AutoPurchase {
            static let premium = "/2.0/user/me/renewals/offers/\(UserOffersAPIStubConfigurator.userOfferID)/premium"
            static let raising = "/2.0/user/me/renewals/offers/\(UserOffersAPIStubConfigurator.userOfferID)/raising"
            static let promotion = "/2.0/user/me/renewals/offers/\(UserOffersAPIStubConfigurator.userOfferID)/promotion"
        }
    }
}

// MARK: - Products

extension UserOffersAPIStubConfigurator {
    static func setupUserOffersProducts(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Products) {
        dynamicStubs.setupStub(remotePath: "/2.0/products/user/me/purchase/init",
                               filename: stubKind.rawValue,
                               method: .POST)
    }
}

// MARK: - List

extension UserOffersAPIStubConfigurator {
    static func setupUserOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.Common) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers",
                               filename: stubKind.rawValue)
    }

    static func setupBannedUserOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.Banned) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers",
                               filename: stubKind.rawValue)
    }

    static func setupOnModerationUserOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.OnModeration) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers",
                               filename: stubKind.rawValue)
    }

    static func setupPublishedUserOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.Published) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers",
                               filename: stubKind.rawValue)
    }

    static func setupUnpublishedUserOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.Unpublished) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers",
                               filename: stubKind.rawValue)
    }

    static func setupVASUserOffersList(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.VAS) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers",
                               filename: stubKind.rawValue)
    }

    static func setupVASAutoPurchaseRaising(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.VASAutoPurchase) {
        dynamicStubs.setupStub(remotePath: Paths.AutoPurchase.raising,
                               filename: stubKind.rawValue,
                               method: .PUT)
    }

    static func setupVASAutoPurchasePremium(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.VASAutoPurchase) {
        dynamicStubs.setupStub(remotePath: Paths.AutoPurchase.premium,
                               filename: stubKind.rawValue,
                               method: .PUT)
    }

    static func setupVASAutoPurchasePromotion(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.List.VASAutoPurchase) {
        dynamicStubs.setupStub(remotePath: Paths.AutoPurchase.promotion,
                               filename: stubKind.rawValue,
                               method: .PUT)
    }
}

// MARK: - Card

extension UserOffersAPIStubConfigurator {
    static func setupBannedUserOffersCard(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Card.Banned) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: stubKind.rawValue)
    }

    static func setupOnModerationUserOffersCard(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Card.OnModeration) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: stubKind.rawValue)
    }

    static func setupPublishedUserOffersCard(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Card.Published) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: stubKind.rawValue)
    }

    static func setupUnpublishedUserOffersCard(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Card.Unpublished) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: stubKind.rawValue)
    }

    static func setupUserOffersCardWithMosRuInfo(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Card.MosRu) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: stubKind.rawValue)
    }

    static func setupVASUserOffersCard(using dynamicStubs: HTTPDynamicStubs, stubKind: StubKind.Card.VAS) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: stubKind.rawValue)
    }
}

// MARK: - Common

extension UserOffersAPIStubConfigurator {
    static func setupAllSupportedRequiredFeaturesForUserOffers(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/device/requiredFeature",
                               filename: "requiredFeatures-userOffers-all-supported.debug")
    }

    static func setupUnsupportedRequiredFeaturesForUserOffers(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/device/requiredFeature",
                               filename: "requiredFeatures-userOffers-with-unsupported.debug")
    }

    static func setupUserOfferPreview(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user/offers/\(Self.userOfferID)",
                               filename: "userOffers-preview.debug")
    }

    static func setupUserOfferCardFailure(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/user/me/offers/\(Self.userOfferID)/card",
                               filename: "commonError.debug")
    }
}

// MARK: - Update

extension UserOffersAPIStubConfigurator {
    static func setupPublishFreeUserOffer(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user/offers/\(Self.userOfferID)/update_status",
                               filename: "updateUserOfferStatus-free.debug",
                               method: .PUT)
    }

    static func setupPublishUnpaidUserOffer(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user/offers/\(Self.userOfferID)/update_status",
                               filename: "updateUserOfferStatus-unpaid.debug",
                               method: .PUT)
    }

    static func setupPublishFailure(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/user/offers/\(Self.userOfferID)/update_status",
                               filename: "commonError.debug",
                               method: .PUT)
    }
}
