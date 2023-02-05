//
//  ARGuidanceController.swift
//  ARKit-YandexMaps
//
//  Created by Alexander Ermichev on 09/08/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation
import UIKit
import YandexMapKit
import RxSwift
import RxCocoa

@available(iOS 11.0, *)
protocol ARGuidanceController: AnyObject {
    var showDebugOptions: Bool { get set }
    var showDestination: Bool { get set }
    var sceneLocationView: SceneLocationView { get }

    func draw(route: YMKPolyline)
    func clearRoute()
}

@available(iOS 11.0, *)
protocol ARGuidanceControllerImplDeps:
    SceneLocationViewDeps,
    ARDebugScreenViewModelDeps,
    ARLocationCorrectionPresenterDeps
{
    var arRouteController: ARRouteController { get }
    var arDebugSettings: ARDebugSettings { get }
    var arGuidanceStateProvider: ARGuidanceStateProvider { get }
}

@available(iOS 11.0, *)
class ARGuidanceControllerImpl: ARGuidanceController {

    var showDebugOptions: Bool = false {
        didSet {
            debugScreenView.alpha = showDebugOptions ? 1.0 : 0.0
            sceneLocationView.showsStatistics = showDebugOptions
        }
    }

    var showDestination: Bool = true {
        didSet { deps.arRouteController.showDestinationPoint = showDestination }
    }

    private(set) lazy var sceneLocationView: SceneLocationView = {
        let sceneView = SceneLocationView(deps: self.deps)
        sceneView.addListener(self)

        dispatch(after: 0.1) { [weak self] in
            self?.setup(sceneView: sceneView)
        }

        return sceneView
    }()

    init(deps: ARGuidanceControllerImplDeps) {
        self.deps = deps
    }

    func draw(route: YMKPolyline) {
        if sceneLocationView.bestLocationEstimate != nil {
            deps.arRouteController.route = route
        } else {
            neededRoute = route
        }
    }

    func clearRoute() {
        deps.arRouteController.route = nil
    }

    fileprivate let deps: ARGuidanceControllerImplDeps

    fileprivate var route: YMKPolyline?
    fileprivate var neededRoute: YMKPolyline?

    fileprivate var locationCorrectionPresenter: ARLocationCorrectionPresenter?

    fileprivate lazy var debugScreenView = ARDebugScreenView()
    fileprivate lazy var debugScreenViewModel: ARDebugScreenViewModel = {
        var vm = ARDebugScreenViewModel(deps: self.deps)
        vm.destinationAnnotationHeight
            .asObservable()
            .bind { [weak self] height in
                guard let slf = self else { return }
                slf.deps.arRouteController.destinationAnnotationHeight = height
            }
            .disposed(by: self.bag)
        return vm
    }()

    fileprivate lazy var statusLabel: UILabel = {
        return apply(UILabel()) {
            $0.backgroundColor = UIColor.gray.withAlphaComponent(0.5)
            $0.textColor = UIColor.white
            $0.textAlignment = .center
        }
    }()

    fileprivate let bag = DisposeBag()

}

@available(iOS 11.0, *)
extension ARGuidanceControllerImpl: SceneLocationViewListener {

    // MARK: SceneLocationViewListener

    func sceneLocationView(_ sceneLocationView: SceneLocationView,
                           didAddSceneLocationEstimate sceneLocationEstimate: SceneLocationEstimate)
    {
        if let route = neededRoute {
            neededRoute = nil
            deps.arRouteController.route = route
        }
    }

    func sceneLocationView(_ sceneLocationView: SceneLocationView,
                           didRemoveSceneLocationEstimate sceneLocationEstimate: SceneLocationEstimate)
    {
        // Nothing to do here
    }

    func sceneLocationView(_ sceneLocationView: SceneLocationView, didConfirmLocationOfNode node: LocationNode) {
        print("sceneLocationView didConfirmLocationOfNode")
    }

    func sceneLocationView(_ sceneLocationView: SceneLocationView, didUpdateLocationNode locationNode: LocationNode) {
        //print("sceneLocationView didUpdateLocationNode")
    }

    func sceneLocationView(_ sceneLocationView: SceneLocationView, didUpdateGroundLevel groundLevel: Float) {
        print("sceneLocationView didUpdateGroundLevel")
    }

}

@available(iOS 11.0, *)
fileprivate extension ARGuidanceControllerImpl {

    fileprivate func setup(sceneView: SceneLocationView) {
        locationCorrectionPresenter = ARLocationCorrectionPresenter(deps: deps, sceneView: sceneView)
        setupSubviews(for: sceneView)

        sceneView.run()
    }

    fileprivate func setupSubviews(for sceneView: SceneLocationView) {

        apply(debugScreenView) {
            sceneView.implView.addSubview($0)
            $0.translatesAutoresizingMaskIntoConstraints = false
            $0.fillContainer()
            $0.bind(to: debugScreenViewModel)
        }

        sceneView.implView.addSubview(deps.arRouteController.placemarkView)
        sceneView.implView.addSubview(deps.arRouteController.placemarkHelper)

        apply(statusLabel) {
            sceneView.implView.addSubview($0)
            $0.translatesAutoresizingMaskIntoConstraints = false
            $0.attachLeftInContainer(margin: 20.0)
            $0.attachRightInContainer(margin: 20.0)
            $0.attachTopInContainer(margin: 20.0)
            $0.addHeight(50)

            deps.arGuidanceStateProvider.state
                .asDriver()
                .map { $0.descriptionText }
                .drive($0.rx.text)
                .disposed(by: bag)

        }


    }

}
