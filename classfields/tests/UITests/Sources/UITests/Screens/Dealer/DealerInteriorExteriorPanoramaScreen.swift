//
//  DealerInteriorExteriorPanoramaScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 12/14/20.
//
import XCTest
import Snapshots

final class DealerInteriorExteriorPanoramaScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var collectionView = findAll(.collectionView).firstMatch

    lazy var addExteriorLabel = find(by: "Добавить панораму автомобиля").firstMatch
    lazy var addInteriorLabel = find(by: "Загрузить интерьер").firstMatch
    lazy var imagesCloseButton = find(by: "Cancel").firstMatch
    lazy var panoramaExteriorInteriorHelpDesc = find(by: "BothPanoramasHelpDesc").firstMatch
    lazy var panoramaExteriorMenuButton = find(by: "panoramaMenuButton_exterior").firstMatch
    lazy var panoramaInteriorMenuButton = find(by: "panoramaMenuButton_interior").firstMatch
    lazy var panoramaReshootButton = find(by: "Переснять").firstMatch
    lazy var panoramaReplaceButton = find(by: "Заменить").firstMatch
}
