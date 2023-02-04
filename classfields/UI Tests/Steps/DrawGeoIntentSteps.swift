//
//  DrawGeoIntentSteps.swift
//  UI Tests
//
//  Created by Aleksey Gotyanov on 10/27/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

final class DrawGeoIntentSteps {
    func tapOnCloseButton() {
        let closeButton = ElementsProvider.obtainButton(
            identifier: "geoIntentDrawing.closeButton"
        )

        closeButton
            .yreEnsureExists()
            .tap()
    }
}
