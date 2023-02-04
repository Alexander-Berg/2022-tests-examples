//
//  SaleListSnippetLayoutTests.swift
//  Tests
//
//  Created by Vitalii Stikhurov on 15.07.2021.
//

import XCTest
import AutoRuModels
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuCellHelpers
@testable import AutoRuViews
import Foundation

class SnippetGalleryLayoutTests: BaseUnitTest {
    func test_photosCreditLayout_extended_noPhoto() {
        let layout = PhotosCreditLayout(percent: "7%",
                                        promoTitle: nil,
                                        buttonText: "Заполнить заявку",
                                        banksIcons: [
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                                        ],
                                        isExtended: true,
                                        backgroundImage: nil)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 382, maxHeight: 286)
    }

    func test_photosCreditLayout_extended_new() {
        let layout = PhotosCreditLayout(percent: "7%",
                                        promoTitle: nil,
                                        buttonText: "Заполнить заявку",
                                        banksIcons: [
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                                        ],
                                        isExtended: true,
                                        backgroundImage: FetchableImage(url: URL(string: "sad.ru")))
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 382, maxHeight: 286)
    }

    func test_photosCreditLayout_extended_draft() {
        let layout = PhotosCreditLayout(percent: "7%",
                                        promoTitle: nil,
                                        buttonText: "Дополнить заявку",
                                        banksIcons: [
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                                        ],
                                        isExtended: true,
                                        backgroundImage: FetchableImage(url: URL(string: "sad.ru")))
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 382, maxHeight: 286)
    }

    func test_photosCreditLayout_small_new() {
        let layout = PhotosCreditLayout(percent: "7%",
                                        promoTitle: nil,
                                        buttonText: "Заполнить заявку",
                                        banksIcons: [
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                                        ],
                                        isExtended: false,
                                        backgroundImage: FetchableImage(url: URL(string: "sad.ru")))
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 200, maxHeight: 140)
    }

    func test_photosCreditLayout_small_draft() {
        let layout = PhotosCreditLayout(percent: "7%",
                                        promoTitle: nil,
                                        buttonText: "Дополнить заявку",
                                        banksIcons: [
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                                            FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                                        ],
                                        isExtended: false,
                                        backgroundImage: FetchableImage(url: URL(string: "sad.ru")))
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 200, maxHeight: 140)
    }

    func test_photosCalllLayout() {
        let layout = PhotosCallButtonLayout(onTap: { })
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 200, maxHeight: 140)
    }

    func test_photosChatsLayout() {
        let layout = PhotosChatsButtonLayout(onTap: { })
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 200, maxHeight: 140)
    }

    func test_photosReportLayout() {
        let layout = BuyReportGalleryButtonLayout.make()
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 200, maxHeight: 140)
    }

    func test_photosContatsLayout() {
        let layout = PhotosContactsButtonLayout(
            model: PhotosContactsButtonLayoutModel(newSale: true,
                                                   registrationDate: Date(),
                                                   saleFromCompany: true,
                                                   officialDealer: true,
                                                   dealerName: "Дилер",
                                                   userName: "Рамзан",
                                                   isAuto: true,
                                                   offerId: ""),
            onTap: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 200, maxHeight: 140)
    }
}
