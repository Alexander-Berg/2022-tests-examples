//
//  EarthView.swift
//  YxSwissKnife
//
//  Created by Denis Malykh on 03.05.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

class EarthView: UIView {

    static let progressStartVelocity = CGFloat(0.8)
    static let progressStopVelocity = CGFloat(2.0)
    static let rotateVelocity = CGFloat(400.0)
    static let rotateBreakAngle = CGFloat(10.0)

    // MARK: - Public State

    var progress: CGFloat {
        set {
            if isRotating, isIdle {
                isRotating = false
                if let earth = earthLayer {
                    earth.setRotate(earth.rotate + EarthView.rotateBreakAngle, velocity: TimeInterval(EarthView.rotateVelocity), complete: nil)
                }
            }
            if let earth = earthLayer {
                earth.progress = max(0.0, min(newValue, 1.0))
            }
        }
        get { return earthLayer?.progress ?? 0.0 }
    }

    var rotate: CGFloat {
        set { earthLayer?.rotate = newValue }
        get { return earthLayer?.rotate ?? 0.0 }
    }

    var isRotating: Bool = false

    var earthImage: UIImage? {
        set { earthLayer?.earthImage = newValue }
        get { return earthLayer?.earthImage }
    }

    var orbitImage: UIImage? {
        set { earthLayer?.orbitImage = newValue }
        get { return earthLayer?.orbitImage }
    }

    var sputnikImage: UIImage? {
        set { earthLayer?.sputnikImage = newValue }
        get { return earthLayer?.sputnikImage }
    }

    // MARK: - Public

    func startRotationAnimation(_ completion: (() -> Void)? = nil) {
        guard !isRotating, isIdle else {
            completion?()
            return
        }

        isRotating = true
        if let earth = earthLayer {
            earth.setProgress(1.0, velocity: TimeInterval(EarthView.progressStartVelocity)) { [weak self] in
                guard let sself = self else { return }
                sself.loopRotateAnimation()
                completion?()
            }
        }
    }

    func stopRotationAnimation(_ completion: (() -> Void)? = nil) {
        guard isRotating, isIdle else {
            completion?()
            return
        }

        isRotating = false
        isIdle = false

        guard let earth = earthLayer else {
            completion?()
            return
        }

        if numberOfLoops > 1 {
            earth.setProgress(0.0, velocity: TimeInterval(EarthView.progressStopVelocity)) { [weak self] in
                guard let sself = self else { return }
                earth.rotate = 0.0
                sself.numberOfLoops = 0
                sself.isIdle = true
                completion?()
            }
        } else {
            earth.setRotate(earth.rotate + 360.0, velocity: TimeInterval(EarthView.rotateVelocity)) {
                earth.setProgress(0.0, velocity: TimeInterval(EarthView.progressStopVelocity)) { [weak self] in
                    guard let sself = self else { return }
                    earth.rotate = 0.0
                    sself.numberOfLoops = 0
                    sself.isIdle = true
                    completion?()
                }
            }
        }
    }

    // MARK: - Lifecycle

    override init(frame: CGRect) {
        super.init(frame: frame)
        bootstrap()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        bootstrap()
    }

    override class var layerClass: AnyClass {
        return EarthLayer.self
    }

    // MARK: - Private State

    fileprivate var earthLayer: EarthLayer? {
        return self.layer as? EarthLayer
    }

    fileprivate var isIdle = true
    fileprivate var numberOfLoops = 0

    // MARK: - Private

    fileprivate func bootstrap() {
        isRotating = false
        isIdle = true

        backgroundColor = UIColor.clear
        earthLayer?.contentsScale = UIScreen.main.scale
    }

    fileprivate func loopRotateAnimation() {
        guard isRotating else { return }

        numberOfLoops += 1

        if let earth = earthLayer {
            earth.setRotate(earth.rotate + 360.0, velocity: TimeInterval(EarthView.rotateVelocity)) { [weak self] in
                guard let sself = self else { return }
                sself.loopRotateAnimation()
            }
        }
    }

}
