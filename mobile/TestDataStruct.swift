//
//  TestDataStruct.swift
//  QATools
//
//  Created by Dmitrii Abanin on 12/4/20.
//

import Foundation

struct Menu {
    var title: String
    var row: [DataSet]
    var isExpanded: Bool
}

struct DataSet {
    let name: String
    let link: String
}
