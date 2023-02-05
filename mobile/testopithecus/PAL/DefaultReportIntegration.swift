//
//  DefaultReportIntegration.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem Zoshchuk on 14.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class DefaultReportIntegration: ReportIntegration {
    public func addFeatureName(_ feature: String) {
        Allure.setTestFeature(feature: feature)
    }
        
    public func addTestpalmId(_ id: Int32) {
        Allure.setTestId(testPalmCaseID: Int(id))
    }
}
