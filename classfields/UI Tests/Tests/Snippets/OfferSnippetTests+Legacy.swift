//
//  OfferSnippetTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 08.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

// MARK: - Call button

extension OfferSnippetTests {
    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func disabled_testCallButtonEnabled() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .callButton(.enabled))
        self.relaunchApp(with: .snippetsTests)

        self.performCommonSteps(specificSteps: { snippet in
            snippet
                .callButtonLabelStarts(with: "Позвонить")
                .isCallButtonTappable()
                .compareWithScreenshot(identifier: "callButton.available.offer")
        })
    }
}

// MARK: - Favorites

extension OfferSnippetTests {
    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func disabled_testFavoritesAvailable() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        self.relaunchApp(with: .snippetsTests)

        self.performCommonSteps(specificSteps: { snippet in
            snippet
                .isFavoritesButtonTappable()
                .compareWithScreenshot(identifier: "notInFavorites.offer")
        })
    }

    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func disabled_testInFavorites() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        self.relaunchApp(with: .snippetsTests)

        self.performCommonSteps(specificSteps: { snippet in
            snippet
                .isFavoritesButtonTappable()
                .tapOnFavoritesButton()

            let favorites = FavoritesRequestPushPermissionsSteps()
            favorites.closeIfPresented()

            let addedToFavorites = AddedToFavoritesNotificationSteps()
            addedToFavorites.closeIfPresented()

            snippet
                .compareWithScreenshot(identifier: "inFavorites.offer")
        })
    }
}

// MARK: - Additional Actions

extension OfferSnippetTests {
    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func disabled_testAddNoteFromMainListing() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .additionalActions(.withoutUserNote))
        PersonalizationAPIStubConfigurator.setupUpsertNote(using: self.dynamicStubs)
        self.relaunchApp(with: .snippetsTests)

        self.performCommonSteps(specificSteps: { snippet in
            snippet
                .isUserNoteNotPresented()
                .tapOnAdditionalActionsButton()

            let additionalActions = SnippetAdditionalActionsSteps()
            additionalActions
                .isScreenPresented()
                .isButtonPresented(.addNote)
                .isButtonNotPresented(.editNote)
                .tapOnButton(.addNote)

            TextPickerSteps(name: "Редактирование заметки")
                .ensureScreenPresented()
                .tapOnFirstPreset()
                .tapOnDoneButton()
                .ensureScreenNotPresented()

            let favorites = FavoritesRequestPushPermissionsSteps()
            favorites.closeIfPresented()

            let addedToFavorites = AddedToFavoritesNotificationSteps()
            addedToFavorites.closeIfPresented()

            snippet
                .isUserNotePresented()
                .compareUserNoteWithScreenshot(identifier: "new")
        })
    }

    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func disabled_testEditNoteFromMainListing() {
        let changedNotesText = "Заодно проверим длинный текст, который должен обрезаться до одной строки"
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .additionalActions(.withUserNote))
        PersonalizationAPIStubConfigurator.setupUpsertNote(using: self.dynamicStubs)
        self.relaunchApp(with: .snippetsTests)

        self.performCommonSteps(specificSteps: { snippet in
            snippet
                .isUserNotePresented()
                .tapOnUserNote()

            TextPickerSteps(name: "Редактирование заметки")
                .ensureScreenPresented()
                .typeText(changedNotesText)
                .tapOnDoneButton()
                .ensureScreenNotPresented()

            snippet
                .isUserNotePresented()
                .compareUserNoteWithScreenshot(identifier: "changed")
        })
    }

    // FIXME: Move the screenshot check to Unit test (https://st.yandex-team.ru/VSAPPS-10667)
    func disabled_testPhoneCallWithError() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .noPhoto)
        SnippetsListAPIStubConfigurator.setupGetOfferNumbersError(using: self.dynamicStubs)

        self.relaunchApp(with: .snippetsTests)

        self.performCommonSteps(specificSteps: { snippet in
            snippet
                .isCallButtonTappable()
                .tapOnCallButton()

            CallSteps()
                .waitForTopNotificationViewExistence()

            self.compareWithScreenshot(identifier: "offerSnippet.phoneCall.withError", timeout: 0.0)
        })
    }
}

// MARK: - Common

extension OfferSnippetTests {
    private func performCommonSteps(specificSteps: (OfferSnippetSteps) -> Void) {
        let list = SearchResultsListSteps()
        let offerList = list
            .isScreenPresented()
            .withOfferList()

        let snippet = offerList
            .cell(withIndex: 0)
            .isPresented()

        specificSteps(snippet)
    }
}
