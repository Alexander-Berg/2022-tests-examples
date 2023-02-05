//
//  PhotoMemoriesLoaderTest.swift
//  Taverna-Unit-Tests
//
//  Created by Timur Turaev on 26.02.2022.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Taverna

internal final class PhotoMemoriesLoaderTest: XCTestCase {
    private var loader: PhotoMemoriesLoading!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))

        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)
        self.loader = PhotoMemoriesLoader(httpClient: httpClient, locale: "en")
    }

    func testParsingRegularResponse() throws {
        let memories = [
            PhotoMemoryModel(id: "000000164578684880909540000001641938292083",
                             link: "/client/remember/000000164578684880909540000001641938292083",
                             imageUrl: "https://downloader.disk.yandex.ru/preview/...",
                             title: "Тунис и Россия",
                             subtitle: "2018"),
            PhotoMemoryModel(id: "000000164570011145704600000001641938848871",
                             link: "/client/remember/000000164570011145704600000001641938848871",
                             imageUrl: "https://downloader.disk.yandex.ru/preview/...",
                             title: "Санкт-Петербург",
                             subtitle: "Неделя с 22 января 2018")
        ]
        let expectedModel = PhotoMemoriesModel(allPhotosLink: "/client/photo",
                                               galleryTailLink: "/client/photo",
                                               memories: memories)

        let exp = self.expectation(description: #function)
        self.loader.loadPhotoMemories { result in
            let model = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(model, .exist(model: expectedModel))
            exp.fulfill()
        }

        self.wait(for: [exp], timeout: 1.0)
    }

    func testParsingEmptyResponse() throws {
        let exp = self.expectation(description: #function)
        self.loader.loadPhotoMemories { result in
            let model = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(model, .empty)
            exp.fulfill()
        }

        self.wait(for: [exp], timeout: 1.0)
    }
}
