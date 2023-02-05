import Foundation
import YandexNaviKit
import YandexMapsMobile
import YandexMapsUtils
import YandexNaviProjectedUI

class BookmarksProviderStub: NSObject, YNKBookmarksProvider
{
    func bookmarksCollections() -> [YNKBookmarksCollection] {
        return collections[index]
    }

    static func createBookmarkInfo(title: String) -> YNKBookmarkInfo {
        return YNKBookmarkInfo(
            title: title,
            position: YMKPoint(lat: 0, lon: 0),
            address: nil,
            context: nil
        )
    }

    var index = 2;
    var collections : [[YNKBookmarksCollection]] = [
        [],
        [YNKBookmarksCollection(title: "Избранное",
                                items: [
                                    createBookmarkInfo(title: "Родители"),
                                    createBookmarkInfo(title: "Теща")
                                ], favorites: true)
        ],
        [YNKBookmarksCollection(title: "Избранное",
                                items: [
                                    createBookmarkInfo(title: "Родители"),
                                    createBookmarkInfo(title: "Теща")
                                ], favorites: false),
         YNKBookmarksCollection(title: "Турция",
                                 items: [
                                     createBookmarkInfo(title: "Отель"),
                                     createBookmarkInfo(title: "Море")
                                 ], favorites: false)
        ],
    ]
}

class PlacesProviderStub: NSObject, ObservablePlacesProvider
{
    func homeInfo() -> YNKPlaceInfo? {
        return YNKPlaceInfo(position: YMKPoint(latitude: 55.724010, longitude: 37.709386), address: "Лучший адрес на свете")
    }

    func workInfo() -> YNKPlaceInfo? {
        return YNKPlaceInfo(position: YMKPoint(latitude: 55.802882, longitude: 37.499227), address: "Лучший адрес на свете")
    }

    func addListener(listener: CarPlayChangeListener){}
    func removeListener(listener: CarPlayChangeListener){}

}

class AnnotationsSettingStub: AnnotationsSetting {
    func value() -> AnnotationsMode {
        .all
    }

    func setValue(_ value: AnnotationsMode) {

    }

    func addListener(_ listener: AnnotationsSettingListener) {

    }

    func removeListener(_ listener: AnnotationsSettingListener) {

    }
}

class BooleanSettingStub: NSObject, YNKBooleanSetting {
    private var value_: Bool = false
    private var listeners = WeakObjectCollection<YNKSettingListener>()

    func subscribe(with settingListener: YNKSettingListener) {
        listeners.insert(settingListener)
    }

    func unsubscribe(with settingListener: YNKSettingListener) {
        listeners.remove(settingListener)
    }

    func value() -> Bool {
        return value_
    }

    func setValue(_ value: Bool) {
        value_ = value
        listeners.forEach { (listener) in
            listener.onSettingChanged()
        }
    }
}

class SoundVolumeSettingStub: SoundVolumeSetting {
    var value: SoundVolume = .medium
}

class AvailableRoadEventsProviderStub : NSObject, YNKAvailableRoadEventsProvider {

    func availableRoadEvents() -> [NSNumber] {
        let events: [YMKRoadEventsEventTag] = [
            .drawbridge,
            .closed,
            .reconstruction,
            .accident,
            .trafficAlert,

            .danger,
            .school,
            .overtakingDanger,
            .pedestrianDanger,
            .crossRoadDanger,

            .police,
            .laneControl,
            .roadMarkingControl,
            .crossRoadControl,
            .noStoppingControl,
            .mobileControl,
            .speedControl
        ]
        return events.map { NSNumber(value: $0.rawValue) }
    }
}

class CameraTransformStorageStub : NSObject, YNKPlatformCameraTransformStorage {
    private var value_ : YNKCameraTransform = YNKCameraTransform(geoPosition: YMKPoint(lat: 55.735520, lon: 37.642474), zoom: 12.0)

    func cameraTransform() -> YNKCameraTransform {
        value_
    }

    func setCameraTransformWithValue(_ value: YNKCameraTransform) {
        value_ = value
    }
}

class PermissionStub : NSObject, Permission {

    let status: PermissionStatus = .granted

    func addListener(_ listener: PermissionListener) {

    }

    func removeListener(_ listener: PermissionListener) {

    }

    func request() {

    }
}
