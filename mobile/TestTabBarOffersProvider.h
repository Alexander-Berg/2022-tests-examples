#pragma once

#include <yandex/maps/navi/resources/names/ic_tabbar_52_fuel.h>
#include <yandex/maps/navikit/resources/resource.h>
#include <yandex/maps/navikit/ui/platform_image_provider.h>
#include <yandex/maps/navi/settings/settings_manager.h>
#include <yandex/maps/navi/ui/tab_bar_model.h>
#include <yandex/maps/navi/ui/ui_controllers.h>

#include <yandex/maps/runtime/subscription/subscription.h>

namespace yandex::maps::navi::ui {

class TestTabBarOffersProvider
    : public OffersAlertProvider
    , public std::enable_shared_from_this<TestTabBarOffersProvider> {
public:
    TestTabBarOffersProvider(UiControllers* uiControllers);
    static std::shared_ptr<TestTabBarOffersProvider> create(UiControllers* uiControllers);

    virtual boost::optional<OffersAlertInfo> offersAlertInfo() const override;
    virtual OffersAlertState alertState() const override;
    virtual bool hasNewOffers() const override;

    virtual void minimizeNotification() override;
    virtual void offersTabOpened() override;
    virtual void notificationShown() override;
    virtual void notificationClicked() override;
    virtual bool shouldCallItemClickWhenNotificationClicked() override;

    virtual void addListener(const std::shared_ptr<OffersAlertProviderListener>& listener) override;
    virtual void removeListener(
        const std::shared_ptr<OffersAlertProviderListener>& listener) override;

private:
    settings::SettingsManager* const settingsManager_;
    OffersAlertState preferredAlertState_;
    runtime::subscription::Subscription<OffersAlertProviderListener> subscription_;
    UiControllers* uiControllers_;
};

}
