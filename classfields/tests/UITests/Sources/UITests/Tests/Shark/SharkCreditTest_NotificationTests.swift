//
//  SharkCreditTest_NotificationTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 29.10.2021.
//

import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleCard AutoRuCredit AutoRuPreliminaryCreditClaim
class SharkCreditTest_NotificationTests: BaseTest {
    struct ClosedAlert: Codable {
        enum Mode: String, Codable {
            case draft
            case draftWithOffer
            case sold
        }

        let modes: Set<Mode>
        let applicationId: String
    }

    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]
    var userDefaults: [String: Any] = [:]
    var sharkMocker: SharkMocker!

    override var appSettings: [String: Any] {
        return settings
    }

    override var launchEnvironment: [String: String] {
        var value = super.launchEnvironment
        let userDefaultsJsonData = try! JSONSerialization.data(withJSONObject: userDefaults, options: [])
        value["STANDARD_USER_DEFAULTS"] = userDefaultsJsonData.base64EncodedString()
        return value
    }

    private var offerId = "1098252972-99d8c274"

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["webHosts"] = "http://127.0.0.1:\(port)"
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]
        settings["skipCreditAlert"] = false
        sharkMocker = SharkMocker(server: server)
    }

    func test_draft_noOffer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        launch()

        mainSteps
            .validateSnapShot(accessibilityId: "CreditAlertViewController", snapshotId: "CreditAlertViewController_draft_noOffer")
    }

    func test_draft_offer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .offer(id: offerId, isActive: true, allowedProducts: [.tinkoff_1]))
            .start()

        launch()

        mainSteps
            .validateSnapShot(accessibilityId: "CreditAlertViewController", snapshotId: "CreditAlertViewController_draft_offer")
    }

    func test_draft_offer_noActive() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .offer(id: offerId, isActive: false, allowedProducts: [.tinkoff_1]))
            .start()

        launch()

        mainSteps
            .validateSnapShot(accessibilityId: "CreditAlertViewController", snapshotId: "CreditAlertViewController_offer_noActive")
    }

    func test_draft_notShowIfCloseDraft() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .none)
            .start()

        let alert = ClosedAlert(modes: [.draft], applicationId: sharkMocker.applicationId)
        let data = try! JSONEncoder().encode(alert)
        userDefaults["lastApplicationIdForClosedNotification"] = String(data: data, encoding: .utf8)

        launch()

        mainSteps
            .wait(for: 3)
            .notExist(selector: "CreditAlertViewController")
    }

    func test_draft_showDraftWithOfferIfCloseDraft() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .offer(id: offerId, isActive: true, allowedProducts: [.tinkoff_1]))
            .start()

        let alert = ClosedAlert(modes: [.draft], applicationId: sharkMocker.applicationId)
        let data = try! JSONEncoder().encode(alert)
        userDefaults["lastApplicationIdForClosedNotification"] = String(data: data, encoding: .utf8)

        launch()

        mainSteps
            .exist(selector: "CreditAlertViewController")
    }

    func test_draft_showDraftWithSoldOfferIfCloseDraftWithOffer() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .offer(id: offerId, isActive: false, allowedProducts: [.tinkoff_1]))
            .start()

        let alert = ClosedAlert(modes: [.draft, .draftWithOffer], applicationId: sharkMocker.applicationId)
        let data = try! JSONEncoder().encode(alert)
        userDefaults["lastApplicationIdForClosedNotification"] = String(data: data, encoding: .utf8)
        launch()

        mainSteps
            .exist(selector: "CreditAlertViewController")
    }

    func test_draft_showForNewApp() {
        sharkMocker
            .baseMock(offerId: offerId)
            .mockOffer(withSharkInfo: true)
            .mockProductList(products: [.tinkoff_1, .tinkoff_2])
            .mockApplication(status: .draft, claims: [], offerType: .offer(id: offerId, isActive: false, allowedProducts: [.tinkoff_1]))
            .start()

        let alert = ClosedAlert(modes: [.draft, .draftWithOffer, .sold], applicationId: "oldApp")
        let data = try! JSONEncoder().encode(alert)
        userDefaults["lastApplicationIdForClosedNotification"] = String(data: data, encoding: .utf8)
        launch()

        mainSteps
            .exist(selector: "CreditAlertViewController")
    }
}
