//
//  ViewController.swift
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 11/04/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import UIKit
import YandexDataSync

class MainViewController: UIViewController {
    
    fileprivate struct Section {
        struct Row {
            var title: String
            var action: () -> Void
        }
        var title: String? = nil
        var rows: [Row] = []
    }
    
    fileprivate let appCtx: AppContext
    fileprivate let accountManager: AccountManager?
    fileprivate let databaseManager: YDSDatabaseManager
    
    fileprivate var sections: [Section] = []
    
    init(appCtx: AppContext) {
        self.appCtx = appCtx
        
        accountManager = appCtx.accountManager.value
        databaseManager = YDSDatabaseManagerInstance.value()
        
        super.init(nibName: nil, bundle: nil)
        
        databaseManager.initialize(withUuid: "0", deviceId: "0")
        title = "Toolbox"
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    class func makeInNavigation(usingAppCtx ctx: AppContext) -> (nav: UINavigationController, vc: MainViewController) {
        let vc = MainViewController(appCtx: ctx)
        let nav = UINavigationController(rootViewController: vc)
        
        return (nav: nav, vc: vc)
    }
    
    // MARK: UI
    
    fileprivate weak var tableView: UITableView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.white
        
        tableView = apply(UITableView(frame: view.bounds, style: .grouped)) { tv in
            view.addSubview(tv)
            tv.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            
            tv.dataSource = self
            tv.delegate = self
            
            tv.register(MainTableCell.self, forCellReuseIdentifier: NSStringFromClass(MainTableCell.self))
        }
        setupElements()
    }

    fileprivate func setupElements() {
        
        // Sync
        var syncSection = Section()
        
        syncSection.title = "Datasync"
        syncSection.rows.append(Section.Row(title: "Bookmarks & History") { [weak self] in
            guard let slf = self else { return }
            
            let vc = SyncViewController(
                databaseManager: slf.appCtx.databaseManager, accountManager: slf.appCtx.accountManager.value)
            
            slf.navigationController?.pushViewController(vc, animated: true)
        })

        // Masstransit
        var masstransitSection = Section()
        
        masstransitSection.title = "Masstransit"
        masstransitSection.rows.append(Section.Row(title: "Moving vehicles (xml)") { [weak self] in
            guard let slf = self else { return }
            let vc = MovingVehiclesViewController.Deps(appCtx: slf.appCtx).make(with: .xml)
            slf.navigationController?.pushViewController(vc, animated: true)
        })
        masstransitSection.rows.append(Section.Row(title: "Moving vehicles (mapkit)") { [weak self] in
            guard let slf = self else { return }
            let vc = MovingVehiclesViewController.Deps(appCtx: slf.appCtx).make(with: .mapkit)
            slf.navigationController?.pushViewController(vc, animated: true)
        })
        
        sections = [
            masstransitSection,
            syncSection
        ]
        tableView.reloadData()
    }
}


extension MainViewController: UITableViewDataSource {

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(
            withIdentifier: NSStringFromClass(MainTableCell.self), for: indexPath)
        
        if let cell = cell as? MainTableCell {
            let row = sections[indexPath.section].rows[indexPath.row]
            
            cell.update(title: row.title)
            cell.accessoryType = .disclosureIndicator
        }
        return cell
    }

    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sections[section].rows.count
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return sections[section].title
    }
    
}


extension MainViewController: UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 50.0
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)

        let row = sections[indexPath.section].rows[indexPath.row]
        row.action()
    }
    
}

