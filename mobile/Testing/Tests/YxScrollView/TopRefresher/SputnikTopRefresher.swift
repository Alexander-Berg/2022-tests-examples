//
//  SputnikTopRefresher.swift
//  YxSwissKnife
//
//  Created by Denis Malykh on 03.05.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

class SputnikTopRefresher: YxScrollViewRefresher {

    static let smallSize = CGFloat(100.0)
    static let bigSize = CGFloat(130.0)
    static let smallThreshold = CGFloat(90.0)
    static let bigThreshold = CGFloat(120.0)
    static let progressMin = CGFloat(0.6)
    static let progressMax = CGFloat(1.4)

    // MARK: - State

    private(set) var earthView: EarthView!

    // MARK: - Lifecycle

    override func bootstrap() {
        super.bootstrap()

        backgroundColor = UIColor.clear
        clipsToBounds = true

        // TODO: check for large iphones (bigSize + bigThreshold)
        size = SputnikTopRefresher.smallSize
        threshold = SputnikTopRefresher.smallThreshold
        releaseTrigger = true

        earthView = EarthView(frame: CGRect(x: 0, y: 0, width: size, height: size))
        earthView.earthImage = UIImage(named: "p2r_earth")
        earthView.orbitImage = UIImage(named: "p2r_orbit")
        earthView.sputnikImage = UIImage(named: "p2r_sputnik")
        addSubview(earthView)
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        earthView.frame = CGRect(
            x: 0.5 * (bounds.size.width - size),
            y: 0.5 * (bounds.size.height - size),
            width: size, height: size)
    }

    // MARK: - YxScrollViewRefresher

    override func didUpdate(to percent: CGFloat) {
        super.didUpdate(to: percent)

        switch state {
        case .calm, .drag:
            earthView.progress = percent >= SputnikTopRefresher.progressMin
                ? (percent - SputnikTopRefresher.progressMin) / (SputnikTopRefresher.progressMax - SputnikTopRefresher.progressMin)
                : 0.0

        default: break
        }
    }

    override func didEndDragging() {
        super.didEndDragging()
        earthView.startRotationAnimation()
    }

    override func hide(animated: Bool, velocity: CGFloat, completion: YxScrollViewRefresher.CompletionBlock?) {
        earthView.stopRotationAnimation {
            super.hide(animated: animated, velocity: velocity, completion: completion)
        }
    }
}
