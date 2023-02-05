//
//  GetPhotoMemoriesTest.swift
//  Taverna-Unit-Tests
//
//  Created by Timur Turaev on 26.02.2022.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Taverna

internal final class GetPhotoMemoriesTest: XCTestCase {
    private var httpClient: HTTPClientProtocol!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))

        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)
    }

    func testParsingRegularResponse() throws {
        let request = GetPhotoMemoriesRequest(size: "123x456", locale: "en")

        let getMemoriesFuture = self.httpClient
            .runRequest(request, responseType: GetPhotoMemoriesResponse.self)
            .receiveOnMainQueue()
        let result = try self.waitFor(getMemoriesFuture, timeout: 1.0).get()

        let items = [
            PhotoMemoryDTO(id: "000000164578684880909540000001641938292083",
                           link: "/client/remember/000000164578684880909540000001641938292083",
                           imageUrl: "https://downloader.disk.yandex.ru/preview/...",
                           title: "Тунис и Россия",
                           subtitle: "2018"),
            PhotoMemoryDTO(id: "000000164570011145704600000001641938848871",
                           link: "/client/remember/000000164570011145704600000001641938848871",
                           imageUrl: "https://downloader.disk.yandex.ru/preview/...",
                           title: "Санкт-Петербург",
                           subtitle: "Неделя с 22 января 2018")
        ]

        XCTAssertEqual(result, GetPhotoMemoriesResponseDTO(empty: false,
                                                           allPhotosLink: "/client/photo",
                                                           galleryTailLink: "/client/photo",
                                                           items: items))
    }

    func testParsingEmptyResponse() throws {
        let request = GetPhotoMemoriesRequest(size: "123x456", locale: "en")

        let getMemoriesFuture = self.httpClient
            .runRequest(request, responseType: GetPhotoMemoriesResponse.self)
            .receiveOnMainQueue()
        let result = try self.waitFor(getMemoriesFuture, timeout: 1.0).get()

        XCTAssertEqual(result, GetPhotoMemoriesResponseDTO(empty: true,
                                                           allPhotosLink: nil,
                                                           galleryTailLink: nil,
                                                           items: nil))
    }
}
