//
//  SiteToursViewTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 31.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import UIKit
@testable import YRESiteCardModule
import YRETestsUtils

final class SiteToursViewTests: XCTestCase {
    func testOneTour() {
        let tours: [SiteTourView.Tour] = [
            .init(photoURL: YREUnwrap(URL(string: "https://yandex.ru")), caption: "Тур 1"),
        ]
    
        let width = UIScreen.main.bounds.width
        let height = SiteTourView.height(width: width, viewModel: tours)

        let view = SiteTourView()
        view.tours = tours
        view.frame = CGRect(x: .zero, y: .zero, width: width, height: height)

        self.assertSnapshot(view)
    }

    func testManyTours() {
        let tours: [SiteTourView.Tour] = [
            .init(photoURL: YREUnwrap(URL(string: "https://yandex.ru")), caption: "Тур 1"),
            .init(photoURL: YREUnwrap(URL(string: "https://yandex.ru")), caption: "Тур 2"),
            .init(photoURL: YREUnwrap(URL(string: "https://yandex.ru")), caption: "Тур 3"),
            .init(photoURL: YREUnwrap(URL(string: "https://yandex.ru")), caption: "Тур 4"),
            .init(photoURL: YREUnwrap(URL(string: "https://yandex.ru")), caption: "Тур 5"),
        ]
    
        let width = UIScreen.main.bounds.width
        let height = SiteTourView.height(width: width, viewModel: tours)

        let view = SiteTourView()
        view.tours = tours
        view.frame = CGRect(x: .zero, y: .zero, width: width, height: height)

        self.assertSnapshot(view)
    }
}
