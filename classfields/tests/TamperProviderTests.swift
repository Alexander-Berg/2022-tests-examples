//
//  YREHttpTamperProviderTests.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 12.05.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

import protocol YRECoreUtils.TamperProviderProtocol
import class YRECoreUtils.TamperProvider
import class YRECoreUtils.MD5Hasher

final class TamperProviderTests: XCTestCase {
    private var tamperProvider: TamperProviderProtocol?

    override func setUp() {
        super.setUp()

        let md5hasher = MD5Hasher()

        let salt = "}S*#~NkBVAWq5QdrP6Kq"
        self.tamperProvider = TamperProvider(hasher: md5hasher, salt: salt, metricaDeviceIDFallback: "nobody")
    }

    override func tearDown() {
        super.tearDown()

        self.tamperProvider = nil
    }

    func testTamperParameterBasedOnGeneratedUUID() {
        // given
        let uuidString = "ce194fab-7969-492e-8fec-599607a67b8e"
        let generatedUUID = UUID(uuidString: uuidString)

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnGeneratedUUID(generatedUUID)

        // then
        XCTAssertEqual(tamperParameter, "05eb3c23fbe8eed7ba20c67c38200ba1", "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems() {
        // given
        let uuidString = Self.uuidString
        var queryItems: [URLQueryItem] = []
        queryItems.append(URLQueryItem(name: "category", value: "HOUSE"))
        queryItems.append(URLQueryItem(name: "countOnly", value: "YES"))
        queryItems.append(URLQueryItem(name: "objectType", value: "OFFER"))
        queryItems.append(URLQueryItem(name: "page", value: "0"))
        queryItems.append(URLQueryItem(name: "priceMax", value: "11000000"))
        queryItems.append(URLQueryItem(name: "priceType", value: "PER_OFFER"))
        queryItems.append(URLQueryItem(name: "rgid", value: "741964"))
        queryItems.append(URLQueryItem(name: "showOnMobile", value: "YES"))
        queryItems.append(URLQueryItem(name: "showSimilar", value: "NO"))
        queryItems.append(URLQueryItem(name: "sort", value: "RELEVANCE"))
        queryItems.append(URLQueryItem(name: "type", value: "SELL"))

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameter, "d5f9a1ee6ac6a89804dc22357666e9c3", "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenLowerAndUpperCase() {
        // given
        let uuidString = Self.uuidString
        var queryItemsLower: [URLQueryItem] = []
        queryItemsLower.append(URLQueryItem(name: "category", value: "house"))
        queryItemsLower.append(URLQueryItem(name: "type", value: "sell"))

        var queryItemsUpper: [URLQueryItem] = []
        queryItemsUpper.append(URLQueryItem(name: "CATEGORY", value: "HOUSE"))
        queryItemsUpper.append(URLQueryItem(name: "TYPE", value: "SELL"))

        // when
        let tamperParameterLower = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString,
                                                                                         urlQueryItems: queryItemsLower)
        let tamperParameterHigher = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString,
                                                                                          urlQueryItems: queryItemsUpper)

        // then
        XCTAssertEqual(tamperParameterLower, tamperParameterHigher, "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenCalledTwice() {
        // given
        let uuidString = Self.uuidString
        var queryItems: [URLQueryItem] = []
        queryItems.append(URLQueryItem(name: "category", value: "house"))
        queryItems.append(URLQueryItem(name: "type", value: "sell"))

        // when
        let tamperParameterFirst = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)
        let tamperParameterSecond = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameterFirst, tamperParameterSecond, "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenDifferentOrder() {
        // given
        let uuidString = Self.uuidString
        var queryItemsFirst: [URLQueryItem] = []
        queryItemsFirst.append(URLQueryItem(name: "category", value: "house"))
        queryItemsFirst.append(URLQueryItem(name: "type", value: "sell"))
        queryItemsFirst.append(URLQueryItem(name: "priceMax", value: "11000000"))

        var queryItemsSecond: [URLQueryItem] = []
        queryItemsSecond.append(URLQueryItem(name: "type", value: "sell"))
        queryItemsSecond.append(URLQueryItem(name: "priceMax", value: "11000000"))
        queryItemsSecond.append(URLQueryItem(name: "category", value: "house"))

        // when
        let tamperParameterFirst = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString,
                                                                                         urlQueryItems: queryItemsFirst)
        let tamperParameterSecond = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString,
                                                                                          urlQueryItems: queryItemsSecond)

        // then
        XCTAssertEqual(tamperParameterFirst, tamperParameterSecond, "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenQueryItemsIsNil() {
        // given
        let uuidString = Self.uuidString
        let queryItems: [URLQueryItem]? = nil

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameter, "e1932a8c6103d10d921a50031c817a20", "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenQueryItemsIsEmpty() {
        // given
        let uuidString = Self.uuidString
        let queryItems: [URLQueryItem] = []

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameter, "e1932a8c6103d10d921a50031c817a20", "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenValueIsNilAndKeyIsEmpty() {
        // given
        let uuidString = Self.uuidString
        var queryItems: [URLQueryItem] = []
        queryItems.append(URLQueryItem(name: "type", value: nil))
        queryItems.append(URLQueryItem(name: "", value: "123"))

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameter, "7b95ab35b8a35b94cce59f8b62ae90c2", "tamperParameters are not equal")
    }

    // https://st.yandex-team.ru/VSAPPS-8499
    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenKeyHasManyValuesAsSingleVariable() {
        // given
        let uuidString = "27f6d1d2ce244a5c941d44fe6bdace02"
        let url = URL(
            string: "http://api.realty.test.vertis.yandex.net/2.0/user/me/offers?category=APARTMENT%2CROOMS%2CHOUSE%2CLOT&direction=asc&includeFeature=STATS&includeFeature=PRODUCTS&page=0&pageSize=30&showStatusV2=UNPUBLISHED%2CBANNED%2CPUBLISHED%2CMODERATION&sort=weight"
        )
        let urlComponents = url.flatMap { URLComponents(url: $0, resolvingAgainstBaseURL: false) }
        let queryItems = urlComponents?.queryItems

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameter, "731b232490255e68d1c57aa5dcb0a174", "tamperParameters are not equal")
    }

    func testTamperParameterBasedOnDeviceUUIDAndURLQueryItems_WhenKeyHasManyValuesAsMultipleVariables() {
        // given
        let uuidString = "27f6d1d2ce244a5c941d44fe6bdace02"
        let url = URL(
            string: "http://api.realty.test.vertis.yandex.net/2.0/user/me/offers?category=APARTMENT&category=ROOMS&category=HOUSE&category=LOT"
        )
        let urlComponents = url.flatMap { URLComponents(url: $0, resolvingAgainstBaseURL: false) }
        let queryItems = urlComponents?.queryItems

        // when
        let tamperParameter = self.tamperProvider?.tamperParameterBasedOnDeviceUUID(uuidString, urlQueryItems: queryItems)

        // then
        XCTAssertEqual(tamperParameter, "ba59203922e09e9a4dc1960bbf756787", "tamperParameters are not equal")
    }

    private static let uuidString = "a308ae2137ac4bdf90fa9e3c39174257"
}
