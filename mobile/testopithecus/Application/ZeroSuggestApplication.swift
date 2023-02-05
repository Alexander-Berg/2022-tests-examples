//
// Created by Artem I. Novikov on 07.04.2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class ZeroSuggestApplication: ZeroSuggest {
    private let zeroSuggestPage = ZeroSuggestPage()
    
    public func isShown() -> Bool {
        return self.zeroSuggestPage.view.exists
    }
    
    public func getZeroSuggest() -> YSArray<String> {
        return YSArray(array: self.zeroSuggestPage.suggestNamesList)
    }
    
    public func searchByZeroSuggest(_ suggest: String) throws {
        guard let suggestLabel = self.zeroSuggestPage.findSuggestByName(suggest) else {
            throw YSError("Unable to find suggest with label \"\(suggest)\"")
        }
        try suggestLabel.tap()
    }
}
