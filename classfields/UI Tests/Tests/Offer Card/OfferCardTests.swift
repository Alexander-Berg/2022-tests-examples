//
//  OfferCardTests.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 10/19/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class OfferCardTests: BaseTestCase {}

// MARK: - Card

extension OfferCardTests {
    func testCallsFromCard() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListBilling(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardBilling(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            let callScenarios = OfferCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: cardSteps
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }

    // https://st.yandex-team.ru/VSAPPS-8218#602d0fd4f38a0d7a84b68f79
    func disabled_testPhotoFallbackWhenNoLargeImages() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupNoLargeImages(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .compareSnapshot(identifier: "photoFallback", block: .gallery)
        }
    }

    func testHideOffer() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)
        PersonalizationAPIStubConfigurator.setupHideOffer(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isOfferCardPresented()
                .isCallButtonTappable()
                .tapOnAdditionalActions()

            let additionalActions = SnippetAdditionalActionsSteps()
            additionalActions
                .isScreenPresented()
                .tapOnButton(.hide)
                .confirmHideAction()

            let list = SearchResultsListSteps()
            list
                .isScreenPresented()
                .withOfferList()
                .isListEmpty()
        }
    }

    func testAddNote() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .additionalActions(.withoutUserNote))

        PersonalizationAPIStubConfigurator.setupUpsertNote(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithoutUsernote(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isUserNoteNotVisible()
                .tapOnAdditionalActions()

            let additionalActions = SnippetAdditionalActionsSteps()
            additionalActions
                .isScreenPresented()
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

            cardSteps
                .isUserNotePresented()
        }
    }

    func testEditNote() {
        let changedNotesText = "Заодно проверим длинный текст, который должен обрезаться до одной строки"
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .additionalActions(.withoutUserNote))

        PersonalizationAPIStubConfigurator.setupUpsertNote(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithUsernote(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isUserNotePresented()
                .scrollToUserNote()
                .tapOnUserNote()

            TextPickerSteps(name: "Редактирование заметки")
                .ensureScreenPresented()
                .typeText(changedNotesText)
                .tapOnDoneButton()
                .ensureScreenNotPresented()

            cardSteps
                .scrollToUserNote()
                .isUserNotePresented()
        }
    }

    func testRemoveNote() {
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .additionalActions(.withoutUserNote))

        PersonalizationAPIStubConfigurator.setupDeleteNote(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithUsernote(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isUserNotePresented()
                .tapOnAdditionalActions()

            let additionalActions = SnippetAdditionalActionsSteps()
            additionalActions
                .isScreenPresented()
                .tapOnButton(.editNote)

            TextPickerSteps(name: "Редактирование заметки")
                .ensureScreenPresented()
                .clearText()
                .tapOnDoneButton()
                .ensureScreenNotPresented()

            cardSteps
                .isUserNoteNotVisible()
        }
    }

    func testAnchors() {
        ImagesStubConfigurator.setupYouTubeVideoPreview(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        OfferCardAPIStubConfiguration.setupOfferCardWithAllContentTypes(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .gallery()
                .isPhotoVisible() // 1st image is photo
                .isAnchorButtonVisible()
                .checkAnchorTitle("Планировка")
                .swipeLeft(times: 1)
                .isVirtualTourVisible() // 2nd image is virtual tour
                .isAnchorButtonNotVisible()
                .swipeLeft(times: 1)
                .isYoutubeVideoVisible() // 3rd image is YouTube video
                .isAnchorButtonVisible()
                .checkAnchorTitle("Планировка")
                .tapAnchorButton() // Scroll to 4th image
                .isFlatPlanVisible() // 4th image is flat plan
                .isAnchorButtonVisible()
                .checkAnchorTitle("План этажа")
                .tapAnchorButton() // Scroll to 5th image
                .isFloorPlanVisible() // 5th image is floor plan
                .isAnchorButtonNotVisible()
        }
    }

    func testCallbackCell() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithCallback(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isOfferCardPresented()
                .scrollToCallbackNode()
                .tapOnCallbackNodePrivacyLink()
            
            ModalActionsAlertSteps()
                .isScreenPresented()
        }
    }

    func testOpenDocuments() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardInSite(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isOfferCardPresented()
                .scrollToDocuments()
                .tapOnDocumentsButton()

            WebPageSteps()
                .screenIsPresented()
                .tapOnCloseButton()
                .screenIsDismissed()
        }
    }

    func testYaRentAuthor() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardYandexRent(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .isOfferCardPresented()
                .scrollToAuthorBlock()

            var offerList: OfferListSteps!

            self.checkAuthorBlockYaRentActionTappedAnalytics {
                offerList = cardSteps
                    .tapOnAuthorBlockAction()
            }

            offerList
                .isScreenPresented()
        }
    }

    // MARK: Private

    private func performCommonTests(isAuthorized: Bool = false, specificCardTests: (OfferCardSteps) -> Void) {
        let configuration = ExternalAppConfiguration.offerCardTests
        configuration.isAuthorized = isAuthorized
        self.relaunchApp(with: configuration)

        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        let offerCardSteps = OfferCardSteps()
        offerCardSteps
            .isOfferCardPresented()
            .isLoadingIndicatorHidden()
        specificCardTests(offerCardSteps)
    }

    // MARK: - Analytics

    private func checkAnalytics(
        event: MetricaAnalyticsEvent,
        steps: @escaping () -> Void
    ) {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()
        let expectation = analyticsAgent.expect(event: event)
        steps()
        expectation.yreEnsureFullFilledWithTimeout()
    }

    private func checkAuthorBlockYaRentActionTappedAnalytics(steps: @escaping () -> Void) {
        self.checkAnalytics(
            event: .init(name: "Аренда. Карточка объявления. Тэп по посмотреть предложения в блоке автора"),
            steps: steps
        )
    }
}

// MARK: - Gallery

extension OfferCardTests {
    func testCallsFromGallery() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListBilling(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardBilling(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            let gallerySteps = cardSteps
                .tapOnGallery()

            let callScenarios = OfferCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: gallerySteps
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }

    func testAnchorsInFullScreenGallery() {
        ImagesStubConfigurator.setupYouTubeVideoPreview(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))
        OfferCardAPIStubConfiguration.setupOfferCardWithAllContentTypes(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            cardSteps
                .gallery()
                .tap()
                .isPhotoVisible() // 1st image is photo
                .isAnchorButtonVisible()
                .checkAnchorTitle("Планировка")
                .swipeLeft(times: 1)
                .isVirtualTourVisible() // 2nd image is virtual tour
                .isAnchorButtonNotVisible()
                .swipeLeft(times: 1)
                .isYoutubeVideoVisible() // 3rd image is YouTube video
                // TODO: Uncomment it after https://st.yandex-team.ru/VSAPPS-7530
                // .isAnchorButtonVisible()
                // .checkAnchorTitle("Планировка")
                // .tapAnchorButton() // Scroll to 4th image
                    .swipeLeft(times: 1)
                // TODO: end
                .isFlatPlanVisible() // 4th image is flat plan
                .isAnchorButtonVisible()
                .checkAnchorTitle("План этажа")
                .tapAnchorButton() // Scroll to 5th image
                .isFloorPlanVisible() // 5th image is floor plan
                .isAnchorButtonNotVisible()
        }
    }
}
