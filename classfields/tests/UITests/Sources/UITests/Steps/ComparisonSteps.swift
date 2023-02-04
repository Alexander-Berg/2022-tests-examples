import XCTest
import Snapshots

class ComparisonSteps: BaseSteps {
    func onComparisonScreen() -> ComparisonScreen {
        self.baseScreen.on(screen: ComparisonScreen.self)
    }

    @discardableResult
    func checkScreenshotOfComparison(identifier: String) -> Self {
        step("Делаем скриншот и сравниваем со снепшотом '\(identifier)'") {
            let screenInsets = UIEdgeInsets(top: 88, left: 0, bottom: 34, right: 0)
            let screenshot = self.onComparisonScreen().scrollableElement.waitAndScreenshot().image
            let croppedImage = screenshot.cropping(insets: screenInsets)

            Snapshot.compareWithSnapshot(
                image: croppedImage,
                identifier: identifier,
                perPixelTolerance: 0.005,
                overallTolerance: 0.001
            )
        }
    }

    @discardableResult
    func tapOnOptionSection(name: String) -> Self {
        step("Тапаем на секцию '\(name)' в шапке") {
            self.onComparisonScreen().optionSection(name: name).tap()
        }
    }

    @discardableResult
    func tapOnDifferenceSwitch() -> Self {
        step("Тапаем на свитч 'только отличия'") {
            self.onComparisonScreen().differenceSwitch.tap()
        }
    }

    @discardableResult
    func scrollUp() -> Self {
        step("Проскролл вниз для сворачивания шапки") {
            self.onComparisonScreen().scrollableElement.gentleSwipe(.up)
        }
    }

    @discardableResult
    func scrollDown() -> Self {
        step("Проскролл вверх для разворачивания шапки") {
            self.onComparisonScreen().scrollableElement.gentleSwipe(.down)
        }
    }

    @discardableResult
    func scrollLeft() -> Self {
        step("Проскроллим вправо для смены колонки") {
            self.onComparisonScreen().scrollableElement.swipeLeft()
        }
    }

    @discardableResult
    func scrollRight() -> Self {
        step("Проскроллим влево для смены колонки") {
            self.onComparisonScreen().scrollableElement.swipeRight()
        }
    }

    @discardableResult
    func checkScreenshotOfHeader(identifier: String) -> Self {
        step("Делаем скриншот шапки и сравниваем с '\(identifier)'") {
            let screenInsets = UIEdgeInsets(top: 88, left: 0, bottom: 0, right: 0)
            let screenshot = self.onComparisonScreen().comparisonHeaderShadowView.waitAndScreenshot().image
            let croppedImage = screenshot.cropping(insets: screenInsets)
            Snapshot.compareWithSnapshot(image: croppedImage, identifier: identifier)
        }
    }
}

class PhotoHeaderComparisonSteps: ComparisonSteps {
    private func onPhotoHeaderComparisonScreen() -> PhotoHeaderComparisonScreen {
        self.baseScreen.on(screen: PhotoHeaderComparisonScreen.self)
    }

    @discardableResult
    func tapOnHeader(column: Int) -> Self {
        step("Тапаем в шапку в колонке #\(column)") {
            self.onPhotoHeaderComparisonScreen().header(column: column).tap()
        }
    }

    @discardableResult
    func tapOnPinButton(column: Int) -> Self {
        step("Тапаем на кнопку запина в шапке в колонке #\(column)") {
            self.onPhotoHeaderComparisonScreen().headerPinButton(column: column).tap()
        }
    }

    @discardableResult
    func tapOnRemoveButton(column: Int) -> Self {
        step("Тапаем на кнопку удаления в шапке в колонке #\(column)") {
            self.onPhotoHeaderComparisonScreen().headerRemoveButton(column: column).tap()
        }
    }

    @discardableResult
    func tapOnCallButton(column: Int) -> Self {
        step("Тапаем на кнопку Позвонить в шапке колонке #\(column)") {
            self.onPhotoHeaderComparisonScreen().headerCallButton(column: column).tap()
        }
    }

    @discardableResult
    func scrollTo(name: String, columnIndex: Int, windowInsets: UIEdgeInsets = .zero) -> Self {
        step("Проскроллим вниз до ячейки '\(name)' в колонке #\(columnIndex)") {
            let element = self.onPhotoHeaderComparisonScreen().clickableCell(name: name, column: columnIndex)
            self.onPhotoHeaderComparisonScreen().scrollTo(element: element, maxSwipes: 5, windowInsets: windowInsets)
        }
    }

    @discardableResult
    func tapOnCell(name: String, columnIndex: Int) -> Self {
        step("Тапаем на ячейку '\(name)' в колонке #\(columnIndex)") {
            self.onPhotoHeaderComparisonScreen().clickableCell(name: name, column: columnIndex).tap()
        }
    }
}

final class OffersComparisonSteps: PhotoHeaderComparisonSteps {
    func onOffersComparisonScreen() -> OffersComparisonScreen {
        self.baseScreen.on(screen: OffersComparisonScreen.self)
    }

    @discardableResult
    func checkComparisonScreenTitle() -> Self {
        step("Проверяем, что виден заголовок экрана сравнений") {
            self.onOffersComparisonScreen().title.shouldExist()
        }
    }
}
