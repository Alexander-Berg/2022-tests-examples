//
//  FiltersSubtests+tags.swift
//  UI Tests
//
//  Created by Fedor Solovev on 16.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

extension FiltersSubtests {
    func tagsToInclude() {
        let runKey = #function
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки 'Искать в описании объявления'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isTagsToIncludeCellPresented()
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        self.testTags(searchType: .include)
    }

    func tagsToExclude() {
        let runKey = #function
        guard Self.subtestsExecutionMarks[runKey] != true else {
            let activityName = "Упрощенная проверка - проверяем наличие ячейки 'Не показывать объявления, если в описании'"
            XCTContext.runActivity(named: activityName) { _ -> Void in
                self.filtersSteps.isTagsToIncludeCellPresented()
            }
            return
        }
        Self.subtestsExecutionMarks[runKey] = true

        self.testTags(searchType: .exclude)
    }

    private struct Consts {
        static let includedTagKey = "includeTag"
        static let excludedTagKey = "excludeTag"
    }

    private struct TagItem: Hashable {
        let id: String
        let title: String
    }

    private enum SearchType {
        case include
        case exclude

        var urlQueryItemName: String {
            switch self {
                case .include:
                    return Consts.includedTagKey
                case .exclude:
                    return Consts.excludedTagKey
            }
        }
    }

    private var tags: [TagItem] {
        return [
            TagItem(id: "1700312", title: "в стиле лофт"),
            TagItem(id: "1700320", title: "двухуровневые квартиры"),
            TagItem(id: "1700346", title: "с панорамными окнами"),
        ]
    }

    private func testTags(searchType: SearchType) {
        let stub = FiltersAPIStubConfigurator(dynamicStubs: self.api.dynamicStubs, anyOfferKind: .offer)
        stub.setupTagSuggestList()

        let filterTagSteps: FiltersTagsSteps
        switch searchType {
            case .include:
                filterTagSteps = self.filtersSteps.isTagsToIncludeCellPresented()
            case .exclude:
                filterTagSteps = self.filtersSteps.isTagsToExcludeCellPresented()
        }

        filterTagSteps.resetButtonNotExists()

        let tagsPickerSteps = FiltersTagsPickerSteps()
        var selectedTags = Set<TagItem>()

        XCTContext.runActivity(named: "Проверка кнопки 'Добавить' на списке тэгов") { _ -> Void in
            for tag in self.tags {
                selectedTags.insert(tag)

                let expectation = self.setupSearch(selectedTags: selectedTags, searchType: searchType)

                filterTagSteps
                    .tapOnAddMoreButton()

                let filtersTagsPickerSteps = FiltersTagsPickerSteps()
                filtersTagsPickerSteps
                    .isTagsPickerPresented()
                    .isSuggestListNotEmpty()
                    .selectSuggest(by: tag.title)

                // @l-saveliy: Cell became bigger, we need sure that all cell still present
                switch searchType {
                    case .include:
                        self.filtersSteps.isTagsToIncludeCellPresented()
                    case .exclude:
                        self.filtersSteps.isTagsToExcludeCellPresented()
                }

                for selectedTag in selectedTags {
                    filterTagSteps.ensureBubbleExists(title: selectedTag.title)
                }
                self.waitForExpectation(expectation)
            }
        }

        XCTContext.runActivity(named: "Проверка кнопки 'назад' на экране саджестов") { _ -> Void in
            filterTagSteps.tapOnAddMoreButton()
            tagsPickerSteps.close()
        }

        XCTContext.runActivity(named: "Проверка удаления выбранных тегов") { _ -> Void in
            let tag = selectedTags.remove(at: selectedTags.startIndex)
            let removeOneBubbleExpectation = self.setupSearch(selectedTags: selectedTags, searchType: searchType)

            filterTagSteps.removeBubble(title: tag.title)
            self.waitForExpectation(removeOneBubbleExpectation)

            for selectedTag in selectedTags {
                filterTagSteps.ensureBubbleExists(title: selectedTag.title)
            }
            filterTagSteps.ensureBubbleNotExists(title: tag.title)

            let removeAllBubblesExpectation = self.setupSearch(selectedTags: [], searchType: searchType)

            filterTagSteps
                .resetButtonExists()
                .tapOnResetButton()

            self.waitForExpectation(removeAllBubblesExpectation)
            for selectedTag in selectedTags {
                filterTagSteps.ensureBubbleNotExists(title: selectedTag.title)
            }
        }
    }

    private func setupSearch(selectedTags: Set<TagItem>, searchType: SearchType) -> XCTestExpectation {
        let expectation = XCTestExpectation(description: "Проверка формирования запроса для выбранных тегов")
        let queryItems = selectedTags.map { URLQueryItem(name: searchType.urlQueryItemName, value: $0.id) }

        let predicate: Predicate<HttpRequest>
        if queryItems.isEmpty {
            predicate = Predicate<HttpRequest>.notContains(queryKey: searchType.urlQueryItemName)
        }
        else {
            predicate = Predicate<HttpRequest>.queryItems(contain: Set(queryItems))
        }

        self.api.setupSearchCounter(predicate: predicate) {
            expectation.fulfill()
        }

        return expectation
    }

    private func waitForExpectation(_ expectation: XCTestExpectation) {
        let expectationResult = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(expectationResult)
    }
}
