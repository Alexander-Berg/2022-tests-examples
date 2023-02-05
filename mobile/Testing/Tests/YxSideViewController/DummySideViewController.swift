//
//  DummySideViewController.swift
//  YxSwissKnife
//
//  Created by Anton Fresher on 04.09.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

class DummySideViewController: UIViewController {

    @IBAction func back(_ sender: UIButton) {
        navigationController?.popViewController(animated: true)
    }

}
