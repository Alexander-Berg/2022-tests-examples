#include <yandex/maps/navikit/experiment.h>
#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

const mapkit::experiments::UiExperimentsManager* getUiExperimentsManager()
{
    return getTestEnvironment()->config()->uiExperimentsManager();
}

}  // namespace yandex
