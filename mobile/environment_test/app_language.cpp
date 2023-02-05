#include <yandex/maps/navikit/app_language.h>
#include <yandex/maps/navikit/locale.h>
#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

std::string appLanguage()
{
    return getTestEnvironment()->config()->appLanguage();
}

std::string locale()
{
    return appLanguage() + "-" + libCountry();
}

}  // namespace yandex
