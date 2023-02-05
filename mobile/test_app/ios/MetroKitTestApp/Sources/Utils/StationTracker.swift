//
//  TrackingStation.swift
//  MetroKitTestApp
//
//  Created by Ilya Lobanov on 10/11/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation

class StationTracker: NSObject, YMLTrackingObjectListener {

    struct PlacemarkInfo {
        typealias PinDirection = YMLSurfaceObjectPinDirection
    
        struct Image {
            var image: UIImage
            var anchor: YMLPoint
        }
    
        enum ImageInfo {
            case normal(image: Image)
            case directed(images: [PinDirection: Image])
        }
    
        enum Style {
            case plane(images: [YMLZoomLevel: ImageInfo])
            case screen(image: ImageInfo)
            
            var scaleMode: YMLPlacemarkSurfaceObjectScaleMode {
                switch self {
                case .plane:
                    return .scheme
                case .screen:
                    return .screen
                }
            }
        }
        
        var style: Style
        var z: Float
    }
    
    let stationId: String
    let trackingObj: YMLTrackingObject
    let placemark: YMLPlacemarkSurfaceObject
    let placemarkInfo: PlacemarkInfo
    let collection: YMLSurfaceObjectCollection

    init(stationId: String, trackingObj: YMLTrackingObject, placemark: YMLPlacemarkSurfaceObject,
        placemarkInfo: PlacemarkInfo, collection: YMLSurfaceObjectCollection)
    {
        self.stationId = stationId
        self.trackingObj = trackingObj
        self.placemark = placemark
        self.placemarkInfo = placemarkInfo
        self.collection = collection
        
        super.init()
        
        trackingObj.setSelectedWithSelected(true, animation: Default.animation)
        trackingObj.addListener(with: self)
        
        updatePlacemark()
    }

    deinit {
        trackingObj.setSelectedWithSelected(false, animation: Default.animation)
        if collection.isValid && placemark.isValid {
            collection.remove(with: placemark)
        }
    }

    // MARK: YMLTrackingObjectListener

    func didChangeSurfaceObject() {
        updatePlacemark()
    }
    
    // MARK: Private
    
    private func updatePlacemark() {
        guard let surfaceObject = trackingObj.currentSurfaceObject else { return }
        
        let center = surfaceObject.bbox.center
        
        placemark.position = center
        
        switch placemarkInfo.style {
        case .plane(let imageInfos):
            if let imageInfo = imageInfos[trackingObj.zoomLevel] {
                updateImage(with: imageInfo)
                placemark.opacity = 1.0
            } else {
                placemark.opacity = 0.0
            }
        case .screen(let imageInfo):
            updateImage(with: imageInfo)
        }
    }
    
    private func updateImage(with imageInfo: PlacemarkInfo.ImageInfo) {
        switch imageInfo {
        case .normal(let image):
            placemark.update(with: image.image, anchor: image.anchor, opacity: placemark.opacity)
        case .directed(let images):
            if let image = images[trackingObj.pinDirection] {
                placemark.update(with: image.image, anchor: image.anchor, opacity: placemark.opacity)
            }
        }
    }
    
    private struct Default {
        static let animation = YMLAnimation(type: .circularEaseInOut, duration: 0.2)
    }
    
}


extension StationTracker.PlacemarkInfo {

    static func makePin(image: StationTracker.PlacemarkInfo.Image) -> StationTracker.PlacemarkInfo {
        return StationTracker.PlacemarkInfo(style: .screen(image: .normal(image: image)), z: 20.0)
    }
    
    static func makePin(images: [PinDirection: StationTracker.PlacemarkInfo.Image]) -> StationTracker.PlacemarkInfo {
        return StationTracker.PlacemarkInfo(style: .screen(image: .directed(images: images)), z: 20.0)
    }

    static func makePinA(color: UIColor) -> StationTracker.PlacemarkInfo {
        return makePin(images: [
            .toTop: .init(image: PinImageResources.a(color: color, direction: .top), anchor: .topAnchor),
            .toLeft: .init(image: PinImageResources.a(color: color, direction: .left), anchor: .leftAnchor),
            .toBottom: .init(image: PinImageResources.a(color: color, direction: .bottom), anchor: .bottomAnchor),
            .toRight: .init(image: PinImageResources.a(color: color, direction: .right), anchor: .rightAnchor)
        ])
    }

    static func makePinB(color: UIColor) -> StationTracker.PlacemarkInfo {
        return makePin(images: [
            .toTop: .init(image: PinImageResources.b(color: color, direction: .top), anchor: .topAnchor),
            .toLeft: .init(image: PinImageResources.b(color: color, direction: .left), anchor: .leftAnchor),
            .toBottom: .init(image: PinImageResources.b(color: color, direction: .bottom), anchor: .bottomAnchor),
            .toRight: .init(image: PinImageResources.b(color: color, direction: .right), anchor: .rightAnchor)
        ])
    }
    
    static func makePinA(stationId: String, service: YMLService?, style: String) -> StationTracker.PlacemarkInfo {
        let color = service?.color(withStyle: style) ?? StyleKit_Metro.marker_red
        return makePinA(color: color)
    }
    
    static func makePinB(stationId: String, service: YMLService?, style: String) -> StationTracker.PlacemarkInfo {
        let color = service?.color(withStyle: style) ?? StyleKit_Metro.marker_red
        return makePinB(color: color)
    }
    
}

extension YMLSurface {
    
    func trackStation(stationId: String, serviceId: String?, placemarkInfo: StationTracker.PlacemarkInfo)
        -> StationTracker?
    {
        guard let trackingObj = trackStationImpl(withStationId: stationId, serviceId: serviceId) else { return nil }
        guard let image = getImage(from: placemarkInfo, trackingObject: trackingObj)?.image else { return nil }
        guard let anchor = getImage(from: placemarkInfo, trackingObject: trackingObj)?.anchor else { return nil }
        guard let position = trackingObj.currentSurfaceObject?.bbox.center else { return nil }
        
        let placemark: YMLPlacemarkSurfaceObject = userCollection.addPlacemark(with: image, position: position,
            anchor: anchor, opacity: 1.0, scaleMode: placemarkInfo.style.scaleMode, isInteractionEnabled: false,
            z: placemarkInfo.z)
        
        return StationTracker(stationId: stationId, trackingObj: trackingObj, placemark: placemark,
            placemarkInfo: placemarkInfo, collection: userCollection)
    }
    
    private func trackStationImpl(withStationId stationId: String, serviceId: String?) -> YMLTrackingObject? {
        if let serviceId = serviceId {
            return trackStation(withStationId: stationId, serviceId: serviceId)
        } else {
            return trackStation(withStationId: stationId)
        }
    }
    
    private func getImage(from info: StationTracker.PlacemarkInfo, trackingObject: YMLTrackingObject)
        -> StationTracker.PlacemarkInfo.Image?
    {
        switch info.style {
        case .plane(let infos):
            return (infos.first?.value).flatMap { getImage(from: $0, trackingObject: trackingObject) }
        case .screen(let info):
            return getImage(from: info, trackingObject: trackingObject)
        }
    }
    
    private func getImage(from info: StationTracker.PlacemarkInfo.ImageInfo, trackingObject: YMLTrackingObject)
        -> StationTracker.PlacemarkInfo.Image?
    {
        switch info {
        case .normal(let image):
            return image
        case .directed(let images):
            return images[trackingObject.pinDirection]
        }
    }
    
    
}

fileprivate extension YMLPoint {
    
    static var topAnchor: YMLPoint {
        return YMLPoint(x: 0.5, y: 0.0)
    }
    
    static var leftAnchor: YMLPoint {
        return YMLPoint(x: 0.0, y: 0.5)
    }
    
    static var bottomAnchor: YMLPoint {
        return YMLPoint(x: 0.5, y: 1.0)
    }
    
    static var rightAnchor: YMLPoint {
        return YMLPoint(x: 1.0, y: 0.5)
    }
    
}

fileprivate extension YMLTrackingObject {

    var pinDirection: YMLSurfaceObjectPinDirection {
        return currentSurfaceObject?.asSchemeSurfaceObject?.pinDirection ?? .toBottom
    }

}
