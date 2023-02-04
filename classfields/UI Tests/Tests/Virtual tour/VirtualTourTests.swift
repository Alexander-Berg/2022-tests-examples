//
//  VirtualTourTests.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 31.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class VirtualTourTests: BaseTestCase {
    func testOpenYandexRentVirtualTourFromCard() {
        APIStubConfigurator.setupOfferSearchResultsList_YandexRent(using: self.dynamicStubs)
        self.relaunchApp(with: .snippetsTests)

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .withOfferList()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        let offerCardSteps = OfferCardSteps()
        offerCardSteps
            .gallery()
            .isVirtualTourNotVisible()
            .swipeLeft(times: 1)
            .isVirtualTourVisible()
            .tap()

        let webPage = WebPageSteps()
        webPage
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()
    }

    func testOpenIframeVirtualTourFromCard() {
        APIStubConfigurator.setupOfferSearchResultsList_IframeVirtualTour(using: self.dynamicStubs)
        self.relaunchApp(with: .snippetsTests)

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .withOfferList()
            .cell(withIndex: 0)
            .isPresented()
            .gallery()
            .isVirtualTourVisible()
            .tap()

        let offerCardSteps = OfferCardSteps()
        offerCardSteps
            .gallery()
            .isVirtualTourVisible()
            .tap()

        let webPage = WebPageSteps()
        webPage
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()
    }

    func testGalleryWithVirtualTourAndNoPhoto() {
        APIStubConfigurator.setupOfferSearchResultsList_YandexRent_VirtualTourOnly(using: self.dynamicStubs)
        self.relaunchApp(with: .snippetsTests)

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .withOfferList()
            .cell(withIndex: 0)
            .isPresented()
            .gallery()
            .isVirtualTourVisible()
            .tap()

        let offerCardSteps = OfferCardSteps()
        offerCardSteps
            .gallery()
            .isVirtualTourVisible()
    }

    func testGalleryWithVirtualTourAndYoutubeVideo() {
        APIStubConfigurator.setupOfferSearchResultsList_YandexRent_YoutubeVideo(using: self.dynamicStubs)
        self.relaunchApp(with: .snippetsTests)

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .withOfferList()
            .cell(withIndex: 0)
            .isPresented()
            .gallery()
            .swipeLeft(times: 1)
            .isVirtualTourVisible()
            .swipeLeft(times: 1)
            .isYoutubeVideoVisible()
            .tap()

        let offerCardSteps = OfferCardSteps()
        offerCardSteps
            .gallery()
            .isYoutubeVideoVisible()
            .swipeRight(times: 1)
            .isVirtualTourVisible()
            .swipeRight(times: 1)
    }
}
