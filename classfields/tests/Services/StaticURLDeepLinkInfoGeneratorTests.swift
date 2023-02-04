//
//  StaticURLDeepLinkInfoGeneratorTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 26/09/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import YREServiceLayer
import class YREModel.DeepLinkSource
import class YREFiltersModel.DeepLinkInfo

class StaticURLDeepLinkInfoGeneratorTests: XCTestCase { }

// MARK: - Routing Deeplinks

extension StaticURLDeepLinkInfoGeneratorTests {
    func testVariousSchemesAndHosts() {
        let links = [
            "https://realty.yandex.ru/management",
            "https://m.realty.yandex.ru/management",
            "yandexrealty://realty.yandex.ru/management",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .userOffersList = type else { return false }
            return true
        })
    }

    func testUserOfferListDeeplinks() {
        let links = [
            "https://realty.yandex.ru/management-new",
            "https://realty.yandex.ru/management",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .userOffersList = type else { return false }
            return true
        })
    }

    func testUserOfferCardDeeplinks() {
        let links = [
            "https://realty.yandex.ru/management/offer/123",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .userOffer = type else { return false }
            return true
        })
    }

    func testAlfabankDeeplinks() {
        let links = [
            "https://realty.yandex.ru/alfabank",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .alfabank = type else { return false }
            return true
        })
    }

    func testEGRNReportDeeplinks() {
        let reportID = "1234"
        let links = [
            "https://realty.yandex.ru/egrn-report/\(reportID)",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .paidExcerpt(let id) = type else { return false }
            guard id == reportID else { return false }
            return true
        })
    }

    func testTechsupportChatDeeplinks() {
        let links = [
            "https://realty.yandex.ru/chat/techsupport",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .techSupportChat = type else { return false }
            return true
        })
    }

    func testChatRoomDeeplinks() {
        let roomID = "1234"
        let links = [
            "https://realty.yandex.ru/room/\(roomID)",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .chat(let id) = type else { return false }
            guard id == roomID else { return false }
            return true
        })
    }

    func testSimilarOffersDeeplinks() {
        let offerID = "1234567890987654321"
        let links = [
            "https://realty.yandex.ru/offer/\(offerID)/similar",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .similarOfferList(let data) = type else { return false }
            guard data.objectID == offerID else { return false }
            return true
        })
    }

    func testInAppServicesDeeplinks() {
        let links = [
            "https://bzfk.adj.st/services?adj_t=4inllqd_xgufl8n&adj_deep_link=yandexrealty%3A%2F%2Fservices",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .inAppServices = type else { return false }
            return true
        })
    }
    
    func testYaRentOwnerLandingDeeplinks() {
        let links = [
            "https://bzfk.adj.st/arenda/owner-landing?adj_t=4inllqd_xgufl8n&adj_deep_link=yandexrealty%3A%2F%2Farenda%2Fowner-landing",
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .ownerLanding = type else { return false }
            return true
        })
    }

    func testYaRentOwnerApplicationDeeplinks() {
        let links = [
            "https://bzfk.adj.st/lk/sdat-kvartiry?adj_t=4inllqd_xgufl8n",
            "https://arenda.yandex.ru/lk/sdat-kvartiry"
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { type in
            guard case .ownerDraftRequest = type else { return false }
            return true
        })
    }

    func testBadRoutingDeeplinks() {
        // All these cases might be tested separately, but I don't see a real profit
        let links = [
            "realty.yandex.ru/management-new",              // no scheme
            "https://management",                           // no host
            "yandexrealty://realty/management",             // bad host
            "yandexrealty://realty.yandex.ru",              // no path
            "yandexrealty://realty.yandex.ru/qwerty",       // wrong path
            "https://realty.yandex.ru/chat",                // wrong path - no chat room
            "https://realty.yandex.ru/chat/1234",           // wrong path - unknown chat room
            "https://realty.yandex.ru/egrn-report",         // wrong path - no report id
            "https://bzfk.adj.st/arenda",                   // wrong path - no internal section specified
        ]
        let urls = links.compactMap { URL(string: $0) }

        let generator = StaticURLDeepLinkInfoGenerator()

        urls.forEach { url in
            let source = DeepLinkSource(kind: .universalLink(url: url, referrerURL: nil))
            let result = generator.deepLinkInfo(forURL: url, source: source)

            XCTAssertNil(result, "Generated DeepLinkInfo for bad url \(url)")
        }
    }

    func testAMPDeeplinks() {
        let links = [
            "https://realty.yandex.ru/amp/management",
            "https://m.realty.yandex.ru/amp/management",
            "yandexrealty://realty.yandex.ru/amp/management",
            "https://arenda.yandex.ru/amp/management",
            "https://bzfk.adj.st/amp/management"
        ]
        self.checkCommonRoutingDeeplinks(links, typeComparator: { _ in
            true
        })
    }

    private func checkCommonRoutingDeeplinks(
        _ links: [String],
        typeComparator: (DeepLinkInfo.Action.Routing) -> Bool
    ) {
        let urls = links.compactMap { URL(string: $0) }

        let generator = StaticURLDeepLinkInfoGenerator()

        urls.forEach { url in
            let source = DeepLinkSource(kind: .universalLink(url: url, referrerURL: nil))

            guard let result = generator.deepLinkInfo(forURL: url, source: source) else {
                XCTFail("Couldn't generate DeepLinkInfo for url \(url)")
                return
            }

            guard
                case .routing(let type) = result.action,
                typeComparator(type) == true
            else {
                XCTFail("Wrong `action` \(result.action) in DeepLinkInfo for url \(url)")
                return
            }

            XCTAssertNil(result.rawActionName, "Unexpected `rawActionName` in DeepLinkInfo for url \(url)")
            XCTAssertNil(result.unusedParametersMap, "Unexpected `unusedParametersMap` in DeepLinkInfo for url \(url)")

            XCTAssertEqual(result.source, source, "Changed `source` in DeepLinkInfo for url \(url)")
        }
    }
}

// MARK: - Service Deeplinks

extension StaticURLDeepLinkInfoGeneratorTests {
    func testMosRuDeeplink() {
        let links = [
            "yandexrealty://callback/mosru?status=ok&task_id=41979a1e07c94b3f8301f5f6a54b3884"
        ]
        let urls = links.compactMap { URL(string: $0) }

        let generator = StaticURLDeepLinkInfoGenerator()

        urls.forEach { url in
            let source = DeepLinkSource(kind: .universalLink(url: url, referrerURL: nil))

            guard let result = generator.deepLinkInfo(forURL: url, source: source) else {
                XCTFail("Couldn't generate DeepLinkInfo for url \(url)")
                return
            }

            guard case .service(.mosRuAuth) = result.action else {
                XCTFail("Wrong `action` \(result.action) in DeepLinkInfo for url \(url)")
                return
            }

            XCTAssertNil(result.rawActionName, "Unexpected `rawActionName` in DeepLinkInfo for url \(url)")
            XCTAssertNil(result.unusedParametersMap, "Unexpected `unusedParametersMap` in DeepLinkInfo for url \(url)")

            XCTAssertEqual(result.source, source, "Changed `source` in DeepLinkInfo for url \(url)")
        }
    }

    func testBadServiceDeeplinks() {
        // All these cases might be tested separately, but I don't see a real profit
        let links = [
            "yandexrealty://notCallback/mosru",                     // bad "host"
            "yandexrealty://callback/random",                       // unknown source
            "yandexrealty://callback/mosru",                        // bad or empty payload
            "yandexrealty://callback/mosru?status=ok&task_id=",     // bad or empty payload
            "https://realty.yandex.ru/amp",                         // no route
        ]
        let urls = links.compactMap { URL(string: $0) }

        let generator = StaticURLDeepLinkInfoGenerator()

        urls.forEach { url in
            let source = DeepLinkSource(kind: .universalLink(url: url, referrerURL: nil))
            let result = generator.deepLinkInfo(forURL: url, source: source)

            XCTAssertNil(result, "Generated DeepLinkInfo for bad url \(url)")
        }
    }
}
