//
//  DeviceLogButton.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 16/04/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

final class DeviceLogButton: UIButton {

    static let preferredSize = CGSize(width: 48, height: 48)
    
    static func make() -> DeviceLogButton {
        let ret = DeviceLogButton(type: .system)
        ret.backgroundColor = .white
        ret.layer.masksToBounds = true
        return ret
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        layer.cornerRadius = bounds.height / 2
    }
}
