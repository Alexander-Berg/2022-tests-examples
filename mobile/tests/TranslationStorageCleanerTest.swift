//
//  TranslationStorageCleanerTest.swift
//  YandexMobileMailTests
//
//  Created by Nikita Ermolenko on 07/11/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
@testable import TranslationServices

internal final class TranslationStorageCleanerTest: XCTestCase {

    override func setUp() {
        super.setUp()
        TranslationTestHelpers.clearStorage()
    }

    override func tearDown() {
        super.tearDown()
        TranslationTestHelpers.clearStorage()
    }

    func testShouldClearFromRecentLanguagesByClearCache() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let language = TranslationLanguage(code: "en", name: "English")

        storage.add(recentLanguage: language)
        storage.clearCache()

        XCTAssertEqual(storage.recentLanguages, [])
        XCTAssertEqual(TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).recentLanguages, [])
    }

    func testShouldClearToRecentLanguagesByClearingCache() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationRecentToLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let language = TranslationLanguage(code: "en", name: "English")

        storage.add(recentLanguage: language)
        storage.clearCache()

        XCTAssertEqual(storage.recentLanguages, [])
        XCTAssertEqual(TranslationRecentToLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).recentLanguages, [])
    }

    func testShouldClearTranslatedMessagesByClearingCache() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let message = TranslatedMessage(mid: 1, fromLanguageCode: "en", languageCode: "en", bodies: [TranslatedMessage.Body(hid: "hid", content: "content")])

        storage.save(translatedMessage: message)
        storage.clearCache()

        XCTAssertNil(storage.translatedMessage(byMid: 1, languageCode: "en", fromLanguageCode: "en"))
        XCTAssertNil(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
            .translatedMessage(byMid: 1, languageCode: "en", fromLanguageCode: "en"))
    }
}
