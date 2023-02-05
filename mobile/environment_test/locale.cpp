#include <yandex/maps/navikit/locale.h>
#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

std::string libLanguage()
{
    return getTestEnvironment()->config()->libLanguage();
}

std::string libCountry()
{
    return getTestEnvironment()->config()->libCountry();
}

std::string libLocale()
{
    return libLanguage() + "_" + libCountry();
}

boost::optional<std::string> countryByOperatorInfo()
{
    return getTestEnvironment()->config()->countryByOperatorInfo();
}

}  // namespace yandex
