//
//  KeyValueStorage.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 02/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public protocol KeyValueStorage: class {
    subscript<T>(key: String) -> T? { get set }
}
