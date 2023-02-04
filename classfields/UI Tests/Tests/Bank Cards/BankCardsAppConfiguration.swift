//
//  Created by Alexey Aleshkov on 27/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREAppConfig

extension ExternalAppConfiguration {
    static var bankCardsUITests: ExternalAppConfiguration {
        return ExternalAppConfiguration(
            launchCount: 1,
            pushNotificationIntroWasShown: true,
            shouldDisplayEmbeddedMainFilters: false,
            mainListOpened: false,
            selectedTabItem: .more,
            isAuthorized: true,
            geoData: .fallback(),
            featureFlags: [:],
            backendEndpoint: .local,
            shouldSaveAppState: false,
            isAnimationEnabled: false
        )
    }
}
