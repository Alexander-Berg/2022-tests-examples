import XCTest
import Snapshots

final class GarageSteps: BaseSteps {

    @discardableResult
    func checkPromoBanner() -> Self {
        step("Скриншотим и проверяем промобаннер гаража") {
            let element = self.onGarageScreen().garagePromo
            Snapshot.compareWithSnapshot(element: element, identifier: .make())
        }
    }

    @discardableResult
    func checkBlock(title: String) -> Self {
        step("Проверяем, что есть блок '\(title)'") {
            let element = self.onGarageScreen().item(title)
            self.onGarageScreen().scrollTo(element: element, swipeDirection: .up)
            element.shouldBeVisible()
        }
    }

    @discardableResult
    func tapOnPromoBanner() -> GarageLandingSteps {
        step("Тап на промобаннер гаража") {
            self.onGarageScreen().garagePromo.tap()
        }
        .as(GarageLandingSteps.self)
    }

    @discardableResult
    func tapOnPromoBannerAddCar() -> AddCarScreen {
        step("Тапаем на добавление тачки на баннере") {
            self.onGarageScreen().garageBannerAddCarButton.tap()
        }
        .as(AddCarScreen.self)
    }

    @discardableResult
    func shouldSeeGarageCard(id: String) -> Self {
        step("Проверяем, что видим гаражную карточку id=\(id)") {
            self.onGarageScreen().garageCar(id: id).shouldExist()
        }
    }

    @discardableResult
    func shouldNotSeeGarageCard(id: String) -> Self {
        step("Проверяем, что не видим гаражную карточку id=\(id)") {
            self.onGarageScreen().garageCar(id: id).shouldNotExist()
        }
    }

    @discardableResult
    func shouldNotSeeLogInText() -> Self {
        step("Проверяем отсутствие надписи войти") {
            self.onGarageScreen().item("Войти").shouldNotExist()
        }
    }

    @discardableResult
    func tapOnGarageCard(id: String) -> GarageCardSteps {
        step("Тапаем на гаражную карточку id=\(id)") {
            self.onGarageScreen().garageCar(id: id).tap()
        }
        .as(GarageCardSteps.self)
    }

    @discardableResult
    func checkGarageCard(id: String, snapshot: String) -> Self {
        step("Скриншотим и проверяем карточку гаража id=\(id) -> \(snapshot)") {
            let card = self.onGarageScreen().garageCar(id: id)
            self.onGarageScreen().scrollTo(element: card, maxSwipes: 2, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 100, right: 0))
            let screenshot = card.waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: snapshot)
        }
    }

    @discardableResult
    func tapOnAddCarButton() -> AddCarScreen {
        step("Тапаем на кнопку для добавления в гараж") {
            let element = self.onGarageScreen().garageAddCarButton
            self.onGarageScreen().scrollTo(element: element, maxSwipes: 2)
            element.tap()
        }
        .as(AddCarScreen.self)
    }

    @discardableResult
    func shouldNotSeeAddCarButton() -> Self {
        step("Проверяем, что нет кнопки для добавления в гараж") {
            self.onGarageScreen().garageAddCarButton.shouldNotExist()
        }
    }

    // MARK: - Private

    private func onGarageScreen() -> GarageScreen {
        return self.baseScreen.on(screen: GarageScreen.self)
    }
}

final class GarageLandingSteps: BaseSteps {
    @discardableResult
    func tapOnAddCarButton() -> AddCarScreen {
        step("Тапаем на кнопку для добавления в гараж") {
            self.onGarageLandingScreen().garageAddCarButton.tap()
        }
        .as(AddCarScreen.self)
    }

    // MARK: - Private

    private func onGarageLandingScreen() -> GarageLandingScreen {
        self.baseScreen.on(screen: GarageLandingScreen.self)
    }
}
