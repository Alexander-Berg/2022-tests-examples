//
//  OfferSnippetScenarios.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 24.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class OfferSnippetScenarios {
    init(snippet: OfferSnippetSteps, dynamicStubs: HTTPDynamicStubs) {
        self.snippet = snippet
        self.dynamicStubs = dynamicStubs
    }

    // MARK: Private

    private let snippet: OfferSnippetSteps
    private let dynamicStubs: HTTPDynamicStubs

    private enum Consts {
        static let offerID: String = "157352957988240017"
    }
}

// MARK: - Calls

extension OfferSnippetScenarios {
    @discardableResult
    func runBillingOnCall(offerID: String = Consts.offerID) -> Self {
        let callScenarios = OfferCallScenarios(
            dynamicStubs: self.dynamicStubs,
            callButtonHandler: self.snippet,
            offerID: offerID
        )
        callScenarios.runBillingOnCall()

        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers(offerID: String = Consts.offerID) -> Self {
        let callScenarios = OfferCallScenarios(
            dynamicStubs: self.dynamicStubs,
            callButtonHandler: self.snippet,
            offerID: offerID
        )
        callScenarios.runCallWithTwoPhoneNumbers()

        return self
    }

    @discardableResult
    func runCallWithError(offerID: String = Consts.offerID) -> Self {
        let callScenarios = OfferCallScenarios(
            dynamicStubs: self.dynamicStubs,
            callButtonHandler: self.snippet,
            offerID: offerID
        )
        callScenarios.runCallWithError()

        return self
    }

    @discardableResult
    func runAddToCallHistory(offerID: String = Consts.offerID) -> Self {
        // To load a list of phones to call
        SnippetsListAPIStubConfigurator.setupGetOfferOnePhoneNumbers(using: self.dynamicStubs, offerID: offerID)
        // To load an item of Call History
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)

        let requestExpectation = XCTestExpectation(description: "Request of Call History list")
        OfferCardAPIStubConfiguration
            .setupOfferCardExpectation(requestExpectation, using: self.dynamicStubs)

        self.snippet
            .tapOnCallButton()

        // Go to Call History screen

        RootNavigationSteps()
            .tabBarTapOnComunicationItem()
        CommunicationSteps()
            .isScreenPresented()
            .tapOnCallHistorySegment()

        let result = XCTWaiter.yreWait(
            for: [requestExpectation],
            timeout: Constants.timeout
        )
        XCTAssertTrue(result, "Expectation has not been fulfilled")

        return self
    }

    @discardableResult
    func runAddToFavorites() -> Self {
        let addToFavoritesExpectation = XCTestExpectation(description: "Add an offer to Favorites")
        FavoritesAPIStubConfigurator.setupAddOfferExpectation(addToFavoritesExpectation, using: self.dynamicStubs)

        self.snippet
            .tapOnFavoritesButton()

        FavoritesRequestPushPermissionsSteps()
            .closeIfPresented()

        AddedToFavoritesNotificationSteps()
            .closeIfPresented()

        let addItemResult = XCTWaiter.yreWait(
            for: [addToFavoritesExpectation],
            timeout: Constants.timeout
        )
        XCTAssertTrue(addItemResult, "Expectation has not been fulfilled")

        // Go to Favorites screen

        let requestFavoriteItemsExpectation = XCTestExpectation(description: "Request of Favorite items")
        FavoritesAPIStubConfigurator
            .setupListExpectation(requestFavoriteItemsExpectation, using: self.dynamicStubs)

        let requestOfferExpectation = XCTestExpectation(description: "Request of an item from Favorites list")
        SnippetsListAPIStubConfigurator.setupListExpectation(
            requestOfferExpectation,
            using: self.dynamicStubs
        )

        RootNavigationSteps()
            .tabBarTapOnFavoriteItem()

        let requestItemResult = XCTWaiter.yreWait(
            for: [requestFavoriteItemsExpectation, requestOfferExpectation],
            timeout: Constants.timeout
        )
        XCTAssertTrue(requestItemResult, "Expectation has not been fulfilled")
        
        return self
    }
}

// MARK: - Notes

extension OfferSnippetScenarios {
    @discardableResult
    func runAddNote(offerID: String = Consts.offerID) -> Self {
        PersonalizationAPIStubConfigurator.setupUpsertNote(using: self.dynamicStubs, offerID: offerID)
        
        self.snippet
            .isUserNoteNotPresented()
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .isButtonPresented(.addNote)
            .isButtonNotPresented(.editNote)
            .tapOnButton(.addNote)

        let textPicker = TextPickerSteps(name: "Создание заметки")
        textPicker
            .ensureScreenPresented()
            .tapOnFirstPreset()
            .tapOnDoneButton()

        FavoritesRequestPushPermissionsSteps()
            .closeIfPresented()

        AddedToFavoritesNotificationSteps()
            .closeIfPresented()

        // This step is separated in attempt to fix the flacky runs on CI
        textPicker
            .ensureScreenNotPresented()

        self.snippet
            .isUserNotePresented()

        return self
    }

    @discardableResult
    func runEditNote() -> Self {
        let changedNoteText = "Заодно проверим длинный текст, который должен обрезаться до одной строки"

        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .isButtonNotPresented(.addNote)
            .isButtonPresented(.editNote)
            .tapOnButton(.cancel)

        self.snippet
            .isUserNotePresented()
            .tapOnUserNote()

        TextPickerSteps(name: "Редактирование заметки")
            .ensureScreenPresented()
            .typeText(changedNoteText)
            .tapOnDoneButton()
            .ensureScreenNotPresented()

        self.snippet
            .hasUserNote(withText: changedNoteText)

        return self
    }
}

// MARK: - Additional Actions

extension OfferSnippetScenarios {
    @discardableResult
    func runAbuse() -> Self {
        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .isButtonPresented(.offerAbuse)
            .tapOnButton(.offerAbuse)

        AbuseSteps()
            .isOfferAbuseScreenPresented()
            .tapOnCloseButton()
            .isOfferAbuseScreenNotPresented()

        return self
    }

    @discardableResult
    func runSharing(offerID: String = Consts.offerID) -> Self {
        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .isButtonPresented(.share)
            .isButtonPresented(.copyLink)
            .tapOnButton(.copyLink)

        if #available(iOS 14.5, *) {
            // FIXME VSAPPS-9889: figure out how to compare pasteboard contents
        }
        else {
            // swiftlint:disable:next force_unwrapping
            let url = URL(string: "https://realty.yandex.ru/offer/\(offerID)")!
            DeviceSteps()
                .isPasteboardEquals(to: url)
        }

        // We don't check tap on 'Share' button here because we display a system component, OS-specific

        return self
    }

    @discardableResult
    func runNoSharing() -> Self {
        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .isButtonNotPresented(.share)
            .isButtonNotPresented(.copyLink)

        return self
    }
}

// MARK: - Hide Offer

extension OfferSnippetScenarios {
    @discardableResult
    func runHideOffer(
        using list: OfferListSteps,
        offerID: String = Consts.offerID
    ) -> Self {
        PersonalizationAPIStubConfigurator.setupHideOffer(using: self.dynamicStubs, offerID: offerID)

        list
            .isListNonEmpty()

        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .tapOnButton(.hide)
            .isScreenNotPresented()
            .confirmHideAction()

        list
            .isListEmpty()
            // Ensure list has not been dismissed on emptiness
            .isScreenPresented()

        return self
    }

    @discardableResult
    func runHideSimilarOffer(
        using card: OfferCardSteps,
        offerID: String = Consts.offerID
    ) -> Self {
        PersonalizationAPIStubConfigurator.setupHideOffer(using: self.dynamicStubs, offerID: offerID)

        card
            .isSimilarOffersListNonEmpty()

        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .tapOnButton(.hide)
            .isScreenNotPresented()
            .confirmHideAction()

        card
            .isSimilarOffersListEmpty()

        return self
    }

    @discardableResult
    func runNoHideOfferFeature() -> Self {
        self.snippet
            .tapOnAdditionalActionsButton()

        SnippetAdditionalActionsSteps()
            .isScreenPresented()
            .isButtonNotPresented(.hide)
            .tapOnButton(.cancel)

        return self
    }
}
