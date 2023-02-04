//
//  ObjectDetailsPresenterMock.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 03.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

@testable import YREYaRentInventoryModule

final class ObjectDetailsPresenterMock: ObjectDetailsPresenterProtocol {
    weak var viewController: ObjectDetailsViewController?

    func viewDidLoad() {
        self.viewController?.updateViewModel(.init(
            defectText: "Текст дефекта. Он может быть в несколько строк. Для этого продолжаю его писать",
            images: [nil, nil, nil]
        ))
    }

    func photoItemTapped(index: Int) {
    }
}
