#include "../menu/storage_data_section.h"

#include <yandex/maps/navi/test_environment.h>
#include <yandex/maps/navi/test_environment_config.h>

#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::ui::menu::tests {

namespace {

runtime::Handle setEnv(const navikit::EnvironmentConfig::Device& device)
{
    return navikit::getTestEnvironment()->setConfig(std::make_unique<navikit::TestEnvironmentConfig>(device));
}

std::vector<std::string> getItems(const std::shared_ptr<MenuSection>& menuSection)
{
    std::vector<std::string> items;
    for (auto* menuItem : menuSection->items()->asVector()) {
        if (auto* item = menuItem->asAd())
            items.push_back("Ad");
        else if (auto* item = menuItem->asSpacer())
            items.push_back("Space");
        else if (auto* item = menuItem->asSpinner())
            items.push_back("Spinner");
        else if (auto* item = menuItem->asSetting())
            items.push_back("Setting: " + item->title());
        else
            items.push_back("Unknown MenuItem");
    }
    return items;
}

}  // namespace

BOOST_AUTO_TEST_CASE(TestStorageDataSection)
{
    const std::vector<std::string> all{"Setting: menu_storage_clear_maps",
                                       "Setting: menu_storage_clear_search_history",
                                       "Setting: menu_storage_clear_route_history",
                                       "Setting: menu_storage_clear_cache"};

    const std::vector<std::string> withoutClearMaps{"Setting: menu_storage_clear_search_history",
                                                    "Setting: menu_storage_clear_route_history",
                                                    "Setting: menu_storage_clear_cache"};

    const struct {
        const navikit::EnvironmentConfig::Device& device;
        const std::vector<std::string>& expectedItems;
    } tests[] = {
        {navikit::EnvironmentConfig::Device::ANDROID, all},
        {navikit::EnvironmentConfig::Device::IPHONE, all},
        {navikit::EnvironmentConfig::Device::CAR_NISSAN, withoutClearMaps},
        {navikit::EnvironmentConfig::Device::YA_AUTO, all},
        {navikit::EnvironmentConfig::Device::YA_CARSHARING, withoutClearMaps},
    };

    for (const auto& test : tests) {
        const auto items = runtime::async::ui()
                               ->async([&test] {
                                   auto handle = setEnv(test.device);
                                   auto storageDataSection = std::make_unique<StorageDataSection>(
                                       nullptr, nullptr, nullptr, nullptr, nullptr);
                                   return getItems(storageDataSection->section());
                               })
                               .get();
        BOOST_CHECK_MESSAGE(items == test.expectedItems, "Failed test #" << (&test - tests));
    }
}

}  // namespace yandex
