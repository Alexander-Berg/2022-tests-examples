//
//  Created by Pavel Zhuravlev on 16.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import UIKit

public final class TestsAppDelegate: UIResponder, UIApplicationDelegate {
    public lazy var window: UIWindow? = {
        return UIWindow(frame: UIScreen.main.bounds)
    }()

    public func application(
        _ application: UIApplication,
        willFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UIView.setAnimationsEnabled(false)
        return true
    }
}
