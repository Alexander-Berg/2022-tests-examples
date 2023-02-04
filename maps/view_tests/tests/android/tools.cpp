#include "../tools.h"

#include <yandex/maps/internal/android/test_window.h>
#include <yandex/maps/runtime/android/jni.h>
#include <yandex/maps/runtime/android/smart_ptr.h>
#include <yandex/maps/runtime/android/object.h>
#include <yandex/maps/mapkit/map/map_window.h>

using namespace yandex::maps::runtime::internal::android;
using namespace yandex::maps::runtime::android;
using namespace yandex::maps::mapkit::map;

namespace yandex::maps::mapkit::viewtests::tests {

namespace {
JniObject jMapView;
} //namespace

void showMapView()
{
    if (jMapView) {
        //view already added
        return;
    }

    auto cls = findClass("com/yandex/mapkit/viewtests/TestViewFactory");
    jMapView = callStaticMethod<JniObject>(cls.get(), "createMapVew",
            "(Landroid/app/Activity;)Lcom/yandex/mapkit/mapview/MapView;", testWindow);
}

Map* map()
{
    auto jMapWindow = field(jMapView.get(), "mapWindow", "com/yandex/mapkit/map/internal/MapWindowBinding");
    auto mapWindow = weakGet<MapWindow>(jMapWindow.get());
    return mapWindow->map();
}

} // namespace yandex::maps::mapkit::viewtests::tests
