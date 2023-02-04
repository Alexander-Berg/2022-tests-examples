//
//  AsyncDisplayKit+Tests.swift
//  YRETextureUtils
//
//  Created by Alexey Salangin on 8/15/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import AsyncDisplayKit

extension ASDisplayNode {
    @objc
    public func debug_setAccessibilityIdentifier(_ value: String?) {
#if DEBUG || ADHOC
        self.accessibilityIdentifier = value
#endif
    }
}
