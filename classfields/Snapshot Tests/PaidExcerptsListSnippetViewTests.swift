//
//  PaidExcerptsListSnippetViewTests.swift
//  Unit Tests
//
//  Created by Ella Meltcina on 21.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
@testable import YREPaidExcerptsListModule

class PaidExcerptsListSnippetViewTests: XCTestCase {
    func testDoneStatusWithOfferLayout() {
        let viewModel = self.makeViewModel(
            status: .done,
            buttons: [.showOffer, .showExcerpt]
        )
        let view = PaidExcerptsListSnippetView(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testDoneStatusWithoutOfferLayout() {
        let viewModel = self.makeViewModel(
            status: .done,
            buttons: [.showExcerpt]
        )
        let view = PaidExcerptsListSnippetView(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testInProgressWithoutOfferLayout() {
        let viewModel = self.makeViewModel(
            status: .inProgress,
            buttons: []
        )
        let view = PaidExcerptsListSnippetView(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testErrorWithoutOfferLayout() {
        let viewModel = self.makeViewModel(
            status: .error,
            buttons: []
        )
        let view = PaidExcerptsListSnippetView(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testInProgressWithOfferLayout() {
        let viewModel = self.makeViewModel(
            status: .inProgress,
            buttons: [.showOffer]
        )
        let view = PaidExcerptsListSnippetView(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    func testErrorWithOfferLayout() {
        let viewModel = self.makeViewModel(
            status: .error,
            buttons: [.payAgain, .showOffer]
        )
        let view = PaidExcerptsListSnippetView(viewModel: viewModel)

        self.setupView(view, with: viewModel)

        self.assertSnapshot(view)
    }

    // MARK: - Private

    private typealias ViewModel = PaidExcerptsListSnippetView.ViewModel

    private func makeViewModel(
        status: ViewModel.Status,
        buttons: [ViewModel.ButtonType]
    ) -> ViewModel {
        ViewModel(
            address: "Москва, Чертнановкская улица, 48к2",
            date: "Отчёт от 22 декабря 2020",
            area: "53,4 м²",
            floor: "10",
            cadastralNumber: "77:05:0007006:****",
            status: status,
            buttons: buttons
        )
    }

    private func setupView(
        _ view: PaidExcerptsListSnippetView,
        with viewModel: ViewModel
    ) {
        let width = UIScreen.main.bounds.width
        let height = PaidExcerptsListSnippetView.height(width: width, viewModel: viewModel)

        let size = CGSize(width: width, height: height)
        view.frame = .init(origin: .zero, size: size)
    }
}
