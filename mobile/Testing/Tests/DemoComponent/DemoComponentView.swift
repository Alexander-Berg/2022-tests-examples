//  Created by Denis Malykh on 06.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

protocol DemoComponentViewDelegate: AnyObject {
    func didTapAcquireInfo()
}

final class DemoComponentView: UIView {
    struct ViewModel {
        let infoText: String
        let observedText: String
    }

    private let infoLabel = UILabel()
    private let observingLabel = UILabel()
    private let acquireInfoButton = UIButton()

    weak var delegate: DemoComponentViewDelegate?

    private var viewModel = ViewModel(
        infoText: "--",
        observedText: "--"
    ) {
        didSet {
            infoLabel.text = viewModel.infoText
            observingLabel.text = viewModel.observedText
        }
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }

    // Can be moved to special struct *Layout
    override func layoutSubviews() {
        super.layoutSubviews()

        let availableSize = CGSize(
            width: bounds.width - 2.0 * margins,
            height: UIView.layoutFittingCompressedSize.height
        )

        let top: CGFloat
        if #available(iOS 11, *) {
            top = safeAreaInsets.top
        } else {
            top = 20
        }

        acquireInfoButton.frame = CGRect(
            x: margins,
            y: top + margins,
            width: availableSize.width,
            height: acquireInfoButton.intrinsicContentSize.height
        )

        var requiredSize = infoLabel.systemLayoutSizeFitting(
            availableSize,
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )

        infoLabel.frame = CGRect(
            x: margins,
            y: acquireInfoButton.frame.maxY + margins,
            width: availableSize.width,
            height: requiredSize.height
        )

        requiredSize = observingLabel.systemLayoutSizeFitting(
            availableSize,
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )

        observingLabel.frame = CGRect(
            x: margins,
            y: infoLabel.frame.maxY + margins,
            width: availableSize.width,
            height: requiredSize.height
        )
    }

    private func setup() {
        backgroundColor = .white
        
        [infoLabel, observingLabel, acquireInfoButton].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = true
            addSubview($0)
        }

        acquireInfoButton.setTitle("Acquire Info", for: .normal)
        acquireInfoButton.setTitleColor(.black, for: .normal)
        acquireInfoButton.contentHorizontalAlignment = .left
        acquireInfoButton.contentEdgeInsets = UIEdgeInsets(top: half, left: half, bottom: half, right: half)
        acquireInfoButton.layer.borderColor = UIColor.black.withAlphaComponent(0.5).cgColor
        acquireInfoButton.layer.borderWidth = 1.0 / UIScreen.main.scale
        acquireInfoButton.layer.cornerRadius = half

        // here overall setup

        infoLabel.text = viewModel.infoText
        observingLabel.text = viewModel.observedText
        acquireInfoButton.addTarget(self, action: #selector(didTapAcquireInfo), for: .touchUpInside)
    }

    func modify(transform: (ViewModel) -> ViewModel) {
        viewModel = transform(viewModel)
    }

    @objc
    func didTapAcquireInfo() {
        delegate?.didTapAcquireInfo()
    }

}

private let margins = CGFloat(16)
private let half = 0.5 * margins
