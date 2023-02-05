#include "../internal/config_parser.h"

#include <yandex/maps/navikit/mocks/mock_config_manager.h>
#include <yandex/maps/navi/mocks/mock_settings_manager.h>
#include <yandex/maps/navi/special_projects/special_project_data_parser.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/logging/logging.h>

#include <boost/test/unit_test.hpp>

#include <algorithm>

namespace yandex::maps::navi::special_projects {

bool operator==(const ActionButtonData& lhs, const ActionButtonData& rhs)
{
    return lhs.text == rhs.text
        && lhs.actions == rhs.actions;
}

bool operator==(const IntentData& lhs, const IntentData& rhs)
{
    return lhs.intent == rhs.intent
        && lhs.onCompletedMessageText == rhs.onCompletedMessageText;
}


bool operator==(const CursorSelectionData& lhs, const CursorSelectionData& rhs)
{
    return lhs.cursors == rhs.cursors
        && lhs.selectionDialogTitleText == rhs.selectionDialogTitleText
        && lhs.selectionDialogDismissText == rhs.selectionDialogDismissText;
}

bool operator==(const IntroData& lhs, const IntroData& rhs)
{
    return std::tie(
            lhs.reportingId,
            lhs.title,
            lhs.description,
            lhs.imagePath,
            lhs.titleColor,
            lhs.descriptionColor,
            lhs.backgroundColor,
            lhs.primaryButton,
            lhs.secondaryButton,
            lhs.closeButtonText) ==
        std::tie(
            rhs.reportingId,
            rhs.title,
            rhs.description,
            rhs.imagePath,
            rhs.titleColor,
            rhs.descriptionColor,
            rhs.backgroundColor,
            rhs.primaryButton,
            rhs.secondaryButton,
            rhs.closeButtonText);
}

bool operator==(const SoundData& lhs, const SoundData& rhs)
{
    return lhs.soundUrls == rhs.soundUrls;
}

bool operator==(const ExternalUriData& lhs, const ExternalUriData& rhs)
{
    return lhs.externalUri == rhs.externalUri;
}

bool operator==(const WebViewData& lhs, const WebViewData& rhs)
{
    return std::tie(lhs.url, lhs.titleText, lhs.externUrlPattern, lhs.authType, lhs.module) ==
        std::tie(rhs.url, rhs.titleText, rhs.externUrlPattern, rhs.authType, rhs.module);
}

bool operator==(const AliceConfig& lhs, const AliceConfig& rhs)
{
    return std::tie(
            lhs.fullDisplayText,
            lhs.shortDisplayText,
            lhs.suggestDisplayTypeText,
            lhs.imagePath) ==
        std::tie(
            rhs.fullDisplayText,
            rhs.shortDisplayText,
            rhs.suggestDisplayTypeText,
            rhs.imagePath);
}

bool operator==(const BannerData::BackgroundStyle& lhs, const BannerData::BackgroundStyle& rhs)
{
    return lhs.normalColor == rhs.normalColor
        && lhs.pressedColor == rhs.pressedColor;
}

bool operator==(const BannerData::BackgroundPaths& lhs, const BannerData::BackgroundPaths& rhs)
{
    return lhs.regularBackgroundPath == rhs.regularBackgroundPath
        && lhs.tabletBackgroundPath == rhs.tabletBackgroundPath;
}

bool operator==(const BannerData& lhs, const BannerData& rhs)
{
    return std::tie(
            lhs.reportingId,
            lhs.titleText,
            lhs.subtitleText,
            lhs.titleColor,
            lhs.subtitleColor,
            lhs.backgroundStyle,
            lhs.backgroundPaths,
            lhs.mainActions,
            lhs.button) ==
        std::tie(
            rhs.reportingId,
            rhs.titleText,
            rhs.subtitleText,
            rhs.titleColor,
            rhs.subtitleColor,
            rhs.backgroundStyle,
            rhs.backgroundPaths,
            rhs.mainActions,
            rhs.button);
}

bool operator==(const BannersConfig& lhs, const BannersConfig& rhs)
{
    return lhs.defaultBanners == rhs.defaultBanners
        && lhs.weatherBanners == rhs.weatherBanners;
}

bool operator==(const BrandRouteConfig& lhs, const BrandRouteConfig& rhs)
{
    return std::tie(
            lhs.reportingId,
            lhs.backgroundColor,
            lhs.logoPath,
            lhs.message,
            lhs.textColor,
            lhs.disclaimerColor,
            lhs.actions,
            lhs.chainIds) ==
        std::tie(
            rhs.reportingId,
            rhs.backgroundColor,
            rhs.logoPath,
            rhs.message,
            rhs.textColor,
            rhs.disclaimerColor,
            rhs.actions,
            rhs.chainIds);
}

bool operator==(const BrandRoutesConfig& lhs, const BrandRoutesConfig& rhs)
{
    return lhs.cursors == rhs.cursors
        && lhs.configsWithChains == rhs.configsWithChains
        && lhs.configWithoutChains == rhs.configWithoutChains;
}

bool operator==(const MenuCellConfig& lhs, const MenuCellConfig& rhs)
{
    return std::tie(
            lhs.reportingId,
            lhs.iconPath,
            lhs.titleText,
            lhs.subTitleText,
            lhs.actions) ==
        std::tie(
            rhs.reportingId,
            rhs.iconPath,
            rhs.titleText,
            rhs.subTitleText,
            rhs.actions);
}

bool operator==(const SplashScreenConfig& lhs, const SplashScreenConfig& rhs)
{
    return lhs.splashPortraitPath == rhs.splashPortraitPath
        && lhs.splashLandscapePath == rhs.splashLandscapePath
        && lhs.cursors == rhs.cursors;
}

bool operator==(const CursorAnimationConfig& lhs, const CursorAnimationConfig& rhs)
{
    return lhs.routeStartSoundEnabled == rhs.routeStartSoundEnabled
        && lhs.cursorIdentifiers == rhs.cursorIdentifiers;
}

bool operator==(const StatusBrandingConfig& lhs, const StatusBrandingConfig& rhs)
{
    return std::tie(
            lhs.reportingId,
            lhs.logoPath,
            lhs.placeholderPath,
            lhs.actions,
            lhs.messages) ==
        std::tie(
            rhs.reportingId,
            rhs.logoPath,
            rhs.placeholderPath,
            rhs.actions,
            rhs.messages);
}

bool operator==(const StatusBrandingsConfig& lhs, const StatusBrandingsConfig& rhs)
{
    return lhs.defaultConfig == rhs.defaultConfig
        && lhs.weatherConfigs == rhs.weatherConfigs;
}

bool operator==(const BookmarkCellConfig& lhs, const BookmarkCellConfig& rhs)
{
    return std::tie(
            lhs.reportingId,
            lhs.iconPath,
            lhs.titleText,
            lhs.subTitleText,
            lhs.actions) ==
        std::tie(
            rhs.reportingId,
            rhs.iconPath,
            rhs.titleText,
            rhs.subTitleText,
            rhs.actions);
}

bool operator==(const SpecialProjectsConfig& lhs, const SpecialProjectsConfig& rhs)
{
    // Not checking field '.jsons' as it's actual json string and therefore could be morphed by anything (like transport)

    return std::tie(
            lhs.components,
            lhs.aliceConfigs,
            lhs.bannersConfigs,
            lhs.brandRoutesConfigs,
            lhs.bookmarkCellConfigs,
            lhs.cursorAnimationConfigs,
            lhs.launchIntroConfigs,
            lhs.menuCellConfigs,
            lhs.splashScreenConfigs,
            lhs.statusBrandingsConfigs) ==
        std::tie(
            rhs.components,
            rhs.aliceConfigs,
            rhs.bannersConfigs,
            rhs.brandRoutesConfigs,
            rhs.bookmarkCellConfigs,
            rhs.cursorAnimationConfigs,
            rhs.launchIntroConfigs,
            rhs.menuCellConfigs,
            rhs.splashScreenConfigs,
            rhs.statusBrandingsConfigs);
}

}

namespace yandex::maps::navi::special_projects::tests {

BOOST_AUTO_TEST_SUITE(SpecialProjectsConfigParser)

BOOST_AUTO_TEST_CASE(JustCreates)
{
    auto configParser = internal::createConfigParser(true, /* isDebugEnvironment */ false);
    auto testingConfigParser = internal::createConfigParser(false, /* isDebugEnvironment */ false);
}

BOOST_AUTO_TEST_CASE(DoNotParsingEmptyConfig)
{
    auto configParser = internal::createConfigParser(true, /* isDebugEnvironment */ false);

    special_projects::SpecialProjectsConfig config;
    ComponentDependencies dependencies;

    const bool success = configParser->appendParsedConfig("{}", &config, &dependencies, "");
    BOOST_CHECK(!success);
}

const std::string BRAND_ROUTE_SP = R"json(
{
    "project_name": "turktelecom",
    "brand_route": {
        "cursors": [],
        "configs": [
            {
                "reporting_id": "turktelecom",
                "background_color": "ff4065F5",
                "logo": "1610976387395.png",
                "message": "MilyonlarYaayda",
                "text_color": "ffffffff",
                "disclaimer_color": "ffffffff",
                "actions": [
                    {
                        "type": "intro",
                        "reporting_id": "turktelecom1",
                        "title": "#MilyonlarYaayda",
                        "description": "Sen de Yaay'la!",
                        "image": "1610975698293.png",
                        "close_button_text": "Kapat",
                        "title_color": "ffffffff",
                        "description_color": "ffffffff",
                        "background_color": "ff000055",
                        "primary_button": {
                            "style": {
                                "normal_color": "ff4065F5",
                                "pressed_color": "ff4065F5",
                                "text_color": "ffffffff"
                            },
                            "text": "Uygulamayı yükle",
                            "actions": [
                                {
                                    "type": "external_uri",
                                    "uri": "https://some_url.some.url.com"
                                }
                            ]
                        }
                    }
                ],
                "chain_ids": []
            }
        ]
    }
})json";

BOOST_AUTO_TEST_CASE(ParseBrandRoute)
{
    auto configParser = internal::createConfigParser(true, /* isDebugEnvironment */ false);

    special_projects::SpecialProjectsConfig config;
    ComponentDependencies dependencies;

    const auto projectName = "turktelecom";
    const bool success = configParser->appendParsedConfig(BRAND_ROUTE_SP, &config, &dependencies, projectName);
    BOOST_CHECK(success);
    BOOST_CHECK(config.brandRoutesConfigs.count(projectName));
    const auto brandRoutesConfig = config.brandRoutesConfigs.at(projectName);
    BOOST_CHECK(brandRoutesConfig.configWithoutChains.size() == 1);
    const auto brandRouteConfig = brandRoutesConfig.configWithoutChains[0];
    BOOST_CHECK_EQUAL(brandRouteConfig.reportingId, "turktelecom");
    BOOST_CHECK(brandRouteConfig.actions.size() == 1);
    const auto action = brandRouteConfig.actions[0];
    BOOST_CHECK(action.type() == typeid(IntroData));
    auto introData = boost::get<IntroData>(action);
    BOOST_CHECK_EQUAL(introData.reportingId, "turktelecom1");
    BOOST_CHECK(introData.closeButtonText);
    BOOST_CHECK_EQUAL(*introData.closeButtonText, "Kapat");
}

const std::string BRAND_ROUTE_SP_SHUFFLED = R"json(
{
    "project_name": "turktelecom",
    "brand_route": {
        "cursors": [],
        "configs": [
            {
                "reporting_id": "turktelecom",
                "background_color": "ff4065F5",
                "logo": "1610976387395.png",
                "message": "MilyonlarYaayda",
                "text_color": "ffffffff",
                "disclaimer_color": "ffffffff",
                "actions": [
                    {
                        "type": "intro",
                        "reporting_id": "turktelecom1",
                        "title": "#MilyonlarYaayda",
                        "description": "Sen de Yaay'la!",
                        "image": "1610975698293.png",
                        "title_color": "ffffffff",
                        "description_color": "ffffffff",
                        "background_color": "ff000055",
                        "primary_button": {
                            "style": {
                                "normal_color": "ff4065F5",
                                "pressed_color": "ff4065F5",
                                "text_color": "ffffffff"
                            },
                            "text": "Uygulamayı yükle",
                            "actions": [
                                {
                                    "type": "external_uri",
                                    "uri": "https://some_url.some.url.com"
                                }
                            ]
                        },
                        "close_button_text": "Kapat"
                    }
                ],
                "chain_ids": []
            }
        ]
    }
})json";

BOOST_AUTO_TEST_CASE(ParseBrandRoute2)
{
    auto configParser = internal::createConfigParser(true, /* isDebugEnvironment */ false);

    special_projects::SpecialProjectsConfig config;
    ComponentDependencies dependencies;

    const auto projectName = "turktelecom";
    const bool success = configParser->appendParsedConfig(BRAND_ROUTE_SP_SHUFFLED, &config, &dependencies, projectName);
    BOOST_CHECK(success);
    BOOST_CHECK(config.brandRoutesConfigs.count(projectName));
    const auto brandRoutesConfig = config.brandRoutesConfigs.at(projectName);
    BOOST_CHECK(brandRoutesConfig.configWithoutChains.size() == 1);
    const auto brandRouteConfig = brandRoutesConfig.configWithoutChains[0];
    BOOST_CHECK_EQUAL(brandRouteConfig.reportingId, "turktelecom");
    BOOST_CHECK(brandRouteConfig.actions.size() == 1);
    const auto action = brandRouteConfig.actions[0];
    BOOST_CHECK(action.type() == typeid(IntroData));
    auto introData = boost::get<IntroData>(action);
    BOOST_CHECK_EQUAL(introData.reportingId, "turktelecom1");
    BOOST_CHECK(introData.closeButtonText);
    BOOST_CHECK_EQUAL(*introData.closeButtonText, "Kapat");
}

const std::string BRAND_ROUTE_SP_1 = R"json(
{
    "project_name": "prj_name_turktelecom",
    "brand_route": {
        "cursors": [],
        "configs": [
            {
                "reporting_id": "turktelecom",
                "background_color": "ff4065F5",
                "logo": "1610976387395.png",
                "message": "MilyonlarYaayda",
                "text_color": "ffffffff",
                "disclaimer_color": "ffffffff",
                "actions": [
                    {
                        "type": "intro",
                        "reporting_id": "turktelecom1",
                        "title": "#MilyonlarYaayda",
                        "description": "Sen de Yaay'la!",
                        "image": "1610975698293.png",
                        "close_button_text": "Kapat",
                        "title_color": "ffffffff",
                        "description_color": "ffffffff",
                        "background_color": "ff000055",
                        "primary_button": {
                            "style": {
                                "normal_color": "ff4065F5",
                                "pressed_color": "ff4065F5",
                                "text_color": "ffffffff"
                            },
                            "text": "Uygulamayı yükle",
                            "actions": [
                                {
                                    "type": "external_uri",
                                    "uri": "https://some_url.some.url.com"
                                }
                            ]
                        }
                    }
                ],
                "chain_ids": []
            }
        ]
    }
})json";

const std::string BRAND_ROUTE_SP_2 = R"json(
{
    "project_name": "prj_name_turktelecom",
    "brand_route": {
        "cursors": [],
        "configs": [
            {
                "reporting_id": "turktelecom",
                "background_color": "ff4065F5",
                "logo": "1610976387395.png",
                "message": "MilyonlarYaayda",
                "text_color": "ffffffff",
                "disclaimer_color": "ffffffff",
                "actions": [
                    {
                        "type": "intro",
                        "reporting_id": "turktelecom1",
                        "title": "#MilyonlarYaayda",
                        "description": "Sen de Yaay'la!",
                        "image": "1610975698293.png",
                        "title_color": "ffffffff",
                        "description_color": "ffffffff",
                        "background_color": "ff000055",
                        "primary_button": {
                            "style": {
                                "normal_color": "ff4065F5",
                                "pressed_color": "ff4065F5",
                                "text_color": "ffffffff"
                            },
                            "text": "Uygulamayı yükle",
                            "actions": [
                                {
                                    "type": "external_uri",
                                    "uri": "https://some_url.some.url.com"
                                }
                            ]
                        },
                        "close_button_text": "Kapat"
                    }
                ],
                "chain_ids": []
            }
        ]
    }
})json";

const std::string BRAND_ROUTE_SP_3_SHUFFLED_MORE = R"json(
{
    "brand_route": {
        "configs": [
            {
                "background_color": "ff4065F5",
                "text_color": "ffffffff",
                "logo": "1610976387395.png",
                "message": "MilyonlarYaayda",
                "disclaimer_color": "ffffffff",
                "actions": [
                    {
                        "type": "intro",
                        "description": "Sen de Yaay'la!",
                        "reporting_id": "turktelecom1",
                        "title": "#MilyonlarYaayda",
                        "image": "1610975698293.png",
                        "title_color": "ffffffff",
                        "description_color": "ffffffff",
                        "background_color": "ff000055",
                        "primary_button": {
                            "style": {
                                "normal_color": "ff4065F5",
                                "pressed_color": "ff4065F5",
                                "text_color": "ffffffff"
                            },
                            "actions": [
                                {
                                    "uri": "https://some_url.some.url.com",
                                    "type": "external_uri"
                                }
                            ],
                            "text": "Uygulamayı yükle"
                        },
                        "close_button_text": "Kapat"
                    }
                ],
                "reporting_id": "turktelecom",
                "chain_ids": []
            }
        ],
        "cursors": []
    },
    "project_name": "prj_name_turktelecom"
})json";

BOOST_AUTO_TEST_CASE(ParseAndCheckAreSame)
{
    auto configParser = internal::createConfigParser(true, /* isDebugEnvironment */ false);

    special_projects::SpecialProjectsConfig config1, config2, config3;
    ComponentDependencies dependencies1, dependencies2, dependencies3;

    const auto projectName = "prj_name_turktelecom";
    const bool success1 = configParser->appendParsedConfig(BRAND_ROUTE_SP_1, &config1, &dependencies1, projectName);
    BOOST_CHECK(success1);

    const bool success2 = configParser->appendParsedConfig(BRAND_ROUTE_SP_2, &config2, &dependencies2, projectName);
    BOOST_CHECK(success2);

    const bool success3 = configParser->appendParsedConfig(BRAND_ROUTE_SP_3_SHUFFLED_MORE, &config3, &dependencies3, projectName);
    BOOST_CHECK(success3);

    // Ok both config have been parsed successfully, now we need to make sure they're equal

    BOOST_CHECK(config1 == config2);
    BOOST_CHECK(config2 == config3);
    BOOST_CHECK(config1 == config3);
}

BOOST_AUTO_TEST_SUITE_END()

}
