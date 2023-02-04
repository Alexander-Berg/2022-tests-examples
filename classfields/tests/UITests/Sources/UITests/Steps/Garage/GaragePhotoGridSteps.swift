final class GaragePhotoGridSteps: BaseSteps {

    @discardableResult
    func tapOnBackButton() -> Self {
        step("Тапаем на кнопку Назад") {
            onGaragePhotoGridScreen().backButton.tap()
        }
    }

    @discardableResult
    func removePhoto(_ id: String) -> Self {
        step("Удаляем фото \(id)") {
            onGaragePhotoGridScreen().deleteButton(forPhoto: id).tap()
        }
    }

    @discardableResult
    func movePhoto(_ id: String, to position: Int) -> Self {
        step("Перемещаем фото \(id) на позицию \(position)") {
            onGaragePhotoGridScreen()
                .photo(withID: id)
                .press(forDuration: 0.5, thenDragTo: onGaragePhotoGridScreen().photo(byIndex: position))
        }
    }

    @discardableResult
    func checkFailedPhoto(_ id: String) -> Self {
        step("Проверяем, для фото \(id) показана ошибка") {
            onGaragePhotoGridScreen()
                .photo(withID: id)
                .descendants(matching: .any)
                .matching(identifier: "Ошибка")
                .firstMatch
                .shouldExist()
        }
    }

    @discardableResult
    func tapOnPhoto(_ id: String) -> Self {
        step("Тапаем на фото \(id)") {
            onGaragePhotoGridScreen().photo(withID: id).tap()
        }
    }

    private func onGaragePhotoGridScreen() -> GaragePhotoGridScreen {
        onMainScreen().on(screen: GaragePhotoGridScreen.self)
    }

}
