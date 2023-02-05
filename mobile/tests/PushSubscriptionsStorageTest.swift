//
//  Created by Timur Turaev on 07.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import TestUtils
@testable import Utils

public final class PushSubscriptionsStorageTest: XCTestCase {
    private var keychain: YOUICKeyChainStore!
    private var storage: PushSubscriptionStorage!

    public override func setUpWithError() throws {
        try super.setUpWithError()

        self.keychain = YOUICKeyChainStore(service: "ru.yandex.mail.test.PushSubscriptionsStorageTest.v0")
        try self.keychain.removeAllItemsWithError()
        self.storage = PushSubscriptionStorage(logger: TestLogger(), store: self.keychain)
    }

    func testFetchFromEmptyStorage() throws {
        let dups = self.storage.fetchDuplicateSubscriptionsForUID(101, mailService: .mailRu, UUID: "uuid_0", deviceID: "device_0", pushToken: "token_0")
        XCTAssertTrue(dups.isEmpty)
    }

    func testCompletionSubscription() throws {
        self.storage.setSubscriptionCompletedForUID(101, mailService: .yandex, UUID: nil, deviceID: "device_0", pushToken: "token_0")
        XCTAssertTrue(self.storage.allInfos.isEmpty)

        self.storage.setSubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_0", deviceID: nil, pushToken: "token_0")
        XCTAssertTrue(self.storage.allInfos.isEmpty)

        self.storage.setSubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_0", deviceID: "device_0", pushToken: "token_0")
        self.storage.setSubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_01", deviceID: "device_01", pushToken: "token_01")
        self.storage.setSubscriptionCompletedForUID(102, mailService: .yandexTeam, UUID: "uuid_1", deviceID: "device_1", pushToken: "token_1")

        XCTAssertEqual(self.storage.allInfos, [
            NSNumber(value: 101): [
                PushSubscriptionInfo(UUID: "uuid_0", deviceID: "device_0", mailService: .yandex, pushToken: "token_0"),
                PushSubscriptionInfo(UUID: "uuid_01", deviceID: "device_01", mailService: .yandex, pushToken: "token_01")
            ],
            NSNumber(value: 102): [
                PushSubscriptionInfo(UUID: "uuid_1", deviceID: "device_1", mailService: .yandexTeam, pushToken: "token_1")
            ]
        ])
    }

    func testUnsubscriptions() throws {
        self.storage.setSubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_0", deviceID: "device_0", pushToken: "token_0")
        self.storage.setSubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_01", deviceID: "device_01", pushToken: "token_01")
        self.storage.setSubscriptionCompletedForUID(102, mailService: .yandexTeam, UUID: "uuid_1", deviceID: "device_1", pushToken: "token_1")

        self.storage.setUnsubscriptionCompletedForUID(103, mailService: .google, UUID: "", deviceID: "", pushToken: "")
        XCTAssertEqual(self.storage.allInfos, [
            NSNumber(value: 101): [
                PushSubscriptionInfo(UUID: "uuid_0", deviceID: "device_0", mailService: .yandex, pushToken: "token_0"),
                PushSubscriptionInfo(UUID: "uuid_01", deviceID: "device_01", mailService: .yandex, pushToken: "token_01")
            ],
            NSNumber(value: 102): [
                PushSubscriptionInfo(UUID: "uuid_1", deviceID: "device_1", mailService: .yandexTeam, pushToken: "token_1")
            ],
            NSNumber(value: 103): [
            ]
        ])
        self.storage.setUnsubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_0", deviceID: "device_0001", pushToken: "token_0")
        XCTAssertEqual(self.storage.allInfos, [
            NSNumber(value: 101): [
                PushSubscriptionInfo(UUID: "uuid_0", deviceID: "device_0", mailService: .yandex, pushToken: "token_0"),
                PushSubscriptionInfo(UUID: "uuid_01", deviceID: "device_01", mailService: .yandex, pushToken: "token_01")
            ],
            NSNumber(value: 102): [
                PushSubscriptionInfo(UUID: "uuid_1", deviceID: "device_1", mailService: .yandexTeam, pushToken: "token_1")
            ],
            NSNumber(value: 103): [
            ]
        ])

        self.storage.setUnsubscriptionCompletedForUID(101, mailService: .yandex, UUID: "uuid_01", deviceID: "device_01", pushToken: "token_01")
        XCTAssertEqual(self.storage.allInfos, [
            NSNumber(value: 101): [
                PushSubscriptionInfo(UUID: "uuid_0", deviceID: "device_0", mailService: .yandex, pushToken: "token_0")
            ],
            NSNumber(value: 102): [
                PushSubscriptionInfo(UUID: "uuid_1", deviceID: "device_1", mailService: .yandexTeam, pushToken: "token_1")
            ],
            NSNumber(value: 103): [
            ]
        ])

        self.storage.removeAllInfoForUID(101)
        XCTAssertEqual(self.storage.allInfos, [
            NSNumber(value: 102): [
                PushSubscriptionInfo(UUID: "uuid_1", deviceID: "device_1", mailService: .yandexTeam, pushToken: "token_1")
            ],
            NSNumber(value: 103): [
            ]
        ])
        self.storage.removeAllInfoForUID(102)
        XCTAssertEqual(self.storage.allInfos, [
            NSNumber(value: 103): [
            ]
        ])
    }

    func testFetchingDups() throws {
        let info = PushSubscriptionInfo(UUID: "uuid_0", deviceID: "device_0", mailService: .hotmail, pushToken: "token_0")
        self.storage.setSubscriptionCompletedForUID(101, mailService: info.mailService, UUID: info.UUID, deviceID: info.deviceID, pushToken: info.pushToken)

        XCTAssertEqual(self.storage.fetchDuplicateSubscriptionsForUID(101, mailService: info.mailService, UUID: info.UUID, deviceID: info.deviceID, pushToken: info.pushToken),
                       [])
        XCTAssertEqual(self.storage.fetchDuplicateSubscriptionsForUID(101, mailService: .google, UUID: info.UUID, deviceID: info.deviceID, pushToken: info.pushToken),
                       [info])
        XCTAssertEqual(self.storage.fetchDuplicateSubscriptionsForUID(101, mailService: info.mailService, UUID: "info.UUID", deviceID: info.deviceID, pushToken: info.pushToken),
                       [info])
        XCTAssertEqual(self.storage.fetchDuplicateSubscriptionsForUID(101, mailService: info.mailService, UUID: info.UUID, deviceID: "info.deviceID", pushToken: info.pushToken),
                       [info])
        XCTAssertEqual(self.storage.fetchDuplicateSubscriptionsForUID(101, mailService: info.mailService, UUID: info.UUID, deviceID: info.deviceID, pushToken: "info.pushToken"),
                       [info])
    }
}
