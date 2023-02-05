//
// Created by Fedor Amosov on 10.12.2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import Foundation

public class XProxyTestCase: YOTestCase {
    private static let yandexHost = "https://xp.yandex-team.ru"

    var configuration: XProxyConfiguration {
        fatalError("No configuration specified!")
    }

    public override var launchArguments: [String] {
        return [CommandLineArguments.baseURLKey, "\(XProxyTestCase.yandexHost)/c/\(self.configuration)/api/mobile"]
    }
}
