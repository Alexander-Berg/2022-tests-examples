//
//  TranslatePresenterTest.swift
//  YandexMobileMailTests
//
//  Created by Nikita Ermolenko on 06/11/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import Utils
@testable import TranslationServices
@testable import Translator

internal final class TranslatePresenterTest: XCTestCase {

    private static var cacheFolderNameURL: URL = {
        let url = try! FileManager.default
            .url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent("Messages", isDirectory: true)

        try! FileManager.default.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)

        return url
    }()

    override class func setUp() {
        TranslationStorageFactory.initializeShared(translatedMessagesCacheFolderNameURL: self.cacheFolderNameURL)
        TranslationStorageCleaner.clearCache()
    }

    func testShouldGeneratePropsInLocalizedOrderWithCorrectSelectedLanguageWhenDefaultAndSelectedLanguageExist() {
        let title = "title"
        let data = TranslatePresenter.Data(title: title,
                                           defaultLanguageSubtitle: "system",
                                           defaultLanguageCode: "en",
                                           selectedLanguageCode: "al")
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: Self.cacheFolderNameURL)
        storage.add(recentLanguage: TranslationLanguage(code: "ru", name: "Russian"))
        storage.save(languages: [
            TranslationLanguage(code: "en", name: "English"),
            TranslationLanguage(code: "ru", name: "Russian"),
            TranslationLanguage(code: "al", name: "Albanian")
        ])

        let renderer = Renderer()
        let presenter = TranslatePresenter(data: data, translationStorage: storage)
        presenter.renderer = renderer

        presenter.start()

        let resultedProps = renderer.props!
        XCTAssertEqual(resultedProps.title, title)
        XCTAssertEqual(resultedProps.recentLanguagesProps, [
            TranslateCellProps(title: "English", subtitle: "system", isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty)
        ])
        XCTAssertEqual(resultedProps.allLanguagesProps, [
            TranslateCellProps(title: "Albanian", subtitle: nil, isSelected: true, onSelect: .empty),
            TranslateCellProps(title: "English", subtitle: nil, isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian", subtitle: nil, isSelected: false, onSelect: .empty)
        ])
    }

    func testShouldGenerateUpToFiveRecentPropsIncludingSystemProps() {
        let title = "title"
        let data = TranslatePresenter.Data(title: title,
                                           defaultLanguageSubtitle: "system",
                                           defaultLanguageCode: "en",
                                           selectedLanguageCode: "al")
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: Self.cacheFolderNameURL)
        storage.add(recentLanguage: TranslationLanguage(code: "ru", name: "Russian"))
        storage.add(recentLanguage: TranslationLanguage(code: "ru1", name: "Russian1"))
        storage.add(recentLanguage: TranslationLanguage(code: "ru2", name: "Russian2"))
        storage.add(recentLanguage: TranslationLanguage(code: "ru3", name: "Russian3"))
        storage.add(recentLanguage: TranslationLanguage(code: "ru4", name: "Russian4"))
        
        storage.save(languages: [
            TranslationLanguage(code: "en", name: "English"),
            TranslationLanguage(code: "ru", name: "Russian"),
            TranslationLanguage(code: "ru1", name: "Russian1"),
            TranslationLanguage(code: "ru2", name: "Russian2"),
            TranslationLanguage(code: "ru3", name: "Russian3"),
            TranslationLanguage(code: "ru4", name: "Russian4")
        ])

        let renderer = Renderer()
        let presenter = TranslatePresenter(data: data, translationStorage: storage)
        presenter.renderer = renderer

        presenter.start()

        let resultedProps = renderer.props!
        XCTAssertEqual(resultedProps.recentLanguagesProps, [
            TranslateCellProps(title: "English", subtitle: "system", isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian4", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian3", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian2", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian1", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty)
        ])
    }

    func testShouldGeneratePropsWithoutDefaultButWithSelected() {
        let title = "title"
        let data = TranslatePresenter.Data(title: title,
                                           defaultLanguageSubtitle: "system",
                                           defaultLanguageCode: nil,
                                           selectedLanguageCode: "al")
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: Self.cacheFolderNameURL)
        storage.add(recentLanguage: TranslationLanguage(code: "ru", name: "Russian"))
        storage.save(languages: [
            TranslationLanguage(code: "en", name: "English"),
            TranslationLanguage(code: "ru", name: "Russian"),
            TranslationLanguage(code: "al", name: "Albanian")
        ])

        let renderer = Renderer()
        let presenter = TranslatePresenter(data: data, translationStorage: storage)
        presenter.renderer = renderer

        presenter.start()

        let resultedProps = renderer.props!
        XCTAssertEqual(resultedProps.title, title)
        XCTAssertEqual(resultedProps.recentLanguagesProps, [
            TranslateCellProps(title: "Russian", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty)
        ])
        XCTAssertEqual(resultedProps.allLanguagesProps, [
            TranslateCellProps(title: "Albanian", subtitle: nil, isSelected: true, onSelect: .empty),
            TranslateCellProps(title: "English", subtitle: nil, isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian", subtitle: nil, isSelected: false, onSelect: .empty)
        ])
    }

    func testShouldGeneratePropsWithoutSelectedButWithDefault() {
        let title = "title"
        let data = TranslatePresenter.Data(title: title,
                                           defaultLanguageSubtitle: "system",
                                           defaultLanguageCode: "en",
                                           selectedLanguageCode: nil)
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: Self.cacheFolderNameURL)
        storage.add(recentLanguage: TranslationLanguage(code: "ru", name: "Russian"))
        storage.save(languages: [
            TranslationLanguage(code: "en", name: "English"),
            TranslationLanguage(code: "ru", name: "Russian"),
            TranslationLanguage(code: "al", name: "Albanian")
        ])

        let renderer = Renderer()
        let presenter = TranslatePresenter(data: data, translationStorage: storage)
        presenter.renderer = renderer

        presenter.start()

        let resultedProps = renderer.props!
        XCTAssertEqual(resultedProps.title, title)
        XCTAssertEqual(resultedProps.recentLanguagesProps, [
            TranslateCellProps(title: "English", subtitle: "system", isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty)
        ])
        XCTAssertEqual(resultedProps.allLanguagesProps, [
            TranslateCellProps(title: "Albanian", subtitle: nil, isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "English", subtitle: nil, isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Russian", subtitle: nil, isSelected: false, onSelect: .empty)
        ])
    }

    func testShouldFilterOnlyAvailablePropsBySearchNotRecent() {
        let title = "title"
        let data = TranslatePresenter.Data(title: title,
                                           defaultLanguageSubtitle: "system",
                                           defaultLanguageCode: "en",
                                           selectedLanguageCode: "al")
        let storage = TranslationRecentFromLanguagesStorage(translatedMessagesCacheFolderNameURL: Self.cacheFolderNameURL)
        storage.add(recentLanguage: TranslationLanguage(code: "br", name: "Brasilian"))
        storage.save(languages: [
            TranslationLanguage(code: "en", name: "English"),
            TranslationLanguage(code: "ru", name: "Russian"),
            TranslationLanguage(code: "al", name: "Albanian"),
            TranslationLanguage(code: "br", name: "Brasilian")
        ])

        let renderer = Renderer()
        let presenter = TranslatePresenter(data: data, translationStorage: storage)
        presenter.renderer = renderer

        presenter.start()
        renderer.search = "Russ"
        RunLoop.current.run(until: Date().addingTimeInterval(0.1))

        let resultedProps = renderer.props!
        XCTAssertEqual(resultedProps.title, title)
        XCTAssertEqual(resultedProps.recentLanguagesProps, [
            TranslateCellProps(title: "English", subtitle: "system", isSelected: false, onSelect: .empty),
            TranslateCellProps(title: "Brasilian", subtitle: "translation.choose-language.recent-language".localizedString(), isSelected: false, onSelect: .empty)
        ])
        XCTAssertEqual(resultedProps.allLanguagesProps, [
            TranslateCellProps(title: "Russian", subtitle: nil, isSelected: false, onSelect: .empty)
        ])
    }
}

// MARK: - Mock

private extension CommandWith {
    static var empty: CommandWith<(IndexPath, TranslateCellProps)> {
        return .init(action: { _, _ in })
    }
}

private class Renderer: TranslateViewPropsRendering {
    var props: TranslateViewProps!
    var search: String = "" {
        didSet {
            self.props.onSearch(self.search)
        }
    }
    func render(props: TranslateViewProps) {
        self.props = props
    }
}
