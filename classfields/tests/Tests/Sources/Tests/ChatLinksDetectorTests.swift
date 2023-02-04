//
//  ChatLinkDetectorTests.swift
//  Tests
//
//  Created by Roman Bevza on 2/20/21.
//

import UIKit
import XCTest
@testable import AutoRuChat
import Foundation

final class ChatLinksDetectorTests: BaseUnitTest {
    func test_chatLinksDetector() {
        let urls: [String: Bool] = ["https://auto.ru-infosale.ru/get/99993547342": false,
                                    "https://infosale-auto.ru.ru/get/99993547342": false,
                                    "https://auto.ru/cars/new/group/peugeot/3008/21010060/21005129/1102530348-363c9571/": true,
                                    "https://avto.ru/cars/new/group/peugeot/3008/21010060/21005129/1102530348-363c9571/": true,
                                    "https://www.drive2.ru/cars/hyundai/": true,
                                    "https://www.google.com/": false,
                                    "https://mag.auto.ru/article/bigchinesopponents/": true
        ]
        urls.forEach { (url, shouldMatch) in
            XCTContext.runActivity(named: "Проверяем URL \(url): Ожидаем: \(shouldMatch ? "валидный" : "невалидный")") { _ in
                XCTAssertEqual(ChatLinksDetector.matchAllowedHosts(URL(string: url)!), shouldMatch)
            }
        }
    }
}
