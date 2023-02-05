//
//  DummyPageViewController.swift
//  YxSwissKnife
//
//  Created by Anton Fresher on 04.09.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

class DummyPageViewController: UIViewController {

    // MARK: - Outlets
    @IBOutlet weak var number: UILabel!

    // MARK: - Public State

    var numberText: String?

    // MARK: - Lifecycle

    override func viewDidAppear(_ animated: Bool) {
        number.text = numberText
    }
}
