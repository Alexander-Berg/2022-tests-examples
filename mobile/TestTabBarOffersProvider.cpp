#include "TestTabBarOffersProvider.h"

#include <yandex/maps/navi/resources/names/menu_icon_gas_green.h>
#include <yandex/maps/navikit/resources/resource.h>
#include <yandex/maps/navi/extended_app_component.h>

#include <yandex/maps/runtime/assert.h>
#include <yandex/maps/runtime/image/platform_bitmap.h>
#include <yandex/maps/runtime/platform_bitmap.h>

namespace yandex::maps::navi::ui {

std::shared_ptr<TestTabBarOffersProvider> TestTabBarOffersProvider::create(
    UiControllers* uiControllers)
{
    return std::make_shared<TestTabBarOffersProvider>(uiControllers);
}

TestTabBarOffersProvider::TestTabBarOffersProvider(UiControllers* uiControllers)
    : settingsManager_(getAppComponent()->settingsManager())
    , preferredAlertState_(OffersAlertState::Opened)
    , uiControllers_(uiControllers)
{
}

boost::optional<OffersAlertInfo> TestTabBarOffersProvider::offersAlertInfo() const
{
    ASSERT(settingsManager_->testTabBarNotificationEnabled());
    ASSERT(!settingsManager_->testTabBarNotificationText().empty());

    boost::optional<runtime::PlatformBitmap> iconBitmap;

    const auto platformImageProvider = uiControllers_->platformImageProvider();

    if (settingsManager_->testTabBarNotificationIconEnabled()) {
        ASSERT(platformImageProvider != nullptr);
        auto iconImage = platformImageProvider->createImage(
            navikit::resources::resource<resources::names::MenuIconGasGreen>(),
            /*cacheable */ false,
            /* scale */ 1);
        ASSERT(iconImage);
        iconBitmap =
            runtime::image::platformBitmapFromImage(*iconImage->createImageProvider()->image());
    }

    OffersAlertInfo alertInfo{
        "test-offers-alert-info",
        iconBitmap,
        settingsManager_->testTabBarNotificationText(),
        Search,
        navi::gas_stations::BannerColorsScheme{
            "FFFFFFFF",
            "FF1A1A1A",
            "FF000000",
            "FFFFFFFF",
        }};

    return alertInfo;
}

OffersAlertState TestTabBarOffersProvider::alertState() const
{
    if (!settingsManager_->testTabBarNotificationEnabled()) {
        return OffersAlertState::None;
    }
    return preferredAlertState_;
}

bool TestTabBarOffersProvider::hasNewOffers() const
{
    return true;
}

void TestTabBarOffersProvider::minimizeNotification()
{
    if (alertState() == OffersAlertState::Opened) {
        preferredAlertState_ = OffersAlertState::Minimized;
        subscription_.notify(&OffersAlertProviderListener::onOffersAlertChanged);
    }
}

void TestTabBarOffersProvider::offersTabOpened() {}

void TestTabBarOffersProvider::notificationShown() {}

void TestTabBarOffersProvider::notificationClicked()
{
    minimizeNotification();
}

bool TestTabBarOffersProvider::shouldCallItemClickWhenNotificationClicked()
{
    return false;
}

void TestTabBarOffersProvider::addListener(
    const std::shared_ptr<OffersAlertProviderListener>& listener)
{
    subscription_.subscribe(listener);
}

void TestTabBarOffersProvider::removeListener(
    const std::shared_ptr<OffersAlertProviderListener>& listener)
{
    subscription_.unsubscribe(listener);
}

}
