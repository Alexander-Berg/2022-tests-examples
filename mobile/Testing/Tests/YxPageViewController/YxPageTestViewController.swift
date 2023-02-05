//
//  YxPageTestViewController.swift
//  YxSwissKnife
//
//  Created by Anton Fresher on 04.09.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit
import Foundation

class YxPageTestViewController: UIViewController {

    weak var pager: YxPageViewController! {
        didSet { pager.dataSource = self }
    }

    // MARK: - Private State

    fileprivate var pages: [UIViewController]!

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()

        // NOTE: disabling "swipe to back" gesture
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false

        let storyboard = UIStoryboard(name: "YxPageViewControllerTest", bundle: Bundle.main)

        pages = [1, 2, 3, 4, 5].compactMap { index in
            if let vc = storyboard.instantiateViewController(withIdentifier: "Page") as? DummyPageViewController {
                vc.numberText = "\(index)"
                return vc
            }

            return nil
        }
        pager?.setCurrentControllers(pages[0])
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let vc = segue.destination as? YxPageViewController {
            pager = vc
            return
        }
        super.prepare(for: segue, sender: sender)
    }

}

extension YxPageTestViewController: YxPageViewControllerDataSource {

    func pageViewController(_ pageViewController: YxPageViewController, viewControllerBefore viewController: UIViewController) -> UIViewController? {
        guard
            let current = viewController as? DummyPageViewController,
            let labelText = current.number.text,
            let index = Int(labelText),
            2...5 ~= index
            else {
                return nil
        }
        let arrayIndex = index - 1
        return pages[arrayIndex - 1]
    }

    func pageViewController(_ pageViewController: YxPageViewController, viewControllerAfter viewController: UIViewController) -> UIViewController? {
        guard
            let current = viewController as? DummyPageViewController,
            let labelText = current.number.text,
            let index = Int(labelText),
            1...4 ~= index
            else {
                return nil
        }
        let arrayIndex = index - 1
        return pages[arrayIndex + 1]
    }
}
