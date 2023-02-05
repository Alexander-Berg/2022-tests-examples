//
//  MainSettings.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 23/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

final class SchemeSettingsAlert {
    
    struct Action {
        var title: String
        var handler: () -> Void
    }
    
    init(title: String?, message: String?, actions: [Action]) {
        self.title = title
        self.message = message
        self.actions = actions
    }
    
    func present(on presenter: UIViewController, barButtonItem: UIBarButtonItem, animated: Bool) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)
        alert.popoverPresentationController?.barButtonItem = barButtonItem
        
        for action in actions {
            let alertAction = UIAlertAction(title: action.title, style: .default) { _ in
                action.handler()
            }
            alert.addAction(alertAction)
        }
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        presenter.present(alert, animated: animated)
    }
    
    // MARK: Private

    private let title: String?
    private let message: String?
    private let actions: [Action]
    
}
