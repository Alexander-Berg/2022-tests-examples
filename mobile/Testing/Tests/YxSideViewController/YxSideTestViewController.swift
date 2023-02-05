//
//  YxSideTestViewController.swift
//  YxSwissKnife
//
//  Created by Anton Fresher on 31.08.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import Foundation

class YxSideTestViewController: YxSideViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // NOTE: disabling "swipe to back" gesture
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false

        // NOTE: hiding navigation bar
        navigationController?.isNavigationBarHidden = true

        let storyboard = UIStoryboard(name: "YxSideViewControllerTest", bundle: Bundle.main)

        leftController = storyboard.instantiateViewController(withIdentifier: "Left")
        rootController = storyboard.instantiateViewController(withIdentifier: "Root")
        rightController = storyboard.instantiateViewController(withIdentifier: "Right")
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillAppear(animated)

        // NOTE: enabling "swipe to back" gesture
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true

        // NOTE: enabling navigation bar
        navigationController?.isNavigationBarHidden = false
    }
}
