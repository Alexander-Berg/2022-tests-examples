//
//  YRECoreUI+Tests.swift
//  YRECoreUI
//
//  Created by Evgeny Y. Petrov on 27/12/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import UIKit
import SwiftUI

extension UIView {
    @objc
    public func debug_setAccessibilityIdentifier(_ value: String?) {
#if DEBUG || ADHOC
        self.accessibilityIdentifier = value
#endif
    }
}

extension UIViewController {
    @objc
    public func debug_setAccessibilityIdentifier(_ value: String?) {
#if DEBUG || ADHOC
        self.view.debug_setAccessibilityIdentifier(value)
#endif
    }
}

extension UIAlertController {
    @objc
    public func debug_markAlertController(accessibilityIdentifier: String?, actionsAccessibilityIdentifiers: [String]!) {
#if DEBUG || ADHOC
        guard self.isViewLoaded else { assertionFailure("Method should be called after alert presentation"); return }
        guard let actionsID = actionsAccessibilityIdentifiers else { assertionFailure(); return }

        self.view.debug_setAccessibilityIdentifier(accessibilityIdentifier)
        for (action, identifier) in zip(self.actions, actionsID) {
            let label = action.value(forKey: "__representer")
            let view = label as? UIView
            view?.debug_setAccessibilityIdentifier(identifier)
        }
#endif
    }
}

extension View {
    public func debug_accessibilityIdentifier(_ accessibilityIdentifier: String) -> some View {
#if DEBUG || ADHOC
        return self.accessibilityIdentifier(accessibilityIdentifier)
#else
        return self
#endif
    }
}

extension UIBarButtonItem {
    public func debug_setAccessibilityIdentifier(_ value: String?) {
#if DEBUG || ADHOC
        self.accessibilityIdentifier = value
#endif
    }
}
