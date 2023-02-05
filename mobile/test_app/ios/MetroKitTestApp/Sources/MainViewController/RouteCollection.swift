//
//  RouteCollection.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 23/03/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import UIKit

final class RouteCollection: NSObject {

    init(container: UIView) {
        self.container = container
        
        collectionLayout = apply(CollectionLayout()) {
            $0.scrollDirection = .horizontal
            $0.minimumLineSpacing = Static.collectionInset
            $0.minimumInteritemSpacing = 0.0
        }
        
        let collectionFrame = CGRect(x: 0.0, y: container.bounds.height - Static.collectionHeight,
            width: container.bounds.width, height: Static.collectionHeight)
        
        collectionView = CollectionView(frame: collectionFrame, collectionViewLayout: collectionLayout)
        
        super.init()
        
        apply(collectionView) {
            $0.autoresizingMask = [.flexibleTopMargin, .flexibleWidth]
            $0.backgroundColor = .clear
            $0.delegate = self
            $0.dataSource = self
            $0.register(RouteCell.self, forCellWithReuseIdentifier: RouteCell.identifier)
            $0.alwaysBounceHorizontal = true
            
            $0.contentInset = UIEdgeInsets(
                top: Static.collectionInset, left: Static.collectionInset, bottom: Static.collectionInset,
                right: Static.collectionInset)
            
            container.addSubview($0)
        }
    }
    
    var routingResult: YMLRoutingResult? = nil {
        didSet {
            collectionView.reloadData()
        }
    }
    
    var onShowRoute: ((_ index: Int) -> Void)? = nil
    
    var contentInsets: UIEdgeInsets {
        if routingResult == nil {
            return .zero
        } else {
            return UIEdgeInsets(top: 0, left: 0, bottom: Static.collectionHeight, right: 0)
        }
    }
    
    // MARK: Private
    
    private struct Static {
        static let collectionInset = CGFloat(12)
        static let cellHeight = CGFloat(70)
        static let collectionHeight = cellHeight + 2 * collectionInset
    }
    
    private class CollectionView: UICollectionView {
        override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
            let v = super.hitTest(point, with: event)
            return v == self ? nil : v
        }
    }
    
    private class CollectionLayout: UICollectionViewFlowLayout {}
    
    private let container: UIView
    private let collectionView: CollectionView
    private let collectionLayout: CollectionLayout
    
    private func allRoutes() -> [YMLRoute] {
        return (routingResult?.main ?? []) + (routingResult?.additional ?? [])
    }
    
    private func route(at index: Int) -> YMLRoute? {
        let routes = allRoutes()
        
        if index >= routes.count {
            return nil
        }
        return routes[index]
    }
    
}

extension RouteCollection: UICollectionViewDelegateFlowLayout, UICollectionViewDataSource {

    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return allRoutes().count
    }
    
    func collectionView(
        _ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout,
        sizeForItemAt indexPath: IndexPath) -> CGSize
    {
        return CGSize(width: container.bounds.width * 0.75, height: Static.cellHeight)
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath)
        -> UICollectionViewCell
    {
        let cell = collectionView.dequeueReusableCell(
            withReuseIdentifier: RouteCell.identifier, for: indexPath) as! RouteCell
        
        if let route = route(at: indexPath.item) {
            let hh = route.time / (60 * 60)
            let mm = route.time / 60 % 60;
            let ss = route.time % 60;
        
            var s = ""
            if hh > 0 {
                s += "\(hh) h "
            }
            if mm > 0 {
                s += "\(mm) min "
            }
            if ss > 0 {
                s += "\(ss) sec "
            }
        
            let time = NSAttributedString(
                string: s,
                attributes: [.font: UIFont.boldSystemFont(ofSize: 16), .foregroundColor: UIColor.black])
            
            let transfers = NSAttributedString(
                string: "\n\(route.transfersCount) transfers",
                attributes: [.font: UIFont.boldSystemFont(ofSize: 14), .foregroundColor: UIColor.lightGray])
            
            let text = NSMutableAttributedString()
            text.append(time)
            text.append(transfers)
            
            cell.attributedText = text
        }
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        collectionView.deselectItem(at: indexPath, animated: true)
        onShowRoute?(indexPath.item)
    }
    
}
