#include <yandex/maps/navikit/format.h>
#include <yandex/maps/navikit/geo_object_position.h>
#include <yandex/maps/navikit/localized_string.h>
#include <yandex/maps/navi/ads/ad_item.h>
#include <yandex/maps/navi/ads/advert_utils.h>

#include <yandex/maps/mapkit/search/advertisement.h>
#include <yandex/maps/mapkit/search/billboard_object_metadata.h>
#include <yandex/maps/mapkit/search/business_object_metadata.h>
#include <yandex/maps/mapkit/search/direct_object_metadata.h>
#include <yandex/maps/mapkit/search/phone.h>

#include <yandex/maps/runtime/keyvalue.h>

#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/optional.hpp>
#include <boost/test/unit_test.hpp>

#include <vector>

namespace yandex::maps::navi::ads::tests {

struct GeoObjectToAdItemFixture {
    std::shared_ptr<mapkit::GeoObject> geoObject
        = std::make_shared<mapkit::GeoObject>(
            std::string("geoObjectNameTest"),
            std::string("geoObjectDescriptionTest"),
            runtime::bindings::Vector<mapkit::geometry::Geometry> {
                mapkit::geometry::Point(0.0, 0.0) },
            boost::none,
            runtime::bindings::StringDictionary<mapkit::Attribution>(),
            runtime::any::Collection(),
            runtime::bindings::Vector<std::string> { "aref" });
};

namespace {

std::string createTestDisclaimer(
    const std::vector<std::string>& testDisclaimers)
{
    return "CommonAdvertising. " + boost::join(testDisclaimers, " ");
}

// Constants
struct DirectObjectMetadata {
    struct ContactInfo {
        const inline static std::string companyName = "test company";
        const inline static std::string companyPhone = "test phone";
    };

    const inline static std::string title = "test direct title";
    const inline static std::string text = "test direct text";
    const inline static mapkit::search::Counter displayCounter {
        "display", "test.display.com"
    };
    const inline static mapkit::search::Counter contactCounter {
        "contact", "test.contact.com"
    };
    const inline static std::vector<std::string> disclaimers {
        "test direct disclaimer"
    };
};

struct BusinessObjectMetadata {
    struct Advertisement {
        struct TextData {
            const inline static std::string title = "test advertisement title";
            const inline static std::string text = "test advertisement text";
            const inline static std::vector<std::string> disclaimers {
                "test advertisement text data disclaimer"
            };
        };

        struct Promo {
            struct Banner {
                const inline static std::string url
                    = "test advertisement promo banner url";
            };

            const inline static std::string title
                = "test advertisement promo title";
            const inline static std::string details
                = "test advertisement promo details";
            const inline static std::vector<std::string> disclaimers {
                "test advertisement promo disclaimer"
            };
            const inline static std::string url
                = "test advertisement promo url";
        };

        const inline static std::string title = "test advertisement title";
        const inline static std::string text = "test advertisement text";
        const inline static std::string logId = "test advertisement log id";
    };

    const inline static std::vector<std::string> formattedPhones {
        "test formatted phone 1",
        "test formatted phone 2",
        "test formatted phone 3"
    };
};

struct BillboardObjectMetadata {
    struct Properties {
        const inline static std::string title = "test billboard properties title";
        const inline static std::string campaignId = "test billboard properties campaign id";
        const inline static std::vector<std::string> disclaimers {
            "test billboard disclaimer"
        };

        struct Products {
            struct Billboard {
                const inline static std::string type = "billboard";
            };

            struct PinOnRoute {
                const inline static std::string type = "pin_on_route_v2";
                const inline static std::string pinTitle = "test pin on route title";
                const inline static std::string pinSubtitle = "test pin on route subtitle";
            };

            struct RouteViaPoint {
                const inline static std::string type = "route_via_point";
                const inline static std::string styleViaPin = "test via pin style";
                const inline static std::string viaActiveTitle = "test via pin active title";
                const inline static std::string viaInactiveTitle = "test via pin inactive title";
                const inline static std::string viaDescription = "test via pin description";
            };

            struct ZeroSpeedBanner {
                const inline static std::string type = "zero_speed_banner";
            };

            struct OverviewBanner {
                const inline static std::string type = "overview_banner";
            };
        };

        struct Chance {
            const inline static std::string validValue = "69";
            const inline static std::string invalidValue = "-69";
        };

        struct Actions {
            struct Call {
                const inline static std::string type = "Call";
                const inline static std::string phone = "";
                const inline static std::string priority = "69";
            };

            struct Search {
                const inline static std::string type = "Search";
                const inline static std::string searchQuery = "test search query";
                const inline static std::string searchTitle = "test search title";
                const inline static std::string priority = "69";
            };

            struct OpenSite {
                const inline static std::string type = "OpenSite";
                const inline static std::string url = "test open site url";
                const inline static std::string title = "test open site title";
                const inline static std::string priority = "69";
            };

            struct ResolveUri {
                const inline static std::string type = "ResolveUri";
                const inline static std::string uri = "test resolve uri uri";
                const inline static std::string eventName = "test esolve uri event name";
                const inline static std::string priority = "69";

                struct Dialog {
                    const inline static std::string title = "test resolve uri dialog title";
                    const inline static std::string content = "test resolve uri dialog content";
                    const inline static std::string ok = "test resolve uri dialog ok";
                    const inline static std::string cancel = "test resolve uri dialog cancel";
                    const inline static std::string eventOk = "test resolve uri dialog event ok";
                    const inline static std::string eventCancel
                        = "test resolve uri dialog event cancel";
                };
            };
        };

        const inline static std::string limitImpressionsPerDay = "17";
        const inline static std::string limitImpressionsTotal = "17";
        const inline static std::string stylePin = "test pin style";
        const inline static std::string description = "test billboard description";
    };

    struct Creatives {
        struct LogoText {
            const inline static std::string id = "test logo+text creative id";
            const inline static std::string type = "logo+text";

            struct Properties {
                const inline static std::string styleLogo = "test logo+text creative styleLogo";
                const inline static std::string text = "test logo+text creative text";
            };
        };

        struct Banner {
            const inline static std::string id = "test banner creative id";
            const inline static std::string type = "banner";

            struct Properties {
                const inline static std::string styleBalloonBanner
                    = "test banner creative balloon style banner";
            };
        };

        struct AudioBanner {
            const inline static std::string id = "test audio banner creative id";
            const inline static std::string type = "audio_banner";

            struct Properties {
                const inline static std::string audioUrl
                    = "test audio banner creative audio url";
                const inline static std::string buttonLeftAnchor
                    = "25.0";
                const inline static std::string styleBalloonBanner
                    = "test audio banner creative balloon style banner";
            };
        };
    };

    const inline static std::string placeId = "test billboard place id";
    const inline static std::string logId = "test billboard log id";
};

struct AdItemResult {
    struct Direct {
        const inline static std::string disclaimer
            = createTestDisclaimer(DirectObjectMetadata::disclaimers);
    };

    struct Advertisement {
        const inline static std::string disclaimer
            = createTestDisclaimer(
                BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    };

    struct Promo {
        const inline static std::string disclaimer
            = createTestDisclaimer(
                BusinessObjectMetadata::Advertisement::Promo::disclaimers);
    };

    struct Billboard {
        const inline static std::string disclaimer
            = createTestDisclaimer(
                BillboardObjectMetadata::Properties::disclaimers);

        const inline static int defaultAdChance = 100;
    };
};

void addDirectMetadata(
    std::shared_ptr<mapkit::GeoObject>& geoObject,
    const boost::optional<mapkit::search::Counter>& displayCounter,
    const boost::optional<mapkit::search::Counter>& contactCounter,
    const boost::optional<std::string>& phone)
{
    const std::shared_ptr<mapkit::search::ContactInfo> contactInfo
        = std::make_shared<mapkit::search::ContactInfo>();
    contactInfo->companyName = DirectObjectMetadata::ContactInfo::companyName;

    contactInfo->phone = phone;

    runtime::bindings::SharedVector<mapkit::search::Counter> counters;
    if (displayCounter) {
        counters.push_back(*displayCounter);
    }
    if (contactCounter) {
        counters.push_back(*contactCounter);
    }

    const mapkit::search::DirectObjectMetadata directObjectMetadata(
        DirectObjectMetadata::title,
        DirectObjectMetadata::text,
        boost::none,
        DirectObjectMetadata::disclaimers,
        boost::none,
        "",
        counters,
        {},
        *contactInfo);

    geoObject->metadataContainer->set(directObjectMetadata);
}

mapkit::search::Advertisement createAdvertisement(
    const boost::optional<std::string>& optionalTitle,
    const boost::optional<std::string>& optionalText,
    const boost::optional<std::string>& logId)
{
    mapkit::search::Advertisement advertisement;

    advertisement.logId = logId;

    if (!optionalTitle && !optionalText) {
        return advertisement;
    }

    advertisement.textData =
        std::make_shared<mapkit::search::Advertisement::TextData>(
            optionalTitle,
            optionalText,
            BusinessObjectMetadata::Advertisement::TextData::disclaimers,
            boost::none);

    return advertisement;
}

void addPromoToAdvertisement(
    mapkit::search::Advertisement* advertisement,
    const boost::optional<std::string>& title,
    const boost::optional<std::string>& bannerUrl,
    const std::string& details,
    const std::vector<std::string>& disclaimers,
    const std::string& url)
{
    boost::optional<mapkit::search::AdvertImage> banner;
    if (bannerUrl) {
        banner = mapkit::search::AdvertImage(*bannerUrl, {});
    }

    advertisement->promo =
        std::make_shared<mapkit::search::Advertisement::Promo>(
            title,
            details,
            disclaimers,
            url,
            banner,
            boost::none);
}

void addBusinessMetadata(
    std::shared_ptr<mapkit::GeoObject>& geoObject,
    const boost::optional<mapkit::search::Advertisement>& advertisement,
    const std::vector<std::string>& formattedPhoneNumbers = {})
{
    mapkit::search::BusinessObjectMetadata businessObjectMetadata;

    auto phones
        = std::make_shared<runtime::bindings::Vector<mapkit::search::Phone>>();
    for (const auto& phoneNumber : formattedPhoneNumbers) {
        mapkit::search::Phone phone;
        phone.formattedNumber = phoneNumber;
        phones->push_back(phone);
    }

    businessObjectMetadata.phones = phones;

    if (advertisement) {
        businessObjectMetadata.advertisement
            = std::make_shared<mapkit::search::Advertisement>(*advertisement);
    }

    geoObject->metadataContainer->set(businessObjectMetadata);
}

std::shared_ptr<runtime::bindings::Vector<runtime::KeyValuePair>> createProductProperties(
    const boost::optional<std::string>& optionalProductType,
    const bool& shouldAddProductProperties,
    const boost::optional<bool>& productHasDiscounts,
    const bool& shouldAddProductViaDescription)
{
    auto properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();

    if (optionalProductType == boost::none) {
        return properties;
    }

    const auto productType = *optionalProductType;

    properties->push_back({"product", productType});

    if (!shouldAddProductProperties) {
        return properties;
    }

    if (productType == BillboardObjectMetadata::Properties::Products::PinOnRoute::type) {
        properties->push_back({
            "pinTitle",
            BillboardObjectMetadata::Properties::Products::PinOnRoute::pinTitle
        });
        properties->push_back({
            "pinSubtitle",
            BillboardObjectMetadata::Properties::Products::PinOnRoute::pinSubtitle
        });
        if (productHasDiscounts) {
            properties->push_back({
                "hasDiscounts",
                *productHasDiscounts ? "true" : "false"
            });
        }
    } else if (productType == BillboardObjectMetadata::Properties::Products::RouteViaPoint::type) {
        properties->push_back({
            "styleViaPin",
            BillboardObjectMetadata::Properties::Products::RouteViaPoint::styleViaPin
        });
        properties->push_back({
            "viaActiveTitle",
            BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaActiveTitle
        });
        properties->push_back({
            "viaInactiveTitle",
            BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaInactiveTitle
        });
        if (shouldAddProductViaDescription) {
            properties->push_back({
                "viaDescription",
                BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaDescription
            });
        }
    }

    return properties;
}

mapkit::search::Creative createLogoTextCreative()
{
    mapkit::search::Creative creative;
    creative.id = BillboardObjectMetadata::Creatives::LogoText::id;
    creative.type = BillboardObjectMetadata::Creatives::LogoText::type;
    creative.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();
    creative.properties->push_back({
        "styleLogo",
        BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo
    });
    creative.properties->push_back({
        "text",
        BillboardObjectMetadata::Creatives::LogoText::Properties::text
    });

    return creative;
}

mapkit::search::Creative createBannerCreative()
{
    mapkit::search::Creative creative;
    creative.id = BillboardObjectMetadata::Creatives::Banner::id;
    creative.type = BillboardObjectMetadata::Creatives::Banner::type;
    creative.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();
    creative.properties->push_back({
        "styleBalloonBanner",
        BillboardObjectMetadata::Creatives::Banner::Properties::styleBalloonBanner
    });

    return creative;
}

mapkit::search::Creative createAudioBannerCreative()
{
    mapkit::search::Creative creative;
    creative.id = BillboardObjectMetadata::Creatives::AudioBanner::id;
    creative.type = BillboardObjectMetadata::Creatives::AudioBanner::type;
    creative.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();
    creative.properties->push_back({
        "audioUrl",
        BillboardObjectMetadata::Creatives::AudioBanner::Properties::audioUrl
    });
    creative.properties->push_back({
        "buttonLeftAnchor",
        BillboardObjectMetadata::Creatives::AudioBanner::Properties::buttonLeftAnchor
    });
    creative.properties->push_back({
        "styleBalloonBanner",
        BillboardObjectMetadata::Creatives::AudioBanner::Properties::styleBalloonBanner
    });

    return creative;
}


std::shared_ptr<runtime::bindings::SharedVector<mapkit::search::Disclaimer>> createBillboardDisclaimers()
{
    auto disclaimers
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::Disclaimer>>();
    for (const auto& disclaimerText : BillboardObjectMetadata::Properties::disclaimers) {
        const mapkit::search::Disclaimer disclaimer(disclaimerText);
        disclaimers->push_back(disclaimer);
    }

    return disclaimers;
}

mapkit::search::BillboardAction createCallAction(
    const bool& shouldAddPhone,
    const bool& hasCustomPriority)
{
    mapkit::search::BillboardAction callAction;
    callAction.type = BillboardObjectMetadata::Properties::Actions::Call::type;
    callAction.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();

    if (shouldAddPhone) {
        callAction.properties->push_back({
            "phone",
            BillboardObjectMetadata::Properties::Actions::Call::phone
        });
    }

    if (hasCustomPriority) {
        callAction.properties->push_back({
            "main",
            BillboardObjectMetadata::Properties::Actions::Call::priority
        });
    }

    return callAction;
}

mapkit::search::BillboardAction createSearchAction(
    const bool& shouldAddSearchQuery,
    const bool& shouldAddSearchTitle)
{
    mapkit::search::BillboardAction searchAction;
    searchAction.type = BillboardObjectMetadata::Properties::Actions::Search::type;
    searchAction.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();

    if (shouldAddSearchQuery) {
        searchAction.properties->push_back({
            "searchQuery",
            BillboardObjectMetadata::Properties::Actions::Search::searchQuery
        });
    }

    if (shouldAddSearchTitle) {
        searchAction.properties->push_back({
            "searchTitle",
            BillboardObjectMetadata::Properties::Actions::Search::searchTitle
        });
    }

    searchAction.properties->push_back({
        "main",
        BillboardObjectMetadata::Properties::Actions::Search::priority
    });

    return searchAction;
}

mapkit::search::BillboardAction createOpenSiteAction(
    const bool& shouldAddUrl,
    const bool& shouldAddTitle)
{
    mapkit::search::BillboardAction openSiteAction;
    openSiteAction.type = BillboardObjectMetadata::Properties::Actions::OpenSite::type;
    openSiteAction.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();

    if (shouldAddUrl) {
        openSiteAction.properties->push_back({
            "url",
            BillboardObjectMetadata::Properties::Actions::OpenSite::url
        });
    }

    if (shouldAddTitle) {
        openSiteAction.properties->push_back({
            "title",
            BillboardObjectMetadata::Properties::Actions::OpenSite::title
        });
    }

    openSiteAction.properties->push_back({
        "main",
        BillboardObjectMetadata::Properties::Actions::OpenSite::priority
    });

    return openSiteAction;
}

mapkit::search::BillboardAction createResolveUriAction(
    const bool& shouldAddUri,
    const bool& shouldAddEventName,
    const bool& shouldAddOptionalDialogProperties,
    const bool& shouldAddDialogTitle,
    const bool& shouldAddDialogContent,
    const bool& shouldAddDialogOk,
    const bool& shouldAddDialogCancel)
{
    mapkit::search::BillboardAction resolveUriAction;
    resolveUriAction.type = BillboardObjectMetadata::Properties::Actions::ResolveUri::type;
    resolveUriAction.properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();

    if (shouldAddUri) {
        resolveUriAction.properties->push_back({
            "uri",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::uri
        });
    }

    if (shouldAddEventName) {
        resolveUriAction.properties->push_back({
            "eventName",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName
        });
    }

    resolveUriAction.properties->push_back({
        "main",
        BillboardObjectMetadata::Properties::Actions::ResolveUri::priority
    });

    if (shouldAddDialogTitle) {
        resolveUriAction.properties->push_back({
            "dialogTitle",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::title
        });
    }

    if (shouldAddDialogContent) {
        resolveUriAction.properties->push_back({
            "dialogContent",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::content
        });
    }

    if (shouldAddDialogOk) {
        resolveUriAction.properties->push_back({
            "dialogOk",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::ok
        });
    }

    if (shouldAddDialogCancel) {
        resolveUriAction.properties->push_back({
            "dialogCancel",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::cancel
        });
    }

    if (shouldAddOptionalDialogProperties) {
        resolveUriAction.properties->push_back({
            "dialogEventOk",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::eventOk
        });

        resolveUriAction.properties->push_back({
            "dialogEventCancel",
            BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::eventCancel
        });
    }

    return resolveUriAction;
}

std::shared_ptr<runtime::bindings::SharedVector<mapkit::search::BillboardAction>> createActions()
{
    auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto callAction = createCallAction(true, true);
    actions->push_back(callAction);

    const auto searchAction = createSearchAction(true, true);
    actions->push_back(searchAction);

    const auto openSiteAction = createOpenSiteAction(true, true);
    actions->push_back(openSiteAction);

    const auto resolveUriAction = createResolveUriAction(
        true,
        true,
        true,
        true,
        true,
        true,
        true);
    actions->push_back(resolveUriAction);

    return actions;
}

void addBillboardMetadata(
    std::shared_ptr<mapkit::GeoObject>& geoObject,
    std::shared_ptr<runtime::bindings::Vector<runtime::KeyValuePair>> productProperties,
    const std::vector<mapkit::search::Creative>& creatives,
    std::shared_ptr<runtime::bindings::SharedVector<mapkit::search::Disclaimer>> disclaimers,
    std::shared_ptr<runtime::bindings::SharedVector<mapkit::search::BillboardAction>> actions,
    const bool& shouldAddIndependentOptionalProperties,
    const bool& isChanceValid,
    const bool& shouldAddAdDescription)
{
    mapkit::search::BillboardObjectMetadata billboardObjectMetadata;

    auto properties
        = std::make_shared<runtime::bindings::Vector<runtime::KeyValuePair>>();

    if (shouldAddIndependentOptionalProperties) {
        properties->push_back({"title", BillboardObjectMetadata::Properties::title});
        properties->push_back({"isAds", "true"});
        properties->push_back({
            "limitImpressionsPerDay",
            BillboardObjectMetadata::Properties::limitImpressionsPerDay
        });
        properties->push_back({
            "limitImpressionsTotal",
            BillboardObjectMetadata::Properties::limitImpressionsTotal
        });
        properties->push_back({
            "chance",
            isChanceValid
                ? BillboardObjectMetadata::Properties::Chance::validValue
                : BillboardObjectMetadata::Properties::Chance::invalidValue
        });
    }

    billboardObjectMetadata.placeId = BillboardObjectMetadata::placeId;

    properties->push_back({"campaignId", BillboardObjectMetadata::Properties::campaignId});

    properties->push_back({"stylePin", BillboardObjectMetadata::Properties::stylePin});
    billboardObjectMetadata.logId = BillboardObjectMetadata::logId;

    if (shouldAddAdDescription) {
        properties->push_back({
            "description",
            BillboardObjectMetadata::Properties::description
        });
    }

    if (productProperties) {
        for (const auto& property : *productProperties) {
            properties->push_back(property);
        }
    }

    billboardObjectMetadata.properties = properties;

    billboardObjectMetadata.creatives
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::Creative>>();

    for (const auto& creative : creatives) {
        billboardObjectMetadata.creatives->push_back(creative);
    }

    billboardObjectMetadata.disclaimers = disclaimers;
    billboardObjectMetadata.actions = actions;

    geoObject->metadataContainer->set(billboardObjectMetadata);
}

void checkEmptyDirectFields(const AdItem& adItem) {
    BOOST_CHECK(adItem.logId.empty());
    BOOST_CHECK(adItem.logInfo == boost::none);
    BOOST_CHECK(!adItem.pinsEnabled);
    BOOST_CHECK(!adItem.routePinsEnabled);
    BOOST_CHECK(adItem.promo == boost::none);
    BOOST_CHECK(adItem.billboard == boost::none);
    BOOST_CHECK(adItem.via == boost::none);
    BOOST_CHECK(adItem.audioZsb == boost::none);
    BOOST_CHECK(adItem.productType == boost::none);
    BOOST_CHECK(adItem.pin.creativeId == boost::none);
    BOOST_CHECK(adItem.pin.placeId == boost::none);
    BOOST_CHECK(adItem.pin.placeCoordinates == boost::none);
    BOOST_CHECK(adItem.pin.pinBitmapId == boost::none);
    BOOST_CHECK(adItem.pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem.pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem.pin.searchAction == boost::none);
    BOOST_CHECK(adItem.pin.openSiteAction == boost::none);
    BOOST_CHECK(adItem.pin.resolveUriAction == boost::none);
    BOOST_CHECK(!adItem.pin.isPinWithSnippet);
    BOOST_CHECK(adItem.pin.pinTitle == boost::none);
    BOOST_CHECK(adItem.pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem.pin.hasDiscounts);
    BOOST_CHECK(adItem.pin.campaignId.empty());
    BOOST_CHECK(adItem.pin.limitImpressionsPerDay == boost::none);
    BOOST_CHECK(adItem.pin.limitImpressionsTotal == boost::none);
    BOOST_CHECK(!adItem.pin.chance);
}

void checkEmptyBusinessFields(const AdItem& adItem)
{
    BOOST_CHECK(adItem.logInfo == boost::none);
    BOOST_CHECK(adItem.billboard == boost::none);
    BOOST_CHECK(adItem.via == boost::none);
    BOOST_CHECK(adItem.audioZsb == boost::none);
    BOOST_CHECK(adItem.productType == boost::none);
    BOOST_CHECK(adItem.pin.creativeId == boost::none);
    BOOST_CHECK(adItem.pin.placeId == boost::none);
    BOOST_CHECK(adItem.pin.placeCoordinates == boost::none);
    BOOST_CHECK(adItem.pin.pinBitmapId == boost::none);
    BOOST_CHECK(adItem.pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem.pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem.pin.searchAction == boost::none);
    BOOST_CHECK(adItem.pin.openSiteAction == boost::none);
    BOOST_CHECK(adItem.pin.resolveUriAction == boost::none);
    BOOST_CHECK(!adItem.pin.isPinWithSnippet);
    BOOST_CHECK(adItem.pin.pinTitle == boost::none);
    BOOST_CHECK(adItem.pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem.pin.hasDiscounts);
    BOOST_CHECK(adItem.pin.campaignId.empty());
    BOOST_CHECK(adItem.pin.limitImpressionsPerDay == boost::none);
    BOOST_CHECK(adItem.pin.limitImpressionsTotal == boost::none);
    BOOST_CHECK(!adItem.pin.chance);
}

void checkEmptyBillboardFields(const AdItem& adItem) {
    BOOST_CHECK(adItem.promo == boost::none);
    BOOST_CHECK(adItem.advertisement == boost::none);
    BOOST_CHECK(adItem.direct == boost::none);
}

void checkSuccessfulBillboardActions(const AdItem& adItem) {
    BOOST_CHECK(adItem.pin.callAction);
    BOOST_CHECK(adItem.pin.callAction->phoneNumber
        == BillboardObjectMetadata::Properties::Actions::Call::phone);
    BOOST_CHECK(adItem.pin.callAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::Call::priority));

    BOOST_CHECK(adItem.pin.searchAction);
    BOOST_CHECK(adItem.pin.searchAction->query
        == BillboardObjectMetadata::Properties::Actions::Search::searchQuery);
    BOOST_CHECK(adItem.pin.searchAction->title
        == BillboardObjectMetadata::Properties::Actions::Search::searchTitle);
    BOOST_CHECK(adItem.pin.searchAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::Search::priority));

    BOOST_CHECK(adItem.pin.openSiteAction);
    BOOST_CHECK(adItem.pin.openSiteAction->url
        == BillboardObjectMetadata::Properties::Actions::OpenSite::url);
    BOOST_CHECK(adItem.pin.openSiteAction->title
        == BillboardObjectMetadata::Properties::Actions::OpenSite::title);
    BOOST_CHECK(adItem.pin.openSiteAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::OpenSite::priority));

    BOOST_CHECK(adItem.pin.resolveUriAction);
    BOOST_CHECK(adItem.pin.resolveUriAction->uri
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::uri);
    BOOST_CHECK(adItem.pin.resolveUriAction->eventName
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName);
    BOOST_CHECK(adItem.pin.resolveUriAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::ResolveUri::priority));
    BOOST_CHECK(adItem.pin.resolveUriAction->dialog->title
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::title);
    BOOST_CHECK(adItem.pin.resolveUriAction->dialog->content
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::content);
    BOOST_CHECK(adItem.pin.resolveUriAction->dialog->ok
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::ok);
    BOOST_CHECK(adItem.pin.resolveUriAction->dialog->cancel
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::cancel);
    BOOST_CHECK(adItem.pin.resolveUriAction->dialog->eventOk
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::eventOk);
    BOOST_CHECK(adItem.pin.resolveUriAction->dialog->eventCancel
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::eventCancel);
}

} // namespace

BOOST_AUTO_TEST_SUITE(DirectGeoObjectToAdItem)

// Trivial test case
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItemEmpty, GeoObjectToAdItemFixture)
{
    BOOST_CHECK_EQUAL(directGeoObjectToPinAdItem(geoObject), nullptr);
}

// Full test case
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItem, GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

// Test case without business metadata
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItemWithoutBusinessMetadata, GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement == boost::none);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

// Test case without advertisement in business object metadata
BOOST_FIXTURE_TEST_CASE(
    DirectGeoObjectToAdItemWithoutAdvertisementInBusinessMetadata,
    GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement == boost::none);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

// Test case without textData in advertisement
BOOST_FIXTURE_TEST_CASE(
    DirectGeoObjectToAdItemWithoutTextDataInAdvertisement,
    GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto advertisement
        = createAdvertisement(boost::none, boost::none, boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement == boost::none);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

// Test case without 'text' in 'textData' in advertisement in business metadata
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItemWithoutTextInAdvertisement, GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        boost::none,
        boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->advertisement->text == boost::none);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

// Test case without 'title' in 'textData' in advertisement in business metadata
BOOST_FIXTURE_TEST_CASE(
    DirectGeoObjectToAdItemWithoutTitleInAdvertisement,
    GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto advertisement = createAdvertisement(
        boost::none,
        BusinessObjectMetadata::Advertisement::text,
        boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement->title == boost::none);
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

// Test case without contact counter in direct metadata => adItem->direct is empty
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItemWithoutContactCounter, GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        boost::none,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct == boost::none);

    checkEmptyDirectFields(*adItem);
}

// Test case without display counter in direct metadata => 'adItem->direct' is empty
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItemWithoutDisplayCounter, GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        boost::none,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == DirectObjectMetadata::ContactInfo::companyPhone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct == boost::none);

    checkEmptyDirectFields(*adItem);
}

// Test case without company phone number
BOOST_FIXTURE_TEST_CASE(DirectGeoObjectToAdItemWithoutCompanyPhoneNumber, GeoObjectToAdItemFixture)
{
    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        boost::none);

    const auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        boost::none);

    addBusinessMetadata(geoObject, advertisement);

    const auto adItem = directGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->title == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers == DirectObjectMetadata::disclaimers);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.message == DirectObjectMetadata::ContactInfo::companyName);
    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);

    checkEmptyDirectFields(*adItem);
}

BOOST_AUTO_TEST_SUITE_END()


BOOST_AUTO_TEST_SUITE(BusinessGeoObjectToAdItem)

// Trivial test case
BOOST_FIXTURE_TEST_CASE(BusinessGeoObjectToAdItemEmpty, GeoObjectToAdItemFixture)
{
    BOOST_CHECK(businessGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Full test case
BOOST_FIXTURE_TEST_CASE(BusinessGeoObjectToAdItem, GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without advertisement in business object metadata
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutAdvertisementInBusinessMetadata,
    GeoObjectToAdItemFixture)
{
    addBusinessMetadata(geoObject, boost::none, BusinessObjectMetadata::formattedPhones);

    BOOST_CHECK(businessGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without logId in advertisement
BOOST_FIXTURE_TEST_CASE(BusinessGeoObjectToAdItemWithoutLogIdInAdvertisement, GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        boost::none);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    BOOST_CHECK(businessGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without promo in advertisement
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutPromoInAdvertisement,
    GeoObjectToAdItemFixture)
{
    const auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo == boost::none);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without promo title in advertisement
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutPromoTitleInAdvertisement,
    GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        boost::none,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo == boost::none);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without promo banner in advertisement
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutPromoBannerInAdvertisement,
    GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        boost::none,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(adItem->promo->banner == boost::none);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without text data in advertisement
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutTextDataInAdvertisement,
    GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        boost::none,
        boost::none,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement == boost::none);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == boost::none);
    BOOST_CHECK(adItem->disclaimers == nullptr);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without 'text' in 'textData' in advertisement in business metadata
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutTextInAdvertisement,
    GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        boost::none,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->advertisement->text == boost::none);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without 'title' in 'textData' in advertisement in business metadata
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutTitleInAdvertisement,
    GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        boost::none,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title == boost::none);
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == boost::none);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without contact counter in direct metadata => adItem->direct is empty
BOOST_FIXTURE_TEST_CASE(BusinessGeoObjectToAdItemWithoutContactCounter, GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        boost::none,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct == boost::none);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without display counter in direct metadata => adItem->direct is empty
BOOST_FIXTURE_TEST_CASE(BusinessGeoObjectToAdItemWithoutDisplayCounter, GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, BusinessObjectMetadata::formattedPhones);

    addDirectMetadata(
        geoObject,
        boost::none,
        DirectObjectMetadata::contactCounter,
        DirectObjectMetadata::ContactInfo::companyPhone);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct == boost::none);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BusinessObjectMetadata::formattedPhones.at(0));
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

// Test case without phone numbers
BOOST_FIXTURE_TEST_CASE(
    BusinessGeoObjectToAdItemWithoutCompanyPhoneNumber,
    GeoObjectToAdItemFixture)
{
    auto advertisement = createAdvertisement(
        BusinessObjectMetadata::Advertisement::title,
        BusinessObjectMetadata::Advertisement::text,
        BusinessObjectMetadata::Advertisement::logId);

    addPromoToAdvertisement(
        &advertisement,
        BusinessObjectMetadata::Advertisement::Promo::title,
        BusinessObjectMetadata::Advertisement::Promo::Banner::url,
        BusinessObjectMetadata::Advertisement::Promo::details,
        BusinessObjectMetadata::Advertisement::Promo::disclaimers,
        BusinessObjectMetadata::Advertisement::Promo::url);

    addBusinessMetadata(geoObject, advertisement, {});

    addDirectMetadata(
        geoObject,
        DirectObjectMetadata::displayCounter,
        DirectObjectMetadata::contactCounter,
        boost::none);

    const auto adItem = businessGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->logId == BusinessObjectMetadata::Advertisement::logId);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->promo);
    BOOST_CHECK(adItem->promo->text == BusinessObjectMetadata::Advertisement::Promo::title);
    BOOST_CHECK(*adItem->promo->details == BusinessObjectMetadata::Advertisement::Promo::details);
    BOOST_CHECK(adItem->promo->disclaimer == AdItemResult::Promo::disclaimer);
    BOOST_CHECK(*adItem->promo->url == BusinessObjectMetadata::Advertisement::Promo::url);
    BOOST_CHECK(*adItem->promo->banner
        == BusinessObjectMetadata::Advertisement::Promo::Banner::url);
    BOOST_CHECK(adItem->advertisement);
    BOOST_CHECK(adItem->advertisement->title
        == navikit::format("%s • ", BusinessObjectMetadata::Advertisement::title));
    BOOST_CHECK(adItem->advertisement->text == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->advertisement->disclaimer == AdItemResult::Advertisement::disclaimer);
    BOOST_CHECK(adItem->direct);
    BOOST_CHECK(adItem->direct->title == DirectObjectMetadata::title);
    BOOST_CHECK(adItem->direct->text == navikit::format(" | %s", DirectObjectMetadata::text));
    BOOST_CHECK(adItem->direct->displayCounterUrl == DirectObjectMetadata::displayCounter.url);
    BOOST_CHECK(adItem->direct->contactCounterUrl == DirectObjectMetadata::contactCounter.url);
    BOOST_CHECK(adItem->direct->disclaimer == AdItemResult::Direct::disclaimer);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(!adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BusinessObjectMetadata::Advertisement::title);
    BOOST_CHECK(adItem->disclaimers != nullptr);
    BOOST_CHECK(*adItem->disclaimers
        == BusinessObjectMetadata::Advertisement::TextData::disclaimers);
    BOOST_CHECK(adItem->pin.message == BusinessObjectMetadata::Advertisement::text);
    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(!adItem->pin.billboard);

    checkEmptyBusinessFields(*adItem);
}

BOOST_AUTO_TEST_SUITE_END()


BOOST_AUTO_TEST_SUITE(BillboardGeoObjectToAdItem)

// Trivial test case
BOOST_FIXTURE_TEST_CASE(EmptyCase, GeoObjectToAdItemFixture)
{
    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Full test case (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(BillboardProductTypeAndLogoTextFirstCreative, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = Billboard, first creative type = Banner)
BOOST_FIXTURE_TEST_CASE(BillboardProductTypeAndBannerFirstCreative, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        bannerCreative,
        logoTextCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
                == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::Banner::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::Banner::id);
    BOOST_CHECK(adItem->pin.billboardBitmapId
        == BillboardObjectMetadata::Creatives::Banner::Properties::styleBalloonBanner);
    BOOST_CHECK(adItem->pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message == boost::none);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo == boost::none);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = Billboard, first creative type = AudioBanner)
BOOST_FIXTURE_TEST_CASE(BillboardProductTypeAndAudioBannerFirstCreative, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        audioBannerCreative,
        logoTextCreative,
        bannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::AudioBanner::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::AudioBanner::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb->audioUrl
        == BillboardObjectMetadata::Creatives::AudioBanner::Properties::audioUrl);
    BOOST_CHECK(adItem->audioZsb->buttonLeftAnchor == boost::lexical_cast<double>(
        BillboardObjectMetadata::Creatives::AudioBanner::Properties::buttonLeftAnchor));
    BOOST_CHECK(adItem->audioZsb->billboardBitmapId
        == BillboardObjectMetadata::Creatives::AudioBanner::Properties::styleBalloonBanner);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message == boost::none);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo == boost::none);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = PinOnRoute, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(PinOnRouteProductTypeAndLogoTextFirstCreative, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::PinOnRoute::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::pinTitle);
    BOOST_CHECK(adItem->pin.pinSubtitle
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::pinSubtitle);
    BOOST_CHECK(adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = PinOnRoute (hasDiscounts == none), first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    PinOnRouteProductTypeAndLogoTextFirstCreativeWithoutHasDiscounts,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::PinOnRoute::type,
        true,
        boost::none,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::pinTitle);
    BOOST_CHECK(adItem->pin.pinSubtitle
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::pinSubtitle);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = RouteViaPoint, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(RouteViaPointProductTypeAndLogoTextFirstCreative, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::RouteViaPoint::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);

    BOOST_CHECK(adItem->via);
    BOOST_CHECK(adItem->via->bitmapId
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::styleViaPin);
    BOOST_CHECK(adItem->via->activeTitle
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaActiveTitle);
    BOOST_CHECK(adItem->via->inactiveTitle
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaInactiveTitle);
    BOOST_CHECK(adItem->via->description
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaDescription);

    BOOST_CHECK(adItem->pin.pinBitmapId == boost::none);
    BOOST_CHECK(adItem->pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(adItem->disclaimers == nullptr);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);
    BOOST_CHECK(adItem->pin.resolveUriAction == boost::none);

    BOOST_CHECK(adItem->billboard == boost::none);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = RouteViaPoint (without viaDescription), first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    RouteViaPointProductTypeAndLogoTextFirstCreativeWithoutViaDescription,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::RouteViaPoint::type,
        true,
        true,
        false);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);

    BOOST_CHECK(adItem->via);
    BOOST_CHECK(adItem->via->bitmapId
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::styleViaPin);
    BOOST_CHECK(adItem->via->activeTitle
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaActiveTitle);
    BOOST_CHECK(adItem->via->inactiveTitle
        == BillboardObjectMetadata::Properties::Products::RouteViaPoint::viaInactiveTitle);
    BOOST_CHECK(adItem->via->description == boost::none);

    BOOST_CHECK(adItem->pin.pinBitmapId == boost::none);
    BOOST_CHECK(adItem->pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(adItem->disclaimers == nullptr);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);
    BOOST_CHECK(adItem->pin.resolveUriAction == boost::none);

    BOOST_CHECK(adItem->billboard == boost::none);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = ZeroSpeedBanner, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    ZeroSpeedBannerProductTypeAndLogoTextFirstCreative,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::ZeroSpeedBanner::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::ZeroSpeedBanner::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message == BillboardObjectMetadata::Properties::description);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = ZeroSpeedBanner, first creative type = LogoText, without ad description)
BOOST_FIXTURE_TEST_CASE(
    ZeroSpeedBannerProductTypeAndLogoTextFirstCreativeWithoutAdDescription,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::ZeroSpeedBanner::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        false);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::ZeroSpeedBanner::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = OverviewBanner, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    OverviewBannerProductTypeAndLogoTextFirstCreative,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::OverviewBanner::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::OverviewBanner::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message == BillboardObjectMetadata::Properties::description);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Full test case (product = OverviewBanner, first creative type = LogoText, without ad description)
BOOST_FIXTURE_TEST_CASE(
    OverviewBannerProductTypeAndLogoTextFirstCreativeWithoutAdDescription,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::OverviewBanner::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        false);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::OverviewBanner::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without independent optional properties in billboard metadata, i.e.
// title, isAds, limitImpressionsPerDay, limitImpressionsTotal, chance
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutOptionalPropertiesInBillboardMetadata,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        false,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == boost::none);
    BOOST_CHECK(!adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay == boost::none);
    BOOST_CHECK(adItem->pin.limitImpressionsTotal == boost::none);
    BOOST_CHECK(adItem->pin.chance == AdItemResult::Billboard::defaultAdChance);
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without product (first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(NoProductTypeAndLogoTextFirstCreative, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        boost::none,
        false,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);

    BOOST_CHECK(adItem->productType == boost::none);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without product properties (product = PinOnRoute, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    PinOnRouteProductTypeAndLogoTextFirstCreativeWithoutProudctPropertiesInBillboardMetadata,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::PinOnRoute::type,
        false,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::PinOnRoute::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(!adItem->pin.billboard);
    BOOST_CHECK(adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without creatives (product = Billboard)
BOOST_FIXTURE_TEST_CASE(BillboardProductTypeWithoutCreatives, GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        {},
        disclaimers,
        actions,
        true,
        true,
        true);

    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without disclaimers (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutDisclaimers,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto actions = createActions();

    auto disclaimers
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::Disclaimer>>();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->disclaimers->empty());

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == boost::none);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without disclaimers (product = Billboard, first creative type = Banner)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndBannerFirstCreativeWithoutDisclaimers,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        bannerCreative,
        logoTextCreative,
        audioBannerCreative
    };

    const auto actions = createActions();

    auto disclaimers
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::Disclaimer>>();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::Banner::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::Banner::id);
    BOOST_CHECK(adItem->pin.billboardBitmapId
        == BillboardObjectMetadata::Creatives::Banner::Properties::styleBalloonBanner);
    BOOST_CHECK(adItem->pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(adItem->disclaimers->empty());

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard == boost::none);

    checkEmptyBillboardFields(*adItem);
}

// Test case without disclaimers (product = Billboard, first creative type = AudioBanner)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndAudioBannerFirstCreativeWithoutDisclaimers,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        audioBannerCreative,
        logoTextCreative,
        bannerCreative
    };

    const auto actions = createActions();

    auto disclaimers
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::Disclaimer>>();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::AudioBanner::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::AudioBanner::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId == boost::none);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb->audioUrl
        == BillboardObjectMetadata::Creatives::AudioBanner::Properties::audioUrl);
    BOOST_CHECK(adItem->audioZsb->buttonLeftAnchor == boost::lexical_cast<double>(
        BillboardObjectMetadata::Creatives::AudioBanner::Properties::buttonLeftAnchor));
    BOOST_CHECK(adItem->audioZsb->billboardBitmapId
        == BillboardObjectMetadata::Creatives::AudioBanner::Properties::styleBalloonBanner);
    BOOST_CHECK(adItem->pin.message == boost::none);
    BOOST_CHECK(adItem->disclaimers->empty());

    checkSuccessfulBillboardActions(*adItem);

    BOOST_CHECK(adItem->billboard == boost::none);

    checkEmptyBillboardFields(*adItem);
}

// Test case without actions (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutActions,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);
    BOOST_CHECK(adItem->pin.resolveUriAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case with default call action priority
// (product = Billboard, first creative type = LogoText)
// We test default priority only in call action, because other actions parsing scenarios
// are exactly the same
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithDefaultActionPriority,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto callAction = createCallAction(true, false);
    actions->push_back(callAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.callAction);
    BOOST_CHECK(adItem->pin.callAction->phoneNumber
        == BillboardObjectMetadata::Properties::Actions::Call::phone);
    BOOST_CHECK(adItem->pin.callAction->actionPriority == DEFAULT_ACTION_PRIORITY);

    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);
    BOOST_CHECK(adItem->pin.resolveUriAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without phone in call action (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutPhoneInCallAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto callAction = createCallAction(false, true);
    actions->push_back(callAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without searchQuery in search action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutSearchQueryInSearchAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto searchAction = createSearchAction(false, true);
    actions->push_back(searchAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without searchTitle in search action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutSearchTitleInSearchAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto searchAction = createSearchAction(true, false);
    actions->push_back(searchAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

   BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without url in open site action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutUrlInOpenSiteAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto openSiteAction = createOpenSiteAction(false, true);
    actions->push_back(openSiteAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without title in open site action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutTitleInOpenSiteAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto openSiteAction = createOpenSiteAction(true, false);
    actions->push_back(openSiteAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.openSiteAction);
    BOOST_CHECK(adItem->pin.openSiteAction->url
        == BillboardObjectMetadata::Properties::Actions::OpenSite::url);
    BOOST_CHECK(adItem->pin.openSiteAction->title
        == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::OpenSite::priority));

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.resolveUriAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without uri in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutUriInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        false,
        true,
        true,
        true,
        true,
        true,
        true);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without event name in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutEventNameInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        true,
        false,
        true,
        true,
        true,
        true,
        true);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    BOOST_CHECK(billboardGeoObjectToPinAdItem(geoObject) == nullptr);
}

// Test case without optional properties in dialog in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutOptionalPropertiesInDialogInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        true,
        true,
        false,
        true,
        true,
        true,
        true);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.resolveUriAction);
    BOOST_CHECK(adItem->pin.resolveUriAction->uri
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::uri);
    BOOST_CHECK(adItem->pin.resolveUriAction->eventName
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName);
    BOOST_CHECK(adItem->pin.resolveUriAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::ResolveUri::priority));
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog->title
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::title);
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog->content
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::content);
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog->ok
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::ok);
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog->cancel
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::Dialog::cancel);
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog->eventOk == boost::none);
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog->eventCancel == boost::none);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without dialog title in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutDialogTitleInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        true,
        true,
        true,
        false,
        true,
        true,
        true);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.resolveUriAction);
    BOOST_CHECK(adItem->pin.resolveUriAction->uri
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::uri);
    BOOST_CHECK(adItem->pin.resolveUriAction->eventName
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName);
    BOOST_CHECK(adItem->pin.resolveUriAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::ResolveUri::priority));
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog == boost::none);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without dialog content in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutDialogContentInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        true,
        true,
        true,
        true,
        false,
        true,
        true);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.resolveUriAction);
    BOOST_CHECK(adItem->pin.resolveUriAction->uri
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::uri);
    BOOST_CHECK(adItem->pin.resolveUriAction->eventName
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName);
    BOOST_CHECK(adItem->pin.resolveUriAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::ResolveUri::priority));
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog == boost::none);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without dialog ok in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutDialogOkInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        true,
        true,
        true,
        true,
        true,
        false,
        true);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.resolveUriAction);
    BOOST_CHECK(adItem->pin.resolveUriAction->uri
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::uri);
    BOOST_CHECK(adItem->pin.resolveUriAction->eventName
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName);
    BOOST_CHECK(adItem->pin.resolveUriAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::ResolveUri::priority));
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog == boost::none);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case without dialog cancel in resolve uri action
// (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithoutDialogCancelInResolveUriAction,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions
        = std::make_shared<runtime::bindings::SharedVector<mapkit::search::BillboardAction>>();

    const auto resolveUriAction = createResolveUriAction(
        true,
        true,
        true,
        true,
        true,
        true,
        false);
    actions->push_back(resolveUriAction);

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        true,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem != nullptr);
    BOOST_CHECK(adItem->productType
        == BillboardObjectMetadata::Properties::Products::Billboard::type);
    BOOST_CHECK(!adItem->pinsEnabled);
    BOOST_CHECK(adItem->routePinsEnabled);
    BOOST_CHECK(adItem->title == BillboardObjectMetadata::Properties::title);
    BOOST_CHECK(adItem->showAdLabel);
    BOOST_CHECK(adItem->pin.placeId == BillboardObjectMetadata::placeId);
    BOOST_CHECK(adItem->pin.placeCoordinates == std::string("{\"lon\": 0, \"lat\": 0}"));
    BOOST_CHECK(adItem->pin.billboard);
    BOOST_CHECK(!adItem->pin.isPinWithSnippet);
    BOOST_CHECK(adItem->pin.pinTitle == boost::none);
    BOOST_CHECK(adItem->pin.pinSubtitle == boost::none);
    BOOST_CHECK(!adItem->pin.hasDiscounts);
    BOOST_CHECK(adItem->pin.campaignId == BillboardObjectMetadata::Properties::campaignId);
    BOOST_CHECK(adItem->pin.limitImpressionsPerDay
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsPerDay));
    BOOST_CHECK(adItem->pin.limitImpressionsTotal
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::limitImpressionsTotal));
    BOOST_CHECK(adItem->pin.chance
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Chance::validValue));
    BOOST_CHECK(adItem->logId == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->logInfo == BillboardObjectMetadata::logId);
    BOOST_CHECK(adItem->via == boost::none);
    BOOST_CHECK(adItem->pin.pinBitmapId == BillboardObjectMetadata::Properties::stylePin);
    BOOST_CHECK(adItem->pin.creativeId
        == BillboardObjectMetadata::Creatives::LogoText::id);
    BOOST_CHECK(adItem->pin.balloonBitmapId
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);
    BOOST_CHECK(adItem->pin.billboardBitmapId == boost::none);
    BOOST_CHECK(adItem->audioZsb == boost::none);
    BOOST_CHECK(adItem->pin.message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(*adItem->disclaimers == BillboardObjectMetadata::Properties::disclaimers);

    BOOST_CHECK(adItem->pin.resolveUriAction);
    BOOST_CHECK(adItem->pin.resolveUriAction->uri
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::uri);
    BOOST_CHECK(adItem->pin.resolveUriAction->eventName
        == BillboardObjectMetadata::Properties::Actions::ResolveUri::eventName);
    BOOST_CHECK(adItem->pin.resolveUriAction->actionPriority
        == boost::lexical_cast<int>(BillboardObjectMetadata::Properties::Actions::ResolveUri::priority));
    BOOST_CHECK(adItem->pin.resolveUriAction->dialog == boost::none);

    BOOST_CHECK(adItem->pin.callAction == boost::none);
    BOOST_CHECK(adItem->pin.searchAction == boost::none);
    BOOST_CHECK(adItem->pin.openSiteAction == boost::none);

    BOOST_CHECK(adItem->billboard);
    BOOST_CHECK(adItem->billboard->message
        == BillboardObjectMetadata::Creatives::LogoText::Properties::text);
    BOOST_CHECK(adItem->billboard->disclaimer == AdItemResult::Billboard::disclaimer);
    BOOST_CHECK(adItem->billboard->logo
        == BillboardObjectMetadata::Creatives::LogoText::Properties::styleLogo);

    checkEmptyBillboardFields(*adItem);
}

// Test case with invalid chance value (product = Billboard, first creative type = LogoText)
BOOST_FIXTURE_TEST_CASE(
    BillboardProductTypeAndLogoTextFirstCreativeWithInvalidChanceValue,
    GeoObjectToAdItemFixture)
{
    auto productProperties = createProductProperties(
        BillboardObjectMetadata::Properties::Products::Billboard::type,
        true,
        true,
        true);

    const auto logoTextCreative = createLogoTextCreative();
    const auto bannerCreative = createBannerCreative();
    const auto audioBannerCreative = createAudioBannerCreative();

    const std::vector<mapkit::search::Creative> creatives = {
        logoTextCreative,
        bannerCreative,
        audioBannerCreative
    };

    const auto disclaimers = createBillboardDisclaimers();

    const auto actions = createActions();

    addBillboardMetadata(
        geoObject,
        productProperties,
        creatives,
        disclaimers,
        actions,
        true,
        false,
        true);

    const auto adItem = billboardGeoObjectToPinAdItem(geoObject);

    BOOST_CHECK(adItem == nullptr);
}

BOOST_AUTO_TEST_SUITE_END()

} // yandex::maps::navikit::ads::tests
