//
//  TestsEnviromentManager.swift
//  YRETestsUtils
//
//  Created by Dmitry Barillo on 28.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

enum TestsEnviromentManager {
    enum TargetType {
        case common
        case unitTests
        case uiTests
    }
    
    static var targetType: TargetType {
        let result: TargetType
        
        let enviroment = ProcessInfo.processInfo.environment
        let rawTargetType = enviroment["TestTargetType"]?.lowercased()
        
        switch rawTargetType {
            case "unit":
                result = .unitTests
            case "ui":
                result = .uiTests
            default:
                result = .common
        }
        
        return result
    }
}
