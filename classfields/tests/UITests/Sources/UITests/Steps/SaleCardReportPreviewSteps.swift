//
//  SaleCardReportPreviewSteps.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 28.05.2020.
//

import XCTest
import Snapshots

class SaleCardReportPreviewSteps: BaseSteps {

    func onSaleCardReportPreviewScreen() -> SaleCardReportPreviewScreen {
        return baseScreen.on(screen: SaleCardReportPreviewScreen.self)
    }

    func addCommentButtonTap() -> ReportCommentEditorSteps {
        onSaleCardReportPreviewScreen().addCommentButton.tap()

        return ReportCommentEditorSteps(context: context)
    }

    func addPhotoButtonTap() -> OfferEditSteps {
        onSaleCardReportPreviewScreen().addPhotoButton.tap()

        return OfferEditSteps(context: context)
    }

    func openSupportChatButtonTap() -> ChatSteps {
        onSaleCardReportPreviewScreen().openChatSupportButton.tap()

        return ChatSteps(context: context)
    }

    func scrollToFreeReport() -> SaleCardReportPreviewSteps {
        onSaleCardReportPreviewScreen().scrollTo(element: onSaleCardReportPreviewScreen().reportOpenFreeButton, windowInsets: .init(top: 0, left: 0, bottom: 250, right: 0), swipeDirection: .up)

        return SaleCardReportPreviewSteps(context: context)
    }

    @discardableResult
    func openFreeReport() -> CarfaxStandaloneCardBasicSteps {
        onSaleCardReportPreviewScreen().reportOpenFreeButton.tap()
        return CarfaxStandaloneCardBasicSteps(context: context)
    }
    
    @discardableResult
    func findVIN(_ vin: String) -> Self {
        onSaleCardReportPreviewScreen().find(by: vin).firstMatch.shouldExist()
        return self
    }

    func copyVIN(_ vin: String) -> Self {
        onSaleCardReportPreviewScreen().find(by: vin).firstMatch.tap(withNumberOfTaps: 2, numberOfTouches: 1)
        return self
    }

    @discardableResult
    func closeCommentInfoCell() -> SaleCardReportPreviewSteps {
        let element = onSaleCardReportPreviewScreen().scrollableElement.cell(containing: "backend_layout_cell_commentInfoCell").images.firstMatch
        element.tap()
        wait(for: 2)
        element.shouldNotExist()

        return SaleCardReportPreviewSteps(context: context)
    }
}
