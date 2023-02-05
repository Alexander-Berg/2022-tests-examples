//
//  SchemeListViewController.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 08/12/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import UIKit

final class SchemeListViewController: UIViewController {

    var onSelectScheme: ((YMLSchemeSummary) -> Void)?
    
    init(schemeManager: SchemeManager) {
        self.viewModel = DefaultSchemeListViewModel(schemeManager: schemeManager)
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - UIViewController
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.backgroundColor = .white
        
        viewModel.addListener(self)
        
        setupViews()
        setupLayout()
        setupNavBar()

        updateList()
    }
    
    // MARK: - Private
    
    private let viewModel: SchemeListViewModel
    
    private lazy var tableView = UITableView()
    private lazy var spinnerView = UIActivityIndicatorView(style: .gray)
    
    private struct Static {
        static let cellId = NSStringFromClass(SchemeListCell.self)
    }
    
    private func setupViews() {
        apply(tableView) { v in
            view.addSubview(v)
            
            v.delegate = self
            v.dataSource = self
            
            v.register(SchemeListCell.self, forCellReuseIdentifier: Static.cellId)
            
            v.backgroundColor = .white
            v.alwaysBounceVertical = true
            v.estimatedRowHeight = 40.0
        }
        
        apply(spinnerView) { v in
            view.addSubview(v)
            v.isHidden = true
            v.isUserInteractionEnabled = false
        }
    }
    
    private func setupLayout() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        
        spinnerView.translatesAutoresizingMaskIntoConstraints = false
        spinnerView.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        spinnerView.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
    }
    
    private func updateList() {
//        spinnerView.isHidden = false
//        spinnerView.startAnimating()
//        tableView.isUserInteractionEnabled = false
        
        viewModel.update {
//            self?.spinnerView.isHidden = true
//            self?.tableView.isUserInteractionEnabled = true
        }
    }
    
    private func setupNavBar() {
        navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Cancel", style: .plain, target: self,
            action: #selector(handleCancelBarButton))
    }
    
    @objc private func handleCancelBarButton() {
        dismiss(animated: true, completion: nil)
    }
    
}


extension SchemeListViewController: SchemeListViewModelListener {
   
    func viewModelDidUpdate(_ model: SchemeListViewModel) {
        tableView.reloadData()
    }

}


extension SchemeListViewController: UITableViewDataSource, UITableViewDelegate {

    func numberOfSections(in tableView: UITableView) -> Int {
        return viewModel.numberOfSections
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.numberOfItems(inSection: section)
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: Static.cellId, for: indexPath) as! SchemeListCell
        
        cell.update(with: viewModel.cellInfo(at: indexPath))
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return viewModel.title(forSection: section)
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        onSelectScheme?(viewModel.schemeSummary(at: indexPath))
        dismiss(animated: true, completion: nil)
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }
    
}
