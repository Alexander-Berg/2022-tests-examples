//
//  YaRentNegativeBannerViewTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREComponents

final class YaRentNegativeBannerViewTests: XCTestCase {
    func testShortNegativeBannerView() {
        let viewModel = YaRentNegativeBannerView.ViewModel(title: "Заголовок", text: "Небольшой текст")
        self.testView(with: viewModel)
    }

    func testLongNegativeBannerView() {
        let viewModel = YaRentNegativeBannerView.ViewModel(
            title: "Очень-Очень длинный заголовок. Прям ну очень длинный. Может ли быть заголовок длиннее? Конечно нет!",
            text: """
            Мы писали, мы писали, наши пальчики устали, мы немножко отдохнем и опять писать начнем. \
            Раз, два, три, четрые, пять, я иду искать. Кто не спрятался, я не виноват.
            Вот это меня унесло, надо ещё этой штуки купить.
            """)
        self.testView(with: viewModel)
    }

    private func testView(with viewModel: YaRentNegativeBannerView.ViewModel, function: String = #function) {
        let view = YaRentNegativeBannerView()
        view.configure(with: viewModel)
        // @l-saveliy: systemLayoutSizeFitting works incorrect in this tests.
        // Use viewController to simulate autolayout
        let viewController = UIViewController()
        viewController.view.addSubview(view)
        view.yre_edgesToSuperview(insets: .init(top: .zero, left: .zero, bottom: .nan, right: .zero))
        self.assertSnapshot(viewController.view, function: function)
    }
}
