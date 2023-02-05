//
//  SchemeListCell.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 08/12/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import UIKit

final class SchemeListCell: UITableViewCell {

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        setupViews()
        setupLayout()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func update(with info: SchemeListCellInfo) {
        self.info = info
        
        titleLabel.text = info.title
        idLabel.text = info.id
        sizeLabel.text = info.size
        
        switch info.state {
        case .normal:
            updateButton.isHidden = true
            activityIndicator.isHidden = true
        case .update:
            updateButton.isHidden = false
            updateButton.setTitle("Update", for: .normal)
            activityIndicator.isHidden = true
        case .download:
            updateButton.isHidden = false
            updateButton.setTitle("Download", for: .normal)
            activityIndicator.isHidden = true
        case .loading:
            updateButton.isHidden = true
            activityIndicator.isHidden = false
            activityIndicator.startAnimating()
        }
    }
    
    // MARK: - Private
    
    private var info: SchemeListCellInfo?
    
    private let titleLabel = UILabel()
    private let idLabel = UILabel()
    private let sizeLabel = UILabel()
    private let updateButton = UIButton(type: .system)
    private let activityIndicator = UIActivityIndicatorView(style: .gray)
    
    private func setupViews() {
        apply(titleLabel) { v in
            contentView.addSubview(v)
            v.font = .preferredFont(forTextStyle: .headline)
        }
        
        apply(idLabel) { v in
            contentView.addSubview(v)
            v.font = .preferredFont(forTextStyle: .subheadline)
            v.textColor = .gray
        }
        
        apply(sizeLabel) { v in
            contentView.addSubview(v)
            v.font = .preferredFont(forTextStyle: .subheadline)
            v.textColor = .gray
        }
        
        apply(updateButton) { v in
            contentView.addSubview(v)
            v.titleLabel?.font = .preferredFont(forTextStyle: .headline)
            v.addTarget(self, action: #selector(handleUpdateButton), for: .touchUpInside)
        }
        
        apply(activityIndicator) { v in
            contentView.addSubview(v)
        }
    }
    
    private func setupLayout() {
        updateButton.translatesAutoresizingMaskIntoConstraints = false
        updateButton.rightAnchor.constraint(equalTo: contentView.rightAnchor, constant: -16.0).isActive = true
        updateButton.topAnchor.constraint(equalTo: contentView.topAnchor).isActive = true
        updateButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        updateButton.setContentCompressionResistancePriority(.required, for: .horizontal)
        updateButton.setContentHuggingPriority(.required, for: .horizontal)
        
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        activityIndicator.leftAnchor.constraint(equalTo: contentView.rightAnchor, constant: -16.0).isActive = true
        activityIndicator.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        let labels = [titleLabel, idLabel, sizeLabel]
        
        for (index, l) in labels.enumerated() {
            l.translatesAutoresizingMaskIntoConstraints = false
            l.leftAnchor.constraint(equalTo: contentView.leftAnchor, constant: 16.0).isActive = true
            updateButton.leftAnchor.constraint(equalTo: l.rightAnchor, constant: 16.0)
            
            if index > 0 {
                l.topAnchor.constraint(equalTo: labels[index - 1].bottomAnchor, constant: 4.0).isActive = true
            }
        }
        
        labels.first?.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8.0).isActive = true
        labels.last?.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8.0).isActive = true
    }
    
    @objc private func handleUpdateButton() {
        guard let info = info else { return }
    
        switch info.state {
        case .update(let block):
            block()
        case .download(let block):
            block()
        case .loading, .normal:
            assertionFailure()
        }
    }
}
