//
//  DeepLinkValidatorTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 16.02.18.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YREServiceLayer

// @coreshock: It's legal to use force-unwrapped URL constructors because:
// * it is fixture
// * we know that provided URL-string is absolutely valid
// * it is test

// swiftlint:disable force_unwrapping

class DeepLinkValidatorTests: XCTestCase {
    func testDeepLinkWithSchemeOnly() {
        let universalSchemeUrl = URL(string: "https://")!
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: universalSchemeUrl))
        
        let customSchemeUrl = URL(string: "yandexrealty://")!
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: customSchemeUrl))
    }
    
    func testDeepLinkWithoutPath() {
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: URL(string: "https://just.host")!))
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: URL(string: "https://just.host/")!))
        
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: URL(string: "yandexrealty://just.host")!))
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: URL(string: "yandexrealty://just.host/")!))
    }
    
    func testDeepLinkFromAllYandexApps() {
        let url = URL(string: "yandexrealty://m.realty.yandex.ru/?nosplash=1&nosmart=1")!
        XCTAssertFalse(DeepLinkValidator.isValidDeepLink(withURL: url))
    }
    
    func testCorrectDeepLink() {
        let universal = URL(string: "https://realty.yandex.ru/newbuilding/23105")!
        XCTAssertTrue(DeepLinkValidator.isValidDeepLink(withURL: universal))
        
        let custom = URL(string: "yandexrealty://realty.yandex.ru/newbuilding/23105")!
        XCTAssertTrue(DeepLinkValidator.isValidDeepLink(withURL: custom))
    }

    func testYandexDeepLink() {
        // Checking for allowed schemes
        XCTAssertTrue(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "yandexrealty://realty.yandex.ru/")!))
        XCTAssertTrue(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "notYandexRealty://realty.yandex.ru/")!))
        XCTAssertTrue(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "https://realty.yandex.ru/")!))

        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "yandexrealty://realty.tplaymeow.com/")!))
        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "notYandexRealty://realty.tplaymeow.com/")!))
        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "https://realty.tplaymeow.com/")!))

        // Checking for unallowed schemes
        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "http://realty.yandex.ru/")!))
        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "nothttps://realty.yandex.ru/")!))
        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "nothttps://realty.tplaymeow.com/")!))

        // Check case sensitive
        XCTAssertTrue(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "https://Arenda.Yandex.ru/")!))
        XCTAssertFalse(DeepLinkValidator.isYandexDeepLink(withURL: URL(string: "HTTPS://realty.yandex.ru/")!))
    }

    func testProcessedYandexDeepLink() {
        func processedLinks(_ links: [String]) -> [String] {
            links.compactMap(URL.init).map(DeepLinkValidator.processedYandexDeepLink).map { $0.absoluteString }
        }

        // Processing should transform passed links
        let allowedLinks: [String] = [
            "yandexrealty://realty.yandex.ru/test",
            "notYandexRealty://realty.yandex.ru/test",
            "https://realty.yandex.ru/test",
        ]
        let expectedLinks: [String] = .init(repeating: "https://realty.yandex.ru/test", count: allowedLinks.count)
        XCTAssertEqual(processedLinks(allowedLinks), expectedLinks)

        // Processing should doesn't change passed links
        let notAllowedLinks: [String] = [
            "anyhttps://realty.yandex.ru/test",
            "anynotYandexRealty://realty.yandex.ru/test",
            "anyyandexrealty://realty.yandex.ru/test",
            "httpsclone://realty.yandex.ru/test",
            "notYandexRealtyClone://realty.yandex.ru/test",
            "yandexrealtyclone://realty.yandex.ru/test",
        ]
        XCTAssertEqual(processedLinks(notAllowedLinks), notAllowedLinks)
    }
}
