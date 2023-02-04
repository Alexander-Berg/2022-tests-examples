//
//  DealerInteriorExteriorPanoramaSteps.swift
//  UITests
//
//  Created by Dmitry Sinev on 12/15/20.
//
import XCTest
import Snapshots

final class DealerInteriorExteriorPanoramaSteps: BaseSteps {

    @discardableResult
    func checkHelpBothPanoramas() -> Self {
        step("Проверяем подсказку по съёмке панорам") {
            validateSnapshot(of: onDealerPanoramasScreen().panoramaExteriorInteriorHelpDesc, snapshotId: "bothPanoramasHelpDesc")
        }
    }

    @discardableResult
    func checkAddExteriorPanorama() -> Self {
        step("Проверяем наличие кнопки добавления внешней панорамы") {
            onDealerPanoramasScreen().addExteriorLabel.shouldExist()
        }
    }

    @discardableResult
    func checkAddInteriorPanorama() -> Self {
        step("Проверяем наличие кнопки добавления внутренней панорамы") {
            onDealerPanoramasScreen().addInteriorLabel.shouldExist()
            onDealerPanoramasScreen().addInteriorLabel.tap()

            self.handleSystemAlertIfNeeded()
                .handleSystemAlertIfNeeded()

            onDealerPanoramasScreen().imagesCloseButton.shouldExist()
            onDealerPanoramasScreen().imagesCloseButton.tap()
        }
    }

    @discardableResult
    func checkExteriorPanoramaMenu() -> Self {
        Step("Проверяем меню внешней панорамы") {
            onDealerPanoramasScreen().panoramaExteriorMenuButton.shouldExist()
            onDealerPanoramasScreen().panoramaExteriorMenuButton.tap()
            onDealerPanoramasScreen().panoramaReshootButton.shouldExist()
            
            wait(for: 1)

            // Tap outside menu to close it
            let normalCoord = onDealerPanoramasScreen().app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
            let coord = normalCoord.withOffset(CGVector(dx: 35, dy: 50))
            coord.tap()
        }
        return self
    }

    @discardableResult
    func checkInteriorPanoramaMenu() -> Self {
        Step("Проверяем меню внутренней панорамы") {
            onDealerPanoramasScreen().panoramaInteriorMenuButton.shouldExist()
            onDealerPanoramasScreen().panoramaInteriorMenuButton.tap()
            onDealerPanoramasScreen().panoramaReplaceButton.shouldExist()
            onDealerPanoramasScreen().panoramaReplaceButton.tap()
        }
        return self
    }

    @discardableResult
    func closeInteriorPanoramaMenu() -> Self {
        Step("Закрываем меню внутренней панорамы") {
            onDealerPanoramasScreen().imagesCloseButton.shouldExist()
            onDealerPanoramasScreen().imagesCloseButton.tap()
        }
        return self
    }

    // MARK: - Screens
    func onDealerPanoramasScreen() -> DealerInteriorExteriorPanoramaScreen {
        return baseScreen.on(screen: DealerInteriorExteriorPanoramaScreen.self)
    }
}
