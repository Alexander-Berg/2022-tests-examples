//
//  SaleCardReportPreviewScreen.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 28.05.2020.
//

import XCTest
import Snapshots

class SaleCardReportPreviewScreen: BaseScreen, Scrollable, NavigationControllerContent {

    lazy var addCommentButton: XCUIElement = {
        let button = findAll(.staticText)["Комментировать"].firstMatch
        scrollTo(element: button)

        return button
    }()

    lazy var addPhotoButton: XCUIElement = {
        let button = findAll(.staticText)["Добавить фото"].firstMatch
        scrollTo(element: button)

        return button
    }()

    lazy var openChatSupportButton: XCUIElement = {
        let button = findAll(.staticText)["Поддержка"].firstMatch
        scrollTo(element: button)

        return button
    }()

    lazy var reportOpenFreeButton = find(by: "Смотреть бесплатный отчёт").firstMatch

    lazy var scrollableElement: XCUIElement = {
        return findAll(.collectionView).firstMatch
    }()

    lazy var previewTitle = find(by: "preview_title_cell").firstMatch
}
