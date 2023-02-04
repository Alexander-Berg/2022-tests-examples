//
//  WizardScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 11/25/20.
//

import XCTest
import Snapshots

class WizardScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var skipButton = find(by: "Пропустить").firstMatch
    lazy var enterManually = find(by: "Заполнить вручную").firstMatch
    lazy var enterVINField = find(by: "app.pickers.vin").firstMatch
    lazy var bmwCell = find(by: "BMW").firstMatch
    lazy var bmw6Cell = find(by: "6 серии").firstMatch
    lazy var bmw6_2020Cell = find(by: "2020").firstMatch
    lazy var restyleCell = findContainedText(by: "Рестайлинг").firstMatch
    lazy var bodyCell = find(by: "Лифтбек").firstMatch
    lazy var engineCell = find(by: "Бензин").firstMatch
    lazy var wheelsCell = find(by: "Полный").firstMatch
    lazy var transmissionCell = find(by: "Автоматическая").firstMatch
    lazy var modificationCell = findContainedText(by: "333").firstMatch
    lazy var colorCell = find(by: "Белый").firstMatch
    lazy var ptsCell = find(by: "Оригинал/Электронный").firstMatch
    lazy var ownerCell = find(by: "Один").firstMatch
    lazy var addPhotoCell = find(by: "Добавить фото").firstMatch
    lazy var addCell = find(by: "Добавить").firstMatch
    lazy var removeFirstPhotoButton = find(by: "remove_photo_0").firstMatch
    lazy var manualOrderButton = find(by: "Вручную").firstMatch
    lazy var photoButton = find(by: "Фото").firstMatch
    lazy var readyButton = find(by: "Готово").firstMatch
    lazy var nextButton = find(by: "Далее").firstMatch
    lazy var continueButton = find(by: "Продолжить").firstMatch
    lazy var closeButton = find(by: "close").firstMatch
    lazy var backButton = find(by: "backButton").firstMatch
    lazy var descriptionScreen = find(by: "description_picker_view").firstMatch
    lazy var distanceScreen = find(by: "mileage_picker_view").firstMatch
    lazy var popUpScreen = find(by: "PopUpLayoutSpec").firstMatch
    lazy var vinCameraButton = find(by: "app.pickers.vin.cameraButton").firstMatch
    lazy var exchangeSwitcher = find(by: "exchangeSwitcher").firstMatch
    lazy var ndsSwitcher = find(by: "ndsSwitcher").firstMatch
    lazy var estimatedPriceLabel = find(by: "estimatedPriceLabel").firstMatch
    lazy var exchangeLabel = findContainedText(by: "Возможен обмен").firstMatch
    lazy var ndsLabel = find(by: "nds_label_to_show_info").firstMatch
    lazy var ndsDesc = find(by: "nds_desc_popup").firstMatch
}
