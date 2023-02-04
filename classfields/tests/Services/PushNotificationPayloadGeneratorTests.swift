//
//  PushNotificationPayloadGeneratorTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 17.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import XCTest
@testable import YREServiceLayer

// https://wiki.yandex-team.ru/users/azakharov/trigger-push/
// https://wiki.yandex-team.ru/users/nstaroverova/Rabota-s-izbrannym/
// https://st.yandex-team.ru/VSAPPS-2453

class PushNotificationPayloadGeneratorTests: XCTestCase {
    // Contains scheme, host and path
    fileprivate let correctDeepLink = "https://yandex.ru/something"
    // Path is missing
    fileprivate let incorrectDeepLink = "https://yandex.ru/"
    // Not an URL at all
    fileprivate let wrongURL = "not-a-link"
}

// MARK: - Unified Format - Open App

extension PushNotificationPayloadGeneratorTests {
    func testMakePayloadFromOpenAppRawPayload() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "OPEN" as AnyObject,
                "push_id": "some-id" as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNotNil(result)
    }
    
    func testMakePayloadFromOpenAppRawPayloadCaseInsensitive() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "OpEn" as AnyObject,
                "push_id": "some-id" as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNotNil(result)
    }
    
    func testMakePayloadFromOpenAppRawPayloadWithWrongAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "not-open" as AnyObject,
                "push_id": "some-id" as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenAppRawPayloadWithWrongKey() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "not-action": "OPEN" as AnyObject,
                "push_id": "some-id" as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenAppRawPayloadWithoutPushID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": ["action": "OPEN" as AnyObject] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    
    func testMakePayloadFromOpenAppRawPayloadWithWrongPushID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "OPEN" as AnyObject,
                "push_id": 123 as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
}

// MARK: - Unified Format - Open DeepLink


extension PushNotificationPayloadGeneratorTests {
    func testMakePayloadFromOpenDeepLinkRawPayload() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNotNil(result)
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadCaseInsensitive() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "DeEplinK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNotNil(result)
    }

    func testMakePayloadFromOpenDeepLinkRawPayloadWithRecipientID() {
        let recipientID = "1234"
        let rawPayload = [
            "user_info": [
                "action": "DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject,
                    "recipient_id": recipientID
                ] as AnyObject
            ] as AnyObject
        ]

        let generator = PushNotificationPayloadGenerator()

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }
        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action type")
            return
        }
        guard case .unified(_, _, let uid) = format else {
            XCTFail("Wrong format type")
            return
        }
        XCTAssertEqual(uid, recipientID)
    }

    func testMakePayloadFromOpenDeepLinkRawPayloadWithWrongAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "not-DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadWithWrongKey() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "not-action": "DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadWithoutPushID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "DEEPLINK" as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadWithWrongPushID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "not-action": "DEEPLINK" as AnyObject,
                "push_id": 123 as AnyObject,
                "data": [
                    "url": self.correctDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadWithoutURL() {
        let generator = PushNotificationPayloadGenerator()
        
        let rawPayload1 = [
            "user_info": [
                "not-action": "DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [] as AnyObject
            ] as AnyObject
        ]
        
        let rawPayload2 = [
            "user_info": [
                "not-action": "DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject
            ] as AnyObject
        ]
        
        XCTAssertNil(generator.makePayload(fromRawPayload: rawPayload1))
        XCTAssertNil(generator.makePayload(fromRawPayload: rawPayload2))
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadWithWrongURL() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "not-DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.wrongURL as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromOpenDeepLinkRawPayloadWithIncorrectDeepLink() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "not-DEEPLINK" as AnyObject,
                "push_id": "some-id" as AnyObject,
                "data": [
                    "url": self.incorrectDeepLink as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
}

// MARK: - Metrica DeepLinks

extension PushNotificationPayloadGeneratorTests {
    func testMakePayloadFromMetricaRawPayload() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["yamp": ["l": self.correctDeepLink as AnyObject] as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNotNil(result)
    }
    
    func testMakePayloadFromMetricaRawPayloadWithWrongURL() {
        let generator = PushNotificationPayloadGenerator()
        // URL doesn't contain any scheme
        let rawPayload = ["yamp": ["l": self.wrongURL as AnyObject] as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromMetricaRawPayloadWithIncorrectDeepLinkURL() {
        let generator = PushNotificationPayloadGenerator()
        // URL doesn't contain any path
        let rawPayload = ["yamp": ["l": self.incorrectDeepLink as AnyObject] as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromMetricaRawPayloadWithWrongDataFormat() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["yamp": ["not-l": self.correctDeepLink as AnyObject] as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromMetricaRawPayloadWithWrongTopDataFormat() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["not-yamp": ["l": self.correctDeepLink as AnyObject] as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromMetricaRawPayloadWithMissingData() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["any-other-key": "1234" as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
}

// MARK: - Legacy Subscriptions

extension PushNotificationPayloadGeneratorTests {
    func testMakePayloadFromLegacySubscriptionRawPayload() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["subscriptionId": "1234" as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNotNil(result)
    }
    
    func testMakePayloadFromLegacySubscriptionRawPayloadWithWrongData() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["subscriptionId": 1 as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testMakePayloadFromLegacySubscriptionRawPayloadWithMissingData() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = ["any-other-key": "1234" as AnyObject]
        
        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
}

// MARK: - Chat new message push

extension PushNotificationPayloadGeneratorTests {
    func testValidChatMessagePush() {
        let generator = PushNotificationPayloadGenerator()
        let userID = "someUserID"
        let roomID = "someRoomID"
        let expectedPushID = "CHAT_NEW_MESSAGE"
        let rawPayload = [
            "user_info": [
                "action": "chat_new_message" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                    "chat_id": roomID as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .chatNewMessage(pushID, pushData) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(pushData.recipientID, userID)
        XCTAssertEqual(pushData.roomID, roomID)
    }

    func testInvalidChatMessageAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_chat_new_message_action" as AnyObject,
                "push_id": "CHAT_NEW_MESSAGE" as AnyObject,
                "data": [
                    "recipient_id": "someUserID" as AnyObject,
                    "chat_id": "someRoomID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyChatMessageRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "chat_new_message" as AnyObject,
                "push_id": "CHAT_NEW_MESSAGE" as AnyObject,
                "data": [
                    "chat_id": "someRoomID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyChatMessageRoomID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "chat_new_message" as AnyObject,
                "push_id": "CHAT_NEW_MESSAGE" as AnyObject,
                "data": [
                    "recipient_id": "someUserID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
}

// MARK: - Tenant payment push

extension PushNotificationPayloadGeneratorTests {
    func testValidTenantPaymentPush() {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let flatID = "flatID"
        let paymentID = "paymentID"
        let expectedPushID = "TENANT_RENT_FIRST_PAYMENT"
        let rawPayload = [
            "user_info": [
                "action": "tenant_rent_payment" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                    "flat_id": flatID as AnyObject,
                    "payment_id": paymentID as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .tenantPayment(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
        XCTAssertEqual(payload.flatID, flatID)
        XCTAssertEqual(payload.paymentID, paymentID)
    }

    func testInvalidTenantPaymentAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_tenant_rent_payment" as AnyObject,
                "push_id": "TENANT_RENT_FIRST_PAYMENT" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "flat_id": "flatID" as AnyObject,
                    "payment_id": "paymentID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyTenantPaymentRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "tenant_rent_payment" as AnyObject,
                "push_id": "TENANT_RENT_FIRST_PAYMENT" as AnyObject,
                "data": [
                    "flat_id": "flatID" as AnyObject,
                    "payment_id": "paymentID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyTenantPaymentFlatID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "tenant_rent_payment" as AnyObject,
                "push_id": "TENANT_RENT_FIRST_PAYMENT" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "payment_id": "paymentID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
    
    func testEmptyTenantPaymentPaymentID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "tenant_rent_payment" as AnyObject,
                "push_id": "TENANT_RENT_FIRST_PAYMENT" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "flat_id": "flat_id" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }
}

// MARK: - OwnerBankCardsPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOwnerBankCardsPush() {
        let expectedPushIDs = [
            "OWNER_RENT_WITHOUT_CARD",
            "OWNER_RENT_CARD_UNAVAILABLE",
            "OWNER_RENT_WITH_MANY_CARDS",
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOwnerBankCardsPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOwnerBankCardsAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_owner_rent_cards" as AnyObject,
                "push_id": "OWNER_RENT_WITHOUT_CARD" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerBankCardsRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_rent_cards" as AnyObject,
                "push_id": "OWNER_RENT_WITHOUT_CARD" as AnyObject,
                "data": [
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOwnerBankCardsPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let rawPayload = [
            "user_info": [
                "action": "owner_rent_cards" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .ownerRentCards(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
    }
}

// MARK: - RentPaymentHistoryPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidRentPaymentHistoryPush() {
        let expectedPushIDs = [
            "OWNER_RENT_PAID_OUT_TO_CARD",
            "OWNER_RENT_PAID_OUT_TO_ACCOUNT",
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidRentPaymentHistoryPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidRentPaymentHistoryAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_rent_payment_history" as AnyObject,
                "push_id": "OWNER_RENT_PAID_OUT_TO_CARD" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "flat_id": "flat_id" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyRentPaymentHistoryRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "rent_payment_history" as AnyObject,
                "push_id": "OWNER_RENT_PAID_OUT_TO_CARD" as AnyObject,
                "data": [
                    "flat_id": "flat_id" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyRentPaymentHistoryFlatID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "rent_payment_history" as AnyObject,
                "push_id": "OWNER_RENT_PAID_OUT_TO_CARD" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidRentPaymentHistoryPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let rawPayload = [
            "user_info": [
                "action": "rent_payment_history" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                    "flat_id": "flat_id" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .rentPaymentHistory(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
    }
}

// MARK: - OpenServicesPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOpenServicesPush() {
        let expectedPushIDs = [
            "OWNER_RENT_PAYOUT_BROKEN",
            "OWNER_ADD_FLAT_INFO",
            "OWNER_REQUEST_DECLINED",
            "OWNER_REQUEST_ACCEPTED",
            "OWNER_OFFER_PUBLISHED"
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOpenServicesPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOpenServicesAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_open_services" as AnyObject,
                "push_id": "OWNER_RENT_PAYOUT_BROKEN" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOpenServicesRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "open_services" as AnyObject,
                "push_id": "OWNER_RENT_PAYOUT_BROKEN" as AnyObject,
                "data": [
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOpenServicesPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let rawPayload = [
            "user_info": [
                "action": "open_services" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .openServices(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
    }
}

// MARK: - OwnerRentINNPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOwnerRentINNPush() {
        let expectedPushIDs = [
            "OWNER_RENT_WITHOUT_INN",
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOwnerRentINNPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOwnerRentINNAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_owner_rent_inn" as AnyObject,
                "push_id": "OWNER_RENT_WITHOUT_INN" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerRentINNRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_rent_inn" as AnyObject,
                "push_id": "OWNER_RENT_WITHOUT_INN" as AnyObject,
                "data": [
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOwnerRentINNPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let rawPayload = [
            "user_info": [
                "action": "owner_rent_inn" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .ownerRentINN(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
    }
}

// MARK: - OwnerDraftRequestPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOwnerDraftRequestPush() {
        let expectedPushIDs = [
            "OWNER_DRAFT_NEED_TO_FINISH",
            "OWNER_DRAFT_NEED_CONFIRMATION"
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOwnerDraftRequestPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOwnerDraftRequestAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_owner_draft_request" as AnyObject,
                "push_id": "OWNER_DRAFT_NEED_TO_FINISH" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "flat_id": "flatID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerDraftRequestRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_draft_request" as AnyObject,
                "push_id": "OWNER_DRAFT_NEED_TO_FINISH" as AnyObject,
                "data": [
                    "flat_id": "flatID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerDraftRequestFlatID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_draft_request" as AnyObject,
                "push_id": "OWNER_DRAFT_NEED_TO_FINISH" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOwnerDraftRequestPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let flatID = "flatID"
        let rawPayload = [
            "user_info": [
                "action": "owner_draft_request" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                    "flat_id": flatID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .ownerDraftRequest(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
        XCTAssertEqual(payload.flatID, flatID)
    }
}

// MARK: - OwnerAddPassportPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOwnerAddPassportPush() {
        let expectedPushIDs = [
            "OWNER_ADD_PASSPORT",
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOwnerAddPassportPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOwnerAddPassportAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_owner_add_passport" as AnyObject,
                "push_id": "OWNER_ADD_PASSPORT" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerAddPassportRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_add_passport" as AnyObject,
                "push_id": "OWNER_ADD_PASSPORT" as AnyObject,
                "data": [
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOwnerAddPassportPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let rawPayload = [
            "user_info": [
                "action": "owner_add_passport" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .ownerAddPassport(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
    }
}

// MARK: - OwnerHouseServiceConfigurationPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOwnerHouseServiceConfigurationPush() {
        let expectedPushIDs = [
            "OWNER_HOUSE_SERVICE_CONDITIONS_CONFIGURATION",
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOwnerHouseServiceConfigurationPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOwnerHouseServiceConfigurationAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_owner_house_service_conditions_configuration" as AnyObject,
                "push_id": "OWNER_HOUSE_SERVICE_CONDITIONS_CONFIGURATION" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "flat_id": "flatID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerHouseServiceConfigurationRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_house_service_conditions_configuration" as AnyObject,
                "push_id": "OWNER_HOUSE_SERVICE_CONDITIONS_CONFIGURATION" as AnyObject,
                "data": [
                    "flat_id": "flatID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerHouseServiceConfigurationFlatID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_house_service_conditions_configuration" as AnyObject,
                "push_id": "OWNER_HOUSE_SERVICE_CONDITIONS_CONFIGURATION" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOwnerHouseServiceConfigurationPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let flatID = "flatID"
        let rawPayload = [
            "user_info": [
                "action": "owner_house_service_conditions_configuration" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                    "flat_id": flatID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .ownerHouseServiceConfiguration(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
        XCTAssertEqual(payload.flatID, flatID)
    }
}

// MARK: - OwnerCheckTenantCandidatesPushPayloadGenerator

extension PushNotificationPayloadGeneratorTests {
    func testValidOwnerCheckTenantCandidatesPush() {
        let expectedPushIDs = [
            "OWNER_CHECK_TENANT_CANDIDATES",
        ]
        expectedPushIDs.forEach({ expectedPushID in
            self.testValidOwnerCheckTenantCandidatesPush(expectedPushID: expectedPushID)
        })
    }

    func testInvalidOwnerCheckTenantCandidatesAction() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "wrong_owner_check_tenant_candidates" as AnyObject,
                "push_id": "OWNER_CHECK_TENANT_CANDIDATES" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                    "flat_id": "flatID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerCheckTenantCandidatesRecipientID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_check_tenant_candidates" as AnyObject,
                "push_id": "OWNER_CHECK_TENANT_CANDIDATES" as AnyObject,
                "data": [
                    "flat_id": "flatID" as AnyObject
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    func testEmptyOwnerCheckTenantCandidatesFlatID() {
        let generator = PushNotificationPayloadGenerator()
        let rawPayload = [
            "user_info": [
                "action": "owner_check_tenant_candidates" as AnyObject,
                "push_id": "OWNER_CHECK_TENANT_CANDIDATES" as AnyObject,
                "data": [
                    "recipient_id": "userID" as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        let result = generator.makePayload(fromRawPayload: rawPayload)
        XCTAssertNil(result)
    }

    private func testValidOwnerCheckTenantCandidatesPush(expectedPushID: String) {
        let generator = PushNotificationPayloadGenerator()
        let userID = "userID"
        let flatID = "flatID"
        let rawPayload = [
            "user_info": [
                "action": "owner_check_tenant_candidates" as AnyObject,
                "push_id": expectedPushID as AnyObject,
                "data": [
                    "recipient_id": userID as AnyObject,
                    "flat_id": flatID as AnyObject,
                ] as AnyObject
            ] as AnyObject
        ]

        guard let result = generator.makePayload(fromRawPayload: rawPayload) else {
            XCTFail("Couldn't make payload")
            return
        }

        guard case .openDeepLink(let format) = result.action else {
            XCTFail("Wrong action")
            return
        }

        guard case let .ownerCheckTenantCandidates(pushID, payload) = format else {
            XCTFail("Wrong format")
            return
        }

        XCTAssertEqual(pushID, expectedPushID)
        XCTAssertEqual(payload.recipientID, userID)
        XCTAssertEqual(payload.flatID, flatID)
    }
}
