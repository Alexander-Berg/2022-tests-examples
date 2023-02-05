//
//  SchemeListViewModel.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 08/12/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation

protocol SchemeListViewModelListener: class {
    func viewModelDidUpdate(_ model: SchemeListViewModel)
}

protocol SchemeListViewModel: class {
    var numberOfSections: Int { get }
    
    func title(forSection section: Int) -> String
    func numberOfItems(inSection section: Int) -> Int
    
    func cellInfo(at indexPath: IndexPath) -> SchemeListCellInfo
    func schemeSummary(at indexPath: IndexPath) -> YMLSchemeSummary
    func update(completion: (() -> Void)?)
    
    func addListener(_ listener: SchemeListViewModelListener)
    func removeListener(_ listener: SchemeListViewModelListener)
}


final class DefaultSchemeListViewModel {
    
    struct Item {
        var cellInfo: SchemeListCellInfo
        var schemeSummary: YMLSchemeSummary
    }
    
    struct Section {
        var title: String
        var items: [Item]
    }
    
    init(schemeManager: SchemeManager) {
        self.schemeManager = schemeManager
        sections = []
        updateListAsync = nil
        updateSchemeAsyncs = [:]
        notifier = Notifier<SchemeListViewModelListener>()
    }
    
    private let schemeManager: SchemeManager
    private var sections: [Section]
    private var updateListAsync: Async<YMLSchemeList?>?
    private var updateSchemeAsyncs: [YMLSchemeId: Async<YMLScheme?>]
    private var notifier: Notifier<SchemeListViewModelListener>
}


extension DefaultSchemeListViewModel: SchemeListViewModel {
    
    var numberOfSections: Int {
        return sections.count
    }
    
    func title(forSection section: Int) -> String {
        return sections[section].title
    }
    
    func numberOfItems(inSection section: Int) -> Int {
        return sections[section].items.count
    }
    
    func cellInfo(at indexPath: IndexPath) -> SchemeListCellInfo {
        assert(Thread.isMainThread)
        
        return sections[indexPath.section].items[indexPath.row].cellInfo
    }
    
    func schemeSummary(at indexPath: IndexPath) -> YMLSchemeSummary {
        assert(Thread.isMainThread)
        
        return sections[indexPath.section].items[indexPath.row].schemeSummary
    }
    
    func update(completion: (() -> Void)?) {
        assert(Thread.isMainThread)
        
        updateListAsync = apply(schemeManager.updateSchemeList()) {
            $0.onCompletion { [weak self] list in
                guard let slf = self else { return }
                
                slf.updateItems(list: list)
                slf.notifier.forEach {
                    $0.viewModelDidUpdate(slf)
                }
                
                completion?()
            }
            
            $0.start()
        }
    }
    func addListener(_ listener: SchemeListViewModelListener) {
        notifier.subscribe(listener)
    }
    
    func removeListener(_ listener: SchemeListViewModelListener) {
        notifier.unsubscribe(listener)
    }
    
    
    private func updateItems(list: YMLSchemeList?) {
        assert(Thread.isMainThread)
        
        guard let listItems = list?.items else {
            sections = []
            return
        }
        
        sections = listItems.map { countryItem in
            let items = countryItem.schemes.map { makeItem(summary: $0) }
            return Section(title: countryItem.countryCode.value, items: items)
        }
    }
    
    private func makeItem(summary: YMLSchemeSummary) -> Item {
        let language = YMLLanguage(value: YXPlatformCurrentState.currentLanguage())
        let title: String = YMLL10nManager.getStringWith(summary.name, language: language) ?? summary.defaultAlias
        let id = "id: \(summary.schemeId.value as String)"
        let size = (summary.updateInfo as YMLUpdateInfo?).map { String(format: "%.3f KB", Float($0.downloadSize) / 1024.0) }
        let state: SchemeListCellInfo.State

        if updateSchemeAsyncs[summary.schemeId] != nil {
            state = .loading
        } else if summary.updateInfo != nil {
            let updateBlock: () -> Void = { [weak self] in
                guard let slf = self else { return }
                
                let async = slf.schemeManager.updateScheme(id: summary.schemeId)
                
                async.onCompletion { [weak self, id = summary.schemeId] scheme in
                    self?.updateSchemeAsyncs[id] = nil
                    self?.update(completion: nil)
                }
                
                slf.updateSchemeAsyncs[summary.schemeId] = async
                slf.update(completion: nil)
                
                async.start()
            }
            
            if summary.isLocal {
                state = .update(updateBlock)
            } else {
                state = .download(updateBlock)
            }
        } else {
            state = .normal
        }

        let cellInfo = SchemeListCellInfo(title: title, id: id, size: size, state: state)
        
        return Item(cellInfo: cellInfo, schemeSummary: summary)
    }
    
}
