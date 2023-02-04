//
//  OfferPanelViewTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 27.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit

final class OfferPanelViewTests: XCTestCase {
    func testCommonOfferLayout() {
        let content = OfferPanelView.ViewModel.Content(
            offerImage: Self.makeOfferImageURL(),
            offerImageEmptyStateAsset: Asset.Images.Offer.NoPhotos.offerRoom,
            title: "3 600 000 ₽",
            primaryDescription: "2 комн., 62,2 м²",
            secondaryDescription: "Москва, Чертановская улица, 9",
            isOutdated: false
        )
        let viewModel = OfferPanelView.ViewModel.content(content)
        let view = Self.makeView(using: viewModel)
        let bkgView = Self.makeWrappedView(for: view)

        self.assertSnapshot(bkgView)
    }

    func testOutdatedOfferLayout() {
        let content = OfferPanelView.ViewModel.Content(
            offerImage: Self.makeOfferImageURL(),
            offerImageEmptyStateAsset: Asset.Images.Offer.NoPhotos.offerRoom,
            title: "3 600 000 ₽",
            primaryDescription: "2 комн., 62,2 м²",
            secondaryDescription: "Москва, Чертановская улица, 9",
            isOutdated: true
        )
        let viewModel = OfferPanelView.ViewModel.content(content)
        let view = Self.makeView(using: viewModel)
        let bkgView = Self.makeWrappedView(for: view)

        self.assertSnapshot(bkgView)
    }

    func testMissingOfferLayout() {
        let viewModel = OfferPanelView.ViewModel.outdatedEmptyOffer
        let view = Self.makeView(using: viewModel)
        let bkgView = Self.makeWrappedView(for: view)

        self.assertSnapshot(bkgView)
    }

    // MARK: Private

    private static func makeView(using viewModel: OfferPanelView.ViewModel) -> OfferPanelView {
        let view = OfferPanelView(viewModel: viewModel)
        view.frame = .init(
            origin: .zero,
            size: .init(width: UIScreen.main.bounds.width, height: view.intrinsicContentSize.height)
        )
        return view
    }

    private static func makeOfferImageURL() -> URL? {
        return URL(string: "http://avatars.mds.yandex.net/some-photo")
    }

    private static func makeWrappedView(for view: UIView) -> UIView {
        let wrapper = UIView(frame: view.bounds)
        wrapper.backgroundColor = ColorScheme.Background.primary
        wrapper.addSubview(view)
        return wrapper
    }
}
