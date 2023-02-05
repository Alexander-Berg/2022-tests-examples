//
//  TranslationStorageTest.swift
//  YandexMobileMailTests
//
//  Created by Nikita Ermolenko on 05/11/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import Utils
@testable import TranslationServices

internal final class TranslationStorageTest: XCTestCase {

    override func setUp() {
        super.setUp()
        TranslationTestHelpers.clearStorage()
    }

    override func tearDown() {
        super.tearDown()
        TranslationTestHelpers.clearStorage()
    }

    // MARK: - All

    func testShouldSaveAllAvailableLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let languages = [
            TranslationLanguage(code: "ru", name: "Russia"),
            TranslationLanguage(code: "tr", name: "Turkish")
        ]

        storage.save(languages: languages)

        XCTAssertEqual(languages, storage.allAvailableLanguages)
        XCTAssertEqual(languages,
                       TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).allAvailableLanguages)
    }

    // MARK: - Ignored

    func testShouldSaveIgnoredLanguage() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let ignoredLanguage = TranslationLanguage(code: "ru", name: "Russia")

        storage.save(languages: [ignoredLanguage])
        storage.add(ignoredLanguage: ignoredLanguage)

        XCTAssertEqual(storage.ignoredLanguages.count, 1)
        XCTAssertEqual(storage.ignoredLanguages.first, ignoredLanguage)
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).ignoredLanguages.count,
                       1)
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).ignoredLanguages.first,
                       ignoredLanguage)
    }

    func testShouldSaveUniqueIgnoredLanguage() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let ignoredLanguage = TranslationLanguage(code: "ru", name: "Russia")

        storage.save(languages: [ignoredLanguage])
        storage.add(ignoredLanguage: ignoredLanguage)
        storage.add(ignoredLanguage: ignoredLanguage)

        XCTAssertEqual(storage.ignoredLanguages.count, 1)
        XCTAssertEqual(storage.ignoredLanguages.first, ignoredLanguage)
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).ignoredLanguages.count,
                       1)
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).ignoredLanguages.first,
                       ignoredLanguage)
    }

    func testShouldRemoveIgnoredLanguage() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let ignoredLanguage = TranslationLanguage(code: "ru", name: "Russia")

        storage.add(ignoredLanguage: ignoredLanguage)
        storage.remove(ignoredLanguage: ignoredLanguage)

        XCTAssertEqual(storage.ignoredLanguages, [])
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).ignoredLanguages,
                       [])
    }

    func testShouldFilterIgnoredLanguagesAmongAllAvailableLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let languages = [
            TranslationLanguage(code: "ru", name: "Russia"),
            TranslationLanguage(code: "tr", name: "Turkish")
        ]
        let ignoredLanguages = [
            TranslationLanguage(code: "ru", name: "Russia"),
            TranslationLanguage(code: "en", name: "English")
        ]

        storage.save(languages: languages)
        storage.save(ignoredLanguages: ignoredLanguages)

        XCTAssertEqual(storage.ignoredLanguages, [TranslationLanguage(code: "ru", name: "Russia")])
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).ignoredLanguages,
                       [TranslationLanguage(code: "ru", name: "Russia")])
    }

    // MARK: - Recent

    func testShouldSaveOnlyFiveIgnoredLanguageInCorrectOrderForFromLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        _testShouldSaveOnlyFiveIgnoredLanguageInCorrectOrder(storage: storage)
    }

    func testShouldSaveOnlyFiveIgnoredLanguageInCorrectOrderForToLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationRecentToLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        _testShouldSaveOnlyFiveIgnoredLanguageInCorrectOrder(storage: storage)
    }

    private func _testShouldSaveOnlyFiveIgnoredLanguageInCorrectOrder(storage: TranslationRecentLanguagesStorageProtocol) {
        storage.save(languages: [
            TranslationLanguage(code: "ru1", name: "Russia1"),
            TranslationLanguage(code: "ru2", name: "Russia2"),
            TranslationLanguage(code: "ru3", name: "Russia3"),
            TranslationLanguage(code: "ru4", name: "Russia4"),
            TranslationLanguage(code: "ru5", name: "Russia5"),
            TranslationLanguage(code: "ru6", name: "Russia6")
        ])

        storage.save(recentLanguages: [
            TranslationLanguage(code: "ru1", name: "Russia1"),
            TranslationLanguage(code: "ru2", name: "Russia2"),
            TranslationLanguage(code: "ru3", name: "Russia3"),
            TranslationLanguage(code: "ru4", name: "Russia4"),
            TranslationLanguage(code: "ru5", name: "Russia5")
        ])

        XCTAssertEqual(storage.recentLanguages, [
            TranslationLanguage(code: "ru5", name: "Russia5"),
            TranslationLanguage(code: "ru4", name: "Russia4"),
            TranslationLanguage(code: "ru3", name: "Russia3"),
            TranslationLanguage(code: "ru2", name: "Russia2"),
            TranslationLanguage(code: "ru1", name: "Russia1")
        ])

        storage.add(recentLanguage: TranslationLanguage(code: "ru6", name: "Russia6"))

        XCTAssertEqual(storage.recentLanguages, [
            TranslationLanguage(code: "ru6", name: "Russia6"),
            TranslationLanguage(code: "ru5", name: "Russia5"),
            TranslationLanguage(code: "ru4", name: "Russia4"),
            TranslationLanguage(code: "ru3", name: "Russia3"),
            TranslationLanguage(code: "ru2", name: "Russia2")
        ])
    }

    func testShouldHaveSeparatedStorageForFromAndToRecentLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let fromStorage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let toStorage = TranslationRecentToLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let fromLanguage = TranslationLanguage(code: "ru", name: "Russia")
        let toLanguage = TranslationLanguage(code: "en", name: "English")

        fromStorage.save(languages: [fromLanguage, toLanguage])
        toStorage.save(languages: [fromLanguage, toLanguage])
        fromStorage.add(recentLanguage: fromLanguage)
        toStorage.add(recentLanguage: toLanguage)

        XCTAssertEqual(fromStorage.recentLanguages, [fromLanguage])
        XCTAssertEqual(toStorage.recentLanguages, [toLanguage])
        XCTAssertEqual(TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).recentLanguages,
                       [fromLanguage])
        XCTAssertEqual(TranslationRecentToLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).recentLanguages,
                       [toLanguage])
    }

    func testShouldFilterRecentToLanguagesAmongAllAvailableLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationRecentToLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        _testShouldFilterRecentLanguagesAmongAllAvailableLanguages(storage: storage)
    }

    func testShouldFilterRecentFromLanguagesAmongAllAvailableLanguages() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        _testShouldFilterRecentLanguagesAmongAllAvailableLanguages(storage: storage)
    }

    private func _testShouldFilterRecentLanguagesAmongAllAvailableLanguages(storage: TranslationRecentLanguagesStorageProtocol) {
        let languages = [
            TranslationLanguage(code: "ru", name: "Russia"),
            TranslationLanguage(code: "tr", name: "Turkish")
        ]
        let recentLanguages = [
            TranslationLanguage(code: "ru", name: "Russia"),
            TranslationLanguage(code: "en", name: "English")
        ]

        storage.save(languages: languages)
        storage.save(recentLanguages: recentLanguages)

        XCTAssertEqual(storage.recentLanguages, [TranslationLanguage(code: "ru", name: "Russia")])
    }

    // MARK: - Translated messages

    func testShouldSaveTranslatedMessage() {
        let cacheFolderURL = TranslationTestHelpers.generateMessagesCacheFolderNameURL
        let storage = TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL)
        let message = TranslatedMessage(mid: 1, fromLanguageCode: "al", languageCode: "en", bodies: [TranslatedMessage.Body(hid: "hid", content: "content")])

        storage.save(translatedMessage: message)
        RunLoop.current.run(until: Date().addingTimeInterval(0.1))

        XCTAssertNil(storage.translatedMessage(byMid: 1, languageCode: "ru", fromLanguageCode: "br"))
        XCTAssertNil(storage.translatedMessage(byMid: 1, languageCode: "en", fromLanguageCode: "br"))
        XCTAssertNil(storage.translatedMessage(byMid: 2, languageCode: "en", fromLanguageCode: "al"))
        XCTAssertNil(storage.translatedMessage(byMid: 2, languageCode: "en", fromLanguageCode: "br"))
        XCTAssertEqual(storage.translatedMessage(byMid: 1, languageCode: "en", fromLanguageCode: "al"), message)
        XCTAssertEqual(TranslationGeneralStorage(translatedMessagesCacheFolderNameURL: cacheFolderURL).translatedMessage(byMid: 1, languageCode: "en", fromLanguageCode: "al"),
                       message)
    }
}

// MARK: - Helpers

private extension TranslationGeneralStorage {
    func save(ignoredLanguages: [TranslationLanguage]) {
        ignoredLanguages.forEach(self.add)
    }
}

extension TranslationRecentLanguagesStorageProtocol {
    func save(recentLanguages: [TranslationLanguage]) {
        recentLanguages.forEach(self.add(recentLanguage:))
    }
}
