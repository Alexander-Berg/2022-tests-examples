//
//  DeviceLogOutputPage.swift
//  YandexMaps
//
//  Created by Alexander Goremykin on 09.03.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import UIKit

final class DeviceLogOutputPage: UIView, UIScrollViewDelegate, UITextViewDelegate {

    // MARK: - Public Properties

    var log: NSAttributedString {
        set {
            textView.attributedText = newValue

            if autoscrollEnabled && newValue.length > 0 {
                let bottom = NSRange(location: newValue.length - 1, length: 1)
                textView.scrollRangeToVisible(bottom)
            }
        }
        get {
            return textView.attributedText
        }
    }

    private(set) var paused = false

    var onClear: (() -> Void)?
    var onPause: ((Bool) -> Void)?

    init() {
        super.init(frame: CGRect.zero)
        setupUI()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - UIView

    override var isUserInteractionEnabled: Bool {
        didSet {
            UIView.animate(withDuration: 0.25) {
                self.pauseButton.alpha = self.isUserInteractionEnabled ? DeviceLog.Parameters.activeStateAlpha : 0.0
                self.clearButton.alpha = self.isUserInteractionEnabled ? DeviceLog.Parameters.activeStateAlpha : 0.0
            }
        }
    }

    // MARK: - ScrollViewDelegate

    @objc func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let windowHeight = Double((window?.bounds ?? CGRect.zero).height)
        let offset = Double(scrollView.contentOffset.y)

        let autoOffsetGap = 10.0
        autoscrollEnabled = offset > Double(scrollView.contentSize.height) - windowHeight - autoOffsetGap
    }

    // MARK: - Private

    private let textView = UITextView()
    private let pauseButton = DeviceLogButton.make()
    private let clearButton = DeviceLogButton.make()
    
    private var autoscrollEnabled = true
    
    static func pauseButtonTitle(paused: Bool) -> String {
        return paused ? "▶️" : "⏸"
    }
    
    private func setupUI() {
        apply(textView) { obj in
            addSubview(obj)
            
            obj.delegate = self
            obj.isEditable = false
            obj.panGestureRecognizer.maximumNumberOfTouches = 1
            obj.backgroundColor = UIColor.darkGray
            obj.isUserInteractionEnabled = true
            obj.alpha = DeviceLog.Parameters.activeStateAlpha
        }

        apply(pauseButton) { obj in
            addSubview(obj)

            obj.setTitle(DeviceLogOutputPage.pauseButtonTitle(paused: paused), for: .normal)
            obj.addTarget(self, action: #selector(controlButtonsHandler(_:)), for: .touchUpInside)
        }

        apply(clearButton) { obj in
            addSubview(obj)

            obj.setTitle("🗑", for: .normal)
            obj.addTarget(self, action: #selector(controlButtonsHandler(_:)), for: .touchUpInside)
        }
        
         setupLayout()
    }
    
    private func setupLayout() {
        typealias Params = DeviceLog.Parameters
    
        textView.translatesAutoresizingMaskIntoConstraints = false
        textView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        textView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        textView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        textView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true

        pauseButton.translatesAutoresizingMaskIntoConstraints = false

        clearButton.translatesAutoresizingMaskIntoConstraints = false

        let statusBarHeight: CGFloat = 20.0
        
        for b in [pauseButton, clearButton] {
            let size = DeviceLogButton.preferredSize
            b.widthAnchor.constraint(equalToConstant: size.width).isActive = true
            b.heightAnchor.constraint(equalToConstant: size.height).isActive = true
        }

        pauseButton.topAnchor.constraint(equalTo: topAnchor, constant: Params.contentMargin + statusBarHeight).isActive = true
        pauseButton.leftAnchor.constraint(equalTo: leftAnchor, constant: Params.contentMargin).isActive = true

        clearButton.topAnchor.constraint(equalTo: topAnchor, constant: Params.contentMargin + statusBarHeight).isActive = true

        pauseButton.rightAnchor.constraint(equalTo: clearButton.leftAnchor, constant: -Params.buttonsSpacing).isActive = true

        textView.contentInset = UIEdgeInsets(top: statusBarHeight + 2.0 * Params.contentMargin + 48.0,
                                             left: 0.0, bottom: 0.0, right: 0.0)
    }
    
    @objc private func controlButtonsHandler(_ sender: UIControl) {
        if sender === pauseButton {
            paused = !paused
            pauseButton.setTitle(DeviceLogOutputPage.pauseButtonTitle(paused: paused), for: .normal)
            onPause?(paused)
        } else if sender === clearButton {
            onClear?()
        }
    }
    
}
