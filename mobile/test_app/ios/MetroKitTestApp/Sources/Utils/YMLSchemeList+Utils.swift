//
//  YMLSchemeList+Utils.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 10/12/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension YMLSchemeList {
    
    func selectSummary(withId id: YMLSchemeId?) -> YMLSchemeSummary? {
        let summaryList = items.flatMap { $0.schemes }

        if let id = id, let summary = summaryList.first(where: { $0.schemeId.value == id.value }) {
            return summary
        } else {
            return summaryList.first(where: { $0.aliases.contains("moscow") })
        }
    }
    
}
