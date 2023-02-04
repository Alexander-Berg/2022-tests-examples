//
//  YaRentInventoryTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 02.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import SwiftUI
import XCTest
import YREInventoryComponents
import Formaldehyde
import YREDesignKit

@testable import YREYaRentInventoryModule

final class YaRentInventoryTests: XCTestCase {
    func testBannerViewWithLink() {
        self.assertView(
            viewBuilder: {
                InventoryBannerView(
                    model: .init(
                        title: "Вот такой вот текст на баннер. А ещё тут можно нажать. ",
                        linkText: "Тык",
                        linkTapped: {}
                    )
                )
            },
            fixedSize: true
        )
    }

    func testRoomList() {
        self.assertView {
            InventoryRoomList(
                sections: [
                    .makeEditableSection(
                        headerTitle: "Комната",
                        editButtonAccessibilityID: "",
                        addItemAccessibilityID: "",
                        items: [
                            .init(imageURL: nil, title: "Объект", hasDefect: true, count: 3, accessibilityID: "", onSelect: {}),
                        ],
                        editTitleAction: {},
                        addItemAction: {}
                    ),
                ]
            )
        }
    }

    func testPreviewList() {
        self.assertView {
            InventoryPreviewList(
                model: .init(
                    rooms: [
                        .makePreviewSection(
                            headerTitle: "Комната",
                            items: [.init(imageURL: nil, title: "Объект", hasDefect: true, count: 3, accessibilityID: "", onSelect: {})]
                        )
                    ],
                    defects: [
                        .init(imageURL: nil, title: "Дефект", accessibilityID: "", onSelect: {})
                    ]
                )
            )
        }
    }

    func testEmptyPreviewList() {
        self.assertView {
            InventoryPreviewList(
                model: .init(
                    rooms: [
                        .makePreviewSection(
                            headerTitle: "Комната",
                            items: []
                        )
                    ],
                    defects: []
                )
            )
        }
    }

    func testDefectList() {
        self.assertView {
            InventoryDefectList(model: .init(
                defects: [
                    .init(imageURL: nil, title: "Дефект", accessibilityID: "", onSelect: {})
                ],
                addNewItemAccessibilityID: "",
                addNewItemAction: {}
            ))
        }
    }

    func testOverlayButton() {
        self.assertView(
            viewBuilder: {
                YREOverlayButton(
                    model: .init(
                        buttonTitle: "Подписать",
                        buttonStyle: .primary,
                        buttonAccessibilityID: "",
                        comment: "Подстрочный текст. На маленьких девайсах он не войдет в одну строку. Вот так"
                    ),
                    action: {}
                )
            },
            fixedSize: true
        )
    }

    func testAddRoomButton() {
        self.assertView {
            InventoryAddRoomButton(action: {})
        }
    }

    func testObjectDetails() {
        let presenter = ObjectDetailsPresenterMock()
        let viewController = ObjectDetailsViewController(presenter: presenter)
        presenter.viewController = viewController
        self.assertSnapshot(viewController.view)
    }

    private func assertView<T: View>(
        viewBuilder: () -> T,
        fixedSize: Bool = false,
        file: StaticString = #file,
        function: String = #function
    ) {
        let view = viewBuilder()
            .frame(width: UIScreen.main.bounds.width)

        if fixedSize {
            // Should be setted cause of crunch for iOS 13. See InventoryBannerView implementation
            let fixedSizeView = view.fixedSize(horizontal: true, vertical: false)
            self.assertSnapshot(fixedSizeView, file: file, function: function)
        }
        else {
            self.assertSnapshot(view, file: file, function: function)
        }
    }
}
