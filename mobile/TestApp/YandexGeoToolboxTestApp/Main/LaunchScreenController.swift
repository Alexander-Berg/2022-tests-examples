//
//  LaunchScreenViewController.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 10/09/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import UIKit

class LaunchScreenController {

    private(set) var vc: UIViewController
    private var imageView: UIView? = nil
    
    init(_ vc: UIViewController) {
        self.vc = vc
        self.imageView = vc.view.subviews.filter { $0.restorationIdentifier == "launch_icon" }.first
    }
    
    class func makeFromStoryboard() -> LaunchScreenController? {
        let sb = UIStoryboard(name: "LaunchScreen", bundle: nil)
        
        let vc = sb.instantiateViewController(withIdentifier: "launch_screen_vc")
        return LaunchScreenController(vc)
    }
    
    func dismissAnimation() {
        let imageView = self.imageView
        
        UIView.animate(
            withDuration: 0.25, delay: 0.0, options: UIViewAnimationOptions.curveEaseOut,
            animations: {
                imageView?.layer.transform = CATransform3DMakeScale(0.5, 0.5, 1.0)
            },
            completion: { _ in

                UIView.animate(
                    withDuration: 0.15, delay: 0.0, options: UIViewAnimationOptions.curveEaseIn,
                    animations: {
                        imageView?.layer.transform = CATransform3DMakeScale(2.0, 2.0, 1.0)
                    },
                    completion: { _ in
                    }
                )
            }
        )
    }
    
}
