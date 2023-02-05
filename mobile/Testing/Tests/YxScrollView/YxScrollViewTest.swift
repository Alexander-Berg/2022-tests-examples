//
//  YxScrollViewTest.swift
//  YxSwissKnife
//
//  Created by Denis Malykh on 03.05.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

class YxScrollViewTest: UIViewController {

    @IBOutlet weak var scroll: YxScrollView! {
        didSet {
            scroll.isTopRefresherBelow = false
            scroll.isTopRefresherEnabled = true

            let refresher = SputnikTopRefresher(frame: .zero)
            refresher.handler = {
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
                    guard let sself = self else { return }
                    sself.scroll.finishRefreshing(animated: true, side: .top)
                }
            }
            refresher.defaultInset = 20
            scroll.topRefresherView = refresher
        }
    }

    @IBOutlet weak var scrollAwareThing: ScrollAwareThing!

    override func viewDidLoad() {
        super.viewDidLoad()
        scroll.awareables.append(scrollAwareThing)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationItem.title = "YxScrollView"
    }
}
