//
//  MainTableCell.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit


class MainTableCell: UITableViewCell {

    func update(title: String?) {
        textLabel?.text = title
        textLabel?.font = Assets.Common.Fonts.smaller
    }
    
}
