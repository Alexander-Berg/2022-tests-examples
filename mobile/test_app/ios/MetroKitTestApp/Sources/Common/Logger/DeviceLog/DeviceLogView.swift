//
//  DeviceLogView.swift
//  YandexMaps
//
//  Created by Alexander Goremykin on 28.04.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit

final class DeviceLogView: UIView, UIScrollViewDelegate {

    typealias OutputPage = DeviceLogOutputPage
    typealias SettingsPage = DeviceLogSettingsPage

    enum State {
        case closed
        case log
        case settings
    }

    var onWillOpenCallback: (() -> Void)?

    let logPageView = OutputPage()
    let settingsPageView = SettingsPage()

    private(set) var state: State = .closed {
        didSet {
            guard oldValue != state else { return }

            switch state {
            case .log:
                onWillOpenCallback?()
                updateControlsVisibility()
            case .settings:
                updateControlsVisibility()
            case .closed:
                break
            }
        }
    }

    init() {
        super.init(frame: CGRect.zero)

        setupUI()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func hide(animated: Bool) {
        state = .closed

        UIView.animate(withDuration: animated ? 0.25 : 0.0,
            animations: {
                self.setNeedsLayout()
                self.layoutIfNeeded()
            },
            completion: { _ in
                self.isHidden = true
            })
    }

    // MARK: - UIView

    override func layoutSubviews() {
        super.layoutSubviews()
        layoutScrollView()
        layoutPages()
    }

    override func willMove(toSuperview newSuperview: UIView?) {
        super.willMove(toSuperview: newSuperview)

        if let superview = newSuperview {
            superview.addGestureRecognizer(scrollView.panGestureRecognizer)

            doubleTapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(doubleTapHandler))
            doubleTapGestureRecognizer.numberOfTapsRequired = 2
            doubleTapGestureRecognizer.numberOfTouchesRequired = 3
            superview.addGestureRecognizer(doubleTapGestureRecognizer)
        } else {
            if let superview = self.superview {
                superview.removeGestureRecognizer(scrollView.panGestureRecognizer)
                superview.removeGestureRecognizer(doubleTapGestureRecognizer)
                doubleTapGestureRecognizer = nil
            }
        }
    }

    // MARK: - ScrollViewDelegate

    @objc func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        scrollingType = .user
        isHidden = false
    }

    @objc func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let offset = Double(scrollView.contentOffset.y)

        switch scrollingType {
        case .user:
            if offset >= Double(bounds.height) * Double(Parameters.numberOfPages) {
                state = .closed
            } else if offset >= Double(bounds.height) * Double(Parameters.numberOfPages - 1) {
                state = .log
            } else {
                state = .settings
            }

            layoutPages()

            if state == .settings {
                settingsPageView.reloadData()
            }

        case .system:
            break
        }
    }

    @objc func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        guard scrollingType == .user else { return }

        scrollingType = .system
        isHidden = state == .closed
    }
    
    // MARK: - Private
    
    private enum ScrollingType {
        case user
        case system
    }
    
    private typealias Parameters = DeviceLog.Parameters

    private let scrollView = UIScrollView()
    private var scrollingType: ScrollingType = .system

    private var closeButton = DeviceLogButton.make()

    private var doubleTapGestureRecognizer: UITapGestureRecognizer!
    
    private func setupUI() {
        isHidden = true

        apply(scrollView) { obj in
            addSubview(obj)
    
            obj.isUserInteractionEnabled = false
            obj.bounces = true
            obj.showsVerticalScrollIndicator = false
            obj.showsHorizontalScrollIndicator = false
            obj.isPagingEnabled = true
            obj.scrollsToTop = false
            obj.delegate = self

            obj.panGestureRecognizer.minimumNumberOfTouches = 3
            obj.panGestureRecognizer.maximumNumberOfTouches = 3

            if #available(iOS 11.0, *) {
                obj.contentInsetAdjustmentBehavior = .never
            }
        }

        apply(logPageView) { obj in
            addSubview(obj)
        }

        apply(settingsPageView) { obj in
            addSubview(obj)
        }

        apply(closeButton) { obj in
            addSubview(obj)
    
            obj.setTitle("❌", for: .normal)
            obj.addTarget(self, action: #selector(controlButtonsHandler(_:)), for: .touchUpInside)
        }
    
        setupLayout()
    }
    
    private func setupLayout() {
        let statusBarHeight: CGFloat = 20
        
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.topAnchor.constraint(equalTo: topAnchor, constant: statusBarHeight + Parameters.contentMargin).isActive = true
        closeButton.rightAnchor.constraint(equalTo: rightAnchor, constant: -Parameters.contentMargin).isActive = true
        closeButton.widthAnchor.constraint(equalToConstant: DeviceLogButton.preferredSize.width).isActive = true
        closeButton.heightAnchor.constraint(equalToConstant: DeviceLogButton.preferredSize.height).isActive = true
    }

    private func layoutScrollView() {
        var targetFrame = window?.bounds ?? CGRect.zero
        targetFrame.size.height *= Parameters.logWindowHeightFraction

        var contentSize = targetFrame.size
        contentSize.height *= CGFloat(Parameters.numberOfPages + 1)
        scrollView.contentSize = contentSize

        switch state {
        case .closed:
            scrollView.contentOffset.y = targetFrame.height * CGFloat(Parameters.numberOfPages)
        case .log:
            scrollView.contentOffset.y = targetFrame.height * CGFloat(Parameters.numberOfPages - 1)
        case .settings:
            scrollView.contentOffset.y = CGFloat(Parameters.numberOfPages - 2)
        }

        scrollView.frame.size = targetFrame.size
    }

    private func layoutPages() {
        let targetFrame = window?.bounds ?? CGRect.zero
        let pageHeight = targetFrame.height * Parameters.logWindowHeightFraction

        if scrollView.contentOffset.y < 0.0 {
            settingsPageView.frame.origin.y = 0.0
        } else {
            settingsPageView.frame.origin.y = -scrollView.contentOffset.y
        }

        if scrollView.contentOffset.y < pageHeight {
            logPageView.frame.origin.y = 0.0
        } else {
            logPageView.frame.origin.y = pageHeight - scrollView.contentOffset.y
        }

        ([logPageView, settingsPageView] as [UIView]).forEach {
            $0.frame.origin.x = 0.0
            $0.frame.size.width = targetFrame.width
            $0.frame.size.height = targetFrame.height * Parameters.logWindowHeightFraction
        }

        settingsPageView.isHidden = state != .settings
    }

    private func updateControlsVisibility() {
        let uiEnabled = isUserInteractionEnabled

        UIView.animate(withDuration: 0.25) {
            self.logPageView.alpha = uiEnabled ? Parameters.activeStateAlpha : Parameters.transparentStateAlpha
            self.closeButton.alpha = (self.state == .settings || uiEnabled) ? Parameters.activeStateAlpha : 0.0
        }
    }

    @objc private func doubleTapHandler() {
        if state == .settings {
            state = .log
        }

        isUserInteractionEnabled = !isUserInteractionEnabled
        updateControlsVisibility()
    }

    @objc private func controlButtonsHandler(_ sender: UIControl) {
        hide(animated: true)
    }

}
