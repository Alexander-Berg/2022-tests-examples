//
//  SchemeListCellInfo.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 08/12/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation

final class SchemeListCellInfo {

    enum State {
        case normal
        case update(() -> Void)
        case download(() -> Void)
        case loading
    }

    let title: String
    let id: String
    let size: String?
    let state: State
    
    init(title: String, id: String, size: String?, state: State) {
        self.title = title
        self.id = id
        self.size = size
        self.state = state
    }
}
