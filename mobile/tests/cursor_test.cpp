#include "../menu/cursor_model_select_screen_presenter_creator.h"
#include "../menu/items/item_base.h"
#include "init_navikit.h"

#include <yandex/maps/navikit/device_info.h>
#include <yandex/maps/navi/extended_app_component.h>
#include <yandex/maps/navi/mocks/mock_menu_item.h>
#include <yandex/maps/navi/mocks/mock_menu_screen.h>
#include <yandex/maps/navi/mocks/mock_message_box_factory.h>
#include <yandex/maps/navi/test_environment.h>

#include <boost/test/unit_test.hpp>
#include <gmock/gmock.h>

#include <vector>

namespace yandex::maps::navi::ui::menu {

using namespace testing;
using Device = navikit::EnvironmentConfig::Device;

namespace {

const std::vector<std::string> vehicleManufacturers = { "jaguar", "kia", "landrover", "porsche", "renault" };

class CursorTestFixture {
public:
    CursorTestFixture() {
        initAppComponentForTesting();
    }
};

std::vector<std::string> getMenuTitles() {
    std::vector<std::string> titles;

    auto onSetMenuItems =
        [&titles](const std::shared_ptr<runtime::bindings::Vector<MenuSection*>>& sections)
        {
            auto addTitle = [&titles](const std::string& title) {
                titles.push_back(title);
            };

            for (auto section : *sections) {
                for (MenuItem* item : section->items()->asVector()) {
                    auto cursorItem = item->asCursor();
                    auto cell = std::make_shared<NiceMock<MockMenuItemCursorCell>>();
                    cursorItem->bind(cell);
                    addTitle(cursorItem->title());
                }
            }
        };

    auto menuScreen = std::make_shared<NiceMock<menu::MockMenuScreen>>();
    EXPECT_CALL(*menuScreen, setMenuItems(_)).
        WillRepeatedly(Invoke(onSetMenuItems));

    auto presenter = createCursorModelSelectScreenPresenter(
        getAppComponent()->cursorModelManager(),
        std::make_shared<NiceMock<MockMessageBoxFactory>>(),
        getAppComponent()->backStack(),
        getAppComponent()->experimentsManager()
    );
    presenter->setView(menuScreen);
    presenter->dismiss();

    return titles;
}

} // anonymous namespace

BOOST_FIXTURE_TEST_SUITE(CursorTests, CursorTestFixture)

BOOST_AUTO_TEST_CASE(testNoOtherVehicleCursorsInVehicles)
{
    UI(
        for (const Device& vehicle : Device::VEHICLES()) {
            auto handle = navikit::getTestEnvironment()->setConfig(
                std::make_unique<navikit::TestEnvironmentConfig>(vehicle));

            const auto titles = getMenuTitles();

            for (const std::string& vehicleManufacturer : vehicleManufacturers) {
                if (navikit::deviceManufacturer() != vehicleManufacturer) {
                    for (const std::string& title : titles) {
                        BOOST_CHECK_MESSAGE(title.find(vehicleManufacturer) == std::string::npos,
                            "deviceManufacturer() is " << navikit::deviceManufacturer() << " " <<
                            "but MenuItem with title '" << title << "' was found");
                    }
                }
            }
        }
    );
}

BOOST_AUTO_TEST_CASE(testNoVehicleCursorsInYaAuto)
{
    UI(
        auto handle = navikit::getTestEnvironment()->setConfig(
            std::make_unique<navikit::TestEnvironmentConfig>(Device::YA_AUTO));

        const auto titles = getMenuTitles();

        for (const std::string& vehicleManufacturer : vehicleManufacturers) {
            for (const std::string& title : titles) {
                BOOST_CHECK_MESSAGE(title.find(vehicleManufacturer) == std::string::npos,
                    "Unexpected '" << title << "' cursor in YaAuto");
            }
        }
    );
}

BOOST_AUTO_TEST_SUITE_END()

}
