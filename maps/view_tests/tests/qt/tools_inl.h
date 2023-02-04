#include "../tools.h"

#include <yandex/maps/mapkit/qt/mapkit.h>
#include <yandex/maps/mapkit/mapview/qt/map_view.h>
#include <yandex/maps/mapkit/map/map_window.h>

using namespace yandex::maps::mapkit::map;

namespace yandex::maps::mapkit::viewtests::tests {

namespace {
std::unique_ptr<mapview::qt::MapView> window;
} //namespace

void showMapView()
{
    if (window) {
        //already created
        return;
    }
    mapkit::qt::initMapKit();
    mapkit::qt::mapKit()->native()->initialize("0", "0");
    window = std::make_unique<mapview::qt::MapView>();
    window->show();
}

Map* map()
{
    return window->mapWindow()->map();
}

} // namespace yandex::maps::mapkit::viewtests::tests
