//
//  MainViewController.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 20/01/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit
import YandexDataSync
import YandexRuntime

class SyncViewController: UIViewController {

    private let databaseManager: YDSDatabaseManager
    private let accountManager: AccountManager
    fileprivate let mainViewModel: SyncViewModel
    private var account: YRTAccount? = nil
    
    init(databaseManager: YDSDatabaseManager, accountManager: AccountManager) {
        self.databaseManager = databaseManager
        self.accountManager = accountManager
        self.mainViewModel = DefaultSyncViewModel(databaseManager: databaseManager,
            kvStorage: UserDefaults.standard.makeScoped("sync"), account: account)
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: UI
    
    private weak var tableView: UITableView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        title = "Sync"
        view.backgroundColor = UIColor.white
        
        setupNavigationBar()
        setupTableView()
        
        mainViewModel.onDidUpdate = { [weak self] in
            self?.tableView.reloadData()
        }
        
        mainViewModel.update()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Navigation bar 
    
    private func setupNavigationBar() {
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: "Login", style: .plain, target: self,
            action: #selector(SyncViewController.handleLoginBarButtonItem(item:)))
    }
    
    private func setupTableView() {
        let tv = UITableView(frame: view.bounds, style: .grouped)
        view.addSubview(tv)
        tv.autoresizingMask = [.flexibleHeight, .flexibleWidth]
        
        tv.dataSource = self
        tv.delegate = self
        
        tableView = tv
    }
    
    func handleLoginBarButtonItem(item: UIBarButtonItem) {
        accountManager.authorizationPresenter?.showAuthorization(passwordRequired: true) { [weak self] account, error in
            let account = self?.accountManager.selectedAccountController >>= YRTAccountTokenController.init
            self?.mainViewModel.account = account
        }
    }

}


extension SyncViewController: UITableViewDelegate, UITableViewDataSource {
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return mainViewModel.numberOfSections
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return mainViewModel.numberOfItems(section: section)
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let reuseIdentifier = NSStringFromClass(SyncTableViewCell.self)
        let cell: SyncTableViewCell
        
        if let c = tableView.dequeueReusableCell(withIdentifier: reuseIdentifier) as? SyncTableViewCell {
            cell = c
        } else {
            cell = SyncTableViewCell(style: .default, reuseIdentifier: reuseIdentifier)
        }
        
        cell.update(info: mainViewModel.cellInfo(indexPath: indexPath))
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return mainViewModel.headerTitle(section: section)
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        let info = mainViewModel.cellInfo(indexPath: indexPath)
        return SyncTableViewCell.height(width: tableView.frame.size.width, info: info)
    }
    
}

