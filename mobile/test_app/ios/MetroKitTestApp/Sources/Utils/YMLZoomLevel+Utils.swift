//
//  YMLZoomLevel+Utils.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 08/04/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension YMLZoomLevel {

    public override func isEqual(_ object: Any?) -> Bool {
        if let rhs = object as? YMLZoomLevel {
            return value == rhs.value
        } else {
            return false
        }
    }
    
    public override var hash: Int {
        get {
            return value.hashValue
        }
    }
    
}
