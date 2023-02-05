#include <yandex/maps/navikit/algorithm.h>
#include <yandex/maps/navikit/mocks/mock_experiments_manager.h>
#include <yandex/maps/navi/environment_config.h>
#include <yandex/maps/navi/intent_parser/intent_parser.h>
#include <yandex/maps/navi/interaction/commercial_usage_limiter.h>
#include <yandex/maps/navi/mocks/mock_action_visitor.h>
#include <yandex/maps/navi/mocks/mock_settings_manager.h>
#include <yandex/maps/navi/mocks/mock_taximeter_action_forwarder_manager.h>
#include <yandex/maps/navi/scheme_parser/scheme_parser.h>
#include <yandex/maps/navi/scheme_parser/scheme_parser_creator.h>
#include <yandex/maps/navi/test_environment.h>

#include <yandex/maps/mapkit/map_at.h>

#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/algorithm/string.hpp>
#include <boost/test/unit_test.hpp>

using namespace testing;
using namespace std::string_literals;

using StringMap = std::map<std::string, std::string>;

namespace std {

std::ostream& operator<<(std::ostream& out, const StringMap& map)
{
    out << '{';
    const char* sep = "";
    for (const auto& item : map) {
        out << sep << item.first << ':' << item.second;
        sep = ", ";
    }
    out << '}';
    return out;
}

}  // namespace std

namespace yandex::maps::navi::scheme_parser::tests {

namespace {

class MyActionVisitor;
class MyKeyReader;

// External events: https://wiki.yandex-team.ru/users/vralex/navigator-intents/
//
const std::string uriAddPoint =
    "yandexnavi://"
    "add_point?category=0&where=%D0%BB%D0%B5%D0%B2%D1%8B%D0%B9%20%D1%80%D1%8F%D0%B4&comment=bad"
    "&lat=73.0&lon=37.0";

const std::string uriBuildRoute =
    "yandexnavi://build_route_on_map?lat_to=55.758192&lon_to=37.642817";
const std::string uriBuildRouteSigned =
    "yandexnavi://"
    "build_route_on_map?lat_to=55.758192&lon_to=37.642817&client=123&signature=U2%"
    "2BhSG1YVbabbEwQu0lxuV%2FJjexQH8TDUACihGhEq1%2Bkl7DpbBbhKqnDjQl%2BwHOGZBNm5kyWmAmY07rccuLk0g%"
    "3D%3D";

const std::string uriClearAnonymousData = "yandexnavi://clear_anonymous_data";
const std::string uriClearAnonymousDataSignedByLauncher =
    "yandexnavi://"
    "clear_anonymous_data?client=3&signature=uD78gE%2F9ACnW%2Fg%"
    "2FWHWaPEUucthCz7BkngK8yMubpTk0GvyNpRdhvh5yP%2FZzwyxHi9jXPuwbKPWpCitDvrGd8yw%3D%3D";
const std::string uriClearAnonymousDataSignedByYandex =
    "yandexnavi://"
    "clear_anonymous_data?client=141&signature=e9HBPCJsW5jPkqMne3%2FwyPXGswm%"
    "2B6ejvc8gOAcIYSo5a9ZzNLcnUpqJfabet4l92PGku886WhNc7f0htYKEzoA%3D%3D";

const std::string uriSetSetting = "yandexnavi://set_setting?name=mySetting&value=myValue";
const std::string uriSetSettingSignedByTaxi =
    "yandexnavi://"
    "set_setting?name=mySetting&value=myValue&client=6&signature=N%2FhtJBnyJajihV%"
    "2FijgJp8c7oETuIWciA0gkTu%2BrnofXS%2FogZe86ni%2FjOgppBFvM5G80nPxZH%2Fv7pvMKjzxbMZQ%3D%3D";
const std::string uriSetSettingSignedByYandex =
    "yandexnavi://"
    "set_setting?name=mySetting&value=myValue&client=141&signature=wNpCsgyiPIA3yM1mDo5%"
    "2BpFlWgOzLVlqaBmDhJuMCSw%2Bv0kaYPlcqNfYUlotZ9lOkF7I%2BrmlOXa2VHOCbeI1Kbw%3D%3D";

const std::string uriShowPoint = "yandexnavi://show_point_on_map?lat=55.42&lon=37.279&no-balloon=1";

const std::string uriShowWebView =
    "yandexnavi://show_web_view?link=https%3A%2F%2Fyandex.ru&title=Welcome";
const std::string uriShowWebViewWithLink = "yandexnavi://show_web_view?title=Welcome&link=";
const std::string uriShowWebViewSignedByYandex =
    "yandexnavi://"
    "show_web_view?link=https%3A%2F%2Fyandex.ru&title=Welcome&client=141&signature=U2Kfaank3CQ%"
    "2FV3%2FMe%2F3IwPjIwoQiYvhZtX%2F3srRQfBwa1u%2BAeVPpc3y%2BS4tWB%2BGrWDKX%2F54CFONlE%2FFEzGX9rA%"
    "3D%3D";
const std::string uriShowWebViewSignedBy123 =
    "yandexnavi://"
    "show_web_view?link=https%3A%2F%2Fyandex.ru&title=Welcome&client=123&signature="
    "TgqN9DtqcOTDLoi2eKhekg0p%2Bnt25Z97uRHUSStqDS9E2Yju2LbnV9htAPi18yD%2BEY2NVfB%2F3wC3Wckmknzgng%"
    "3D%3D";

const std::string uriRemoteAuth = "yandexnavi://show_ui/menu/remote_auth?qr=123456";

const std::string uriMapSearch = "yandexnavi://map_search?text=55.758192,37.642817";

struct TestInput {
    std::string uri;
    Source source;
    boost::optional<std::string> app;

    TestInput(const char* uri) : TestInput(uri, Source::Intent) {}

    TestInput(
        const std::string& uri,
        Source source = Source::Intent,
        const boost::optional<std::string>& app = boost::none)
        : uri(uri), source(source), app(app)
    {
    }
};

struct TestResult {
    bool returnValue;
    std::string action;
    StringMap params;
    std::string screens;
    int commitedCommercialUsage;
    bool reportedLimitExceeded;
    bool voiceModeLocked;

    TestResult(bool value, const std::string& action = {}, int commitedCommercialUsage = 0)
        : TestResult(value, action, {}, commitedCommercialUsage)
    {
    }

    TestResult(
        bool value,
        const std::string& action,
        const StringMap& params,
        int commitedCommercialUsage = 0)
        : returnValue(value)
        , action(action)
        , params(params)
        , commitedCommercialUsage(commitedCommercialUsage)
        , reportedLimitExceeded(false)
        , voiceModeLocked(false)
    {
    }

    TestResult& expectReportedLimitExceeded()
    {
        reportedLimitExceeded = true;
        return *this;
    }

    TestResult& expectVoiceModeLocked()
    {
        voiceModeLocked = true;
        return *this;
    }

    TestResult& expectScreens(const std::string& value)
    {
        screens = value;
        return *this;
    }
};

const TestResult resultRemoteAuthTrusted =
    TestResult(true, "showUI", {{"qr", "123456"}, {"trusted", "true"}})
        .expectScreens("/Menu/RemoteAuth");

const TestResult resultRemoteAuthUntrusted =
    TestResult(true, "showUI", {{"qr", "123456"}}).expectScreens("/Menu/RemoteAuth");

struct ParseUriTest {
    TestInput input;
    TestResult expectedResult;
};

std::string toString(intent_parser::Screen screen)
{
    static const std::map<intent_parser::Screen, std::string> screens = {
        {intent_parser::Screen::Bookmarks, "Bookmarks"},
        {intent_parser::Screen::Cursors, "Cursors"},
        {intent_parser::Screen::DownloadMaps, "DownloadMaps"},
        {intent_parser::Screen::Fines, "Fines"},
        {intent_parser::Screen::GasStations, "GasStations"},
        {intent_parser::Screen::GasStationsManual, "GasStationsManual"},
        {intent_parser::Screen::GasStationsPayment, "GasStationsPayment"},
        {intent_parser::Screen::GasStationsSupport, "GasStationsSupport"},
        {intent_parser::Screen::Map, "Map"},
        {intent_parser::Screen::Mastercard, "Mastercard"},
        {intent_parser::Screen::Menu, "Menu"},
        {intent_parser::Screen::Parkings, "Parkings"},
        {intent_parser::Screen::RemoteAuth, "RemoteAuth"},
        {intent_parser::Screen::RoadEventsSettings, "RoadEventsSettings"},
        {intent_parser::Screen::Search, "Search"},
        {intent_parser::Screen::Settings, "Settings"},
        {intent_parser::Screen::SettingsSoundLang, "SettingsSoundLang"},
        {intent_parser::Screen::SettingsVoice, "SettingsVoice"},
        {intent_parser::Screen::Starwars, "Starwars"},
        {intent_parser::Screen::Statistics, "Statistics"},
        {intent_parser::Screen::Travel, "Travel"},
        {intent_parser::Screen::VoiceControl, "VoiceControl"},
    };

    return mapkit::optionalMapAt(screens, screen).value_or("#" + std::to_string(int(screen)));
}

void truncateParam(StringMap& params, const std::string& key) {
    const auto it = params.find(key);
    if (it != params.end()) {
        std::string& value = it->second;
        if (value.length() > 5)
            value = value.substr(0, 5) + "...";
    }
}

class SchemeParserFixture : public interaction::CommercialUsageLimiter {
public:
    void setAction(const std::string& action, const StringMap& params = {})
    {
        ASSERT(action_.empty());
        action_ = action;

        params_ = params;
        truncateParam(params_, "signature");
    }

    void setScreens(const std::vector<intent_parser::Screen>& screensToShow)
    {
        ASSERT(screens_.empty());
        screens_ = "/" + boost::join(navikit::transformIntoVector(screensToShow, toString), "/");
    }

    void lockVoiceMode() { voiceModeLocked_ = true; }

    void reportLimitExceeded() { reportedLimitExceeded_ = true; }

    // CommercialUsageLimiter
    //
    virtual void commitCommercialUsage() override { ++commitedCommercialUsage_; }

    virtual bool isLimitExceeded() const override
    {
        checkedCommercialUsage_ = true;
        return limitExceeded_;
    }

protected:
    SchemeParserFixture()
    {
        ON_CALL(experimentsManager_, experimentSnapshotValue("navi_white_list"s))
            .WillByDefault(ReturnPointee(&whiteList_));
        ON_CALL(settingsManager_, shouldIgnoreUnverifiedIntents())
            .WillByDefault(ReturnPointee(&ignoreUnverifiedIntents_));
        ON_CALL(taximeterActionForwarderManager_, forwardIntent(_))
            .WillByDefault(ReturnPointee(&isIntentForwardedSuccessfully_));

        runtime::async::ui()->spawn([&] { setUp(); }).wait();
    }

    virtual ~SchemeParserFixture()
    {
        runtime::async::ui()->spawn([&] { tearDown(); }).wait();
    }

    void setUp();
    void tearDown();

    void testParseUri(const std::vector<ParseUriTest>& tests);

    void resetState()
    {
        action_.clear();
        params_.clear();
        screens_.clear();
        reportedLimitExceeded_ = false;
        voiceModeLocked_ = false;
        checkedCommercialUsage_ = false;
        commitedCommercialUsage_ = 0;
    }

    void setLimitExceeded(bool value) { limitExceeded_ = value; }
    void setIgnoreUnverifiedIntents(bool value) { ignoreUnverifiedIntents_ = value; }
    void setIsIntentForwardedSuccessfully(bool value) { isIntentForwardedSuccessfully_ = value; }
    void setWhiteList(const std::string& value) { whiteList_ = value; }
    void verifyResult(bool returnValue, const ParseUriTest& test) const;

private:
    NiceMock<settings::MockSettingsManager> settingsManager_;
    navikit::experiments::MockExperimentsManager experimentsManager_;
    std::unique_ptr<intent_parser::IntentParser> intentParser_;
    NiceMock<taximeter_action_forwarder::MockTaximeterActionForwarderManager> taximeterActionForwarderManager_;
    std::shared_ptr<SchemeParser> schemeParser_;
    boost::optional<std::string> whiteList_;
    bool limitExceeded_ = false;
    bool ignoreUnverifiedIntents_ = false;
    bool isIntentForwardedSuccessfully_ = false;

    std::string action_;
    StringMap params_;
    std::string screens_;
    bool reportedLimitExceeded_;
    bool voiceModeLocked_;
    mutable bool checkedCommercialUsage_;
    int commitedCommercialUsage_;
};

class SchemeParserInAutoFixture : public SchemeParserFixture {
public:
    SchemeParserInAutoFixture()
    {
        runtime::async::ui()
            ->spawn([&] {
                testConfig_ = navikit::getTestEnvironment()->setConfig(
                    std::make_unique<navikit::TestEnvironmentConfig>(navikit::EnvironmentConfig::Device::CAR_NISSAN));
            })
            .wait();
    }

    ~SchemeParserInAutoFixture() override
    {
        runtime::async::ui()->spawn([&] { testConfig_.reset(); }).wait();
    }

private:
    runtime::Handle testConfig_;
};

class MyActionVisitor : public intent_parser::MockActionVisitor {
public:
    MyActionVisitor(SchemeParserFixture* fixture) : fixture_(fixture) {}

    virtual runtime::Handle lockVoiceMode() override
    {
        fixture_->lockVoiceMode();
        return runtime::Handle();
    }

    virtual void onAddPointAction(
        int /* category */,
        const boost::optional<std::string>& /* where */,
        const boost::optional<std::string>& /* comment */,
        const boost::optional<mapkit::geometry::Point>& /* position */,
        bool /* requiresVoiceConfirmation */,
        bool /* forcePublish */) override
    {
        fixture_->setAction("addPoint");
    }

    virtual void onClearAnonymousDataAction() override
    {
        fixture_->setAction("clearAnonymousData");
    }

    virtual void onRouteAction(
        const boost::optional<navikit::RoutePoint>& /* to */,
        const std::vector<navikit::RoutePoint>& /* via */,
        const boost::optional<navikit::RoutePoint>& /* from */,
        const intent_parser::RouteActionOptions& /* options */) override
    {
        fixture_->setAction("route");
    }

    virtual void onSetSetting(
        const std::string& /* name */, const std::map<std::string, std::string>& params) override
    {
        fixture_->setAction("setSetting", params);
    }

    virtual void onShowPointAction(
        const mapkit::geometry::Point& /* point */,
        const boost::optional<int>& /* zoom */,
        const boost::optional<std::string>& /* description */,
        const boost::optional<bool>& /* noBaloon */) override
    {
        fixture_->setAction("showPoint");
    }

    virtual void onShowUIAction(
        const std::vector<intent_parser::Screen>& screensToOpen,
        const std::map<std::string, std::string>& params) override
    {
        fixture_->setAction("showUI", params);
        fixture_->setScreens(screensToOpen);
    }

    virtual void onShowAuth() override { fixture_->setAction("showAuth"); }

    virtual void onShowWebViewAction(
        const std::string& url,
        const boost::optional<std::string>& /* title */,
        intent_parser::AuthType /* authType */,
        const boost::optional<std::string>& /* externTemplate */) override
    {
        if (boost::ends_with(url, "/navi-b2b") || boost::ends_with(url, "/navi/b2b"))
            fixture_->reportLimitExceeded();
        else
            fixture_->setAction("showWebView");
    }

    virtual void onIntentForwarded() override
    {
        fixture_->setAction("forwarded");
    }

    virtual void onMapSearchAction(const std::string& /*text*/,
        const boost::optional<std::string>& /*displayText*/) override
    {
        fixture_->setAction("search");
    }

private:
    SchemeParserFixture* const fixture_;
};

class MyKeyReader : public KeyReader {
public:
    virtual boost::optional<std::string> readPublicKey(int clientId) override
    {
        // from yandexnavi.core/res.xml
        //
        static const std::map<int, std::string> publicKeys{
            {3,
             "-----BEGIN PUBLIC KEY-----\n"
             "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMuZDuyLsUZNOgu+kVBFu3YfVCiPqtdd\n"
             "XIPsXf1W48D6MSNDr4mBYkxi+o9cGbS02PKo30IcZjmhJIXJJywRAAsCAwEAAQ==\n"
             "-----END PUBLIC KEY-----"},
            {6,
             "-----BEGIN PUBLIC KEY-----\n"
             "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMqSPiS43mykyjG387VUcW/iTojLV3R9\n"
             "j0J+7Z/Pcc27lbiN+XhbKx5PFSD2Zol5RWTnEC0Uue01hllzrroV3a0CAwEAAQ==\n"
             "-----END PUBLIC KEY-----"},
            {123,
             "-----BEGIN PUBLIC KEY-----\n"
             "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKhYltWyf/crowvV+dTVGPTXDu2Nzodp\n"
             "ngf/slRchEg4pRCLOq/Tie8bhmN5/wZE1IJcCEYr7+/LIDBwnIdq2g0CAwEAAQ==\n"
             "-----END PUBLIC KEY-----"},
            {141,
             "-----BEGIN PUBLIC KEY-----\n"
             "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAOf1yX/zLlr+Ez/ZVlAfkF/6uiCKUHFI\n"
             "DEQznu1yAvpG8gXMhyTNDqlTWFr3djwbZ0OJoht0bElBAUaImSjIixcCAwEAAQ==\n"
             "-----END PUBLIC KEY-----"}};
        return mapkit::optionalMapAt(publicKeys, clientId);
    }
};

void SchemeParserFixture::setUp()
{
    intentParser_ = intent_parser::createIntentParser(nullptr);
    schemeParser_ = createSchemeParser(&settingsManager_, &experimentsManager_,
            this, intentParser_.get(), &taximeterActionForwarderManager_);
    schemeParser_->setDelegate(std::make_shared<MyActionVisitor>(this));
    schemeParser_->setKeyReader(std::make_shared<MyKeyReader>());
}

void SchemeParserFixture::tearDown()
{
    schemeParser_.reset();
    intentParser_.reset();
}

void SchemeParserFixture::testParseUri(const std::vector<ParseUriTest>& tests)
{
    runtime::async::ui()
        ->spawn([&] {
            for (const auto& test : tests) {
                resetState();
                const auto& input = test.input;
                bool returnValue = schemeParser_->parseUri(input.uri, input.source, input.app);
                verifyResult(returnValue, test);
            }
        })
        .wait();
}

void SchemeParserFixture::verifyResult(bool returnValue, const ParseUriTest& test) const
{
    const auto& expectedResult = test.expectedResult;

    BOOST_CHECK_EQUAL(returnValue, expectedResult.returnValue);
    BOOST_CHECK_EQUAL(action_, expectedResult.action);
    BOOST_CHECK_EQUAL(params_, expectedResult.params);
    BOOST_CHECK_EQUAL(screens_, expectedResult.screens);
    BOOST_CHECK_EQUAL(commitedCommercialUsage_, expectedResult.commitedCommercialUsage);
    BOOST_CHECK_EQUAL(reportedLimitExceeded_, expectedResult.reportedLimitExceeded);
    BOOST_CHECK_EQUAL(voiceModeLocked_, expectedResult.voiceModeLocked);

    if (!expectedResult.returnValue) {
        ASSERT(expectedResult.action.empty());
        ASSERT(expectedResult.params.empty());
        ASSERT(expectedResult.screens.empty());
        ASSERT(!expectedResult.reportedLimitExceeded);
        ASSERT(!expectedResult.voiceModeLocked);
    }
}

}  // namespace

BOOST_FIXTURE_TEST_SUITE(scheme_parser, SchemeParserFixture)

BOOST_AUTO_TEST_CASE(ParseUri)
{
    setLimitExceeded(false);

    const std::vector<ParseUriTest> tests{
        {{uriAddPoint, Source::Intent}, {true, "addPoint"}},
        {{uriBuildRoute, Source::Intent}, {true, "route", 1}},
        {{uriBuildRoute, Source::Intent, "com.white.app"s}, {true, "route", 1}},
        {{uriBuildRouteSigned, Source::Intent}, {true, "route"}},
        {{uriShowPoint, Source::Intent}, {true, "showPoint", 1}},
        {{uriShowWebView, Source::Intent}, {true, ""}},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(ClearAnonymousData)
{
    const std::vector<ParseUriTest> tests{
        {{uriClearAnonymousData, Source::Naviprovider}, {true, "clearAnonymousData"}},
        {{uriClearAnonymousDataSignedByLauncher, Source::ProtectedBackgroundIntent}, true},
        {{uriClearAnonymousDataSignedByYandex, Source::ProtectedBackgroundIntent}, true},
        {{uriClearAnonymousData, Source::Intent}, false},
        {{uriClearAnonymousData, Source::ProtectedBackgroundIntent}, {true, "", 1}},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(LimitExceeded)
{
    setLimitExceeded(true);

    const std::vector<ParseUriTest> tests{
        {{uriAddPoint, Source::Intent}, {true, "addPoint"}},
        {{uriAddPoint, Source::BackgroundIntent}, false},

        {{uriBuildRoute, Source::Intent},
         TestResult(true, "route", 1).expectReportedLimitExceeded()},
        {{uriBuildRoute, Source::BackgroundIntent}, {true, "route", 1}},

        {{uriShowPoint, Source::Intent},
         TestResult(true, "showPoint", 1).expectReportedLimitExceeded()},
        {{uriShowPoint, Source::BackgroundIntent}, false},

        {{uriRemoteAuth, Source::Intent}, resultRemoteAuthUntrusted},
        {{uriRemoteAuth, Source::BackgroundIntent}, false},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(LimitExceededAndIgnoreUnverifiedIntents)
{
    setLimitExceeded(true);
    setIgnoreUnverifiedIntents(true);

    const std::vector<ParseUriTest> tests{
        {{uriAddPoint, Source::Intent}, {true, "addPoint"}},
        {{uriAddPoint, Source::BackgroundIntent}, false},

        {{uriBuildRoute, Source::Intent}, TestResult(true).expectReportedLimitExceeded()},
        {{uriBuildRoute, Source::BackgroundIntent}, true},

        {{uriShowPoint, Source::Intent}, TestResult(true).expectReportedLimitExceeded()},
        {{uriShowPoint, Source::BackgroundIntent}, false},

        {{uriRemoteAuth, Source::Intent}, resultRemoteAuthUntrusted},
        {{uriRemoteAuth, Source::BackgroundIntent}, false},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(ParseUriWithApp)
{
    setLimitExceeded(false);
    setWhiteList("white.app,com.white.app,ru.yandex.white.app");

    const std::vector<ParseUriTest> tests{
        {{uriAddPoint, Source::Intent, "unkwnown.app"s}, {true, "addPoint"}},
        {{uriAddPoint, Source::Intent, "white.app"s}, {true, "addPoint"}},

        {{uriBuildRoute, Source::Intent, "unkwnown.app"s}, {true, "route", 1}},
        {{uriBuildRoute, Source::Intent, "com.white.app"s}, {true, "route"}},

        {{uriBuildRouteSigned, Source::Intent, "unkwnown.app"s}, {true, "route"}},
        {{uriBuildRouteSigned, Source::Intent, "ru.yandex.white.app"s}, {true, "route"}},

        {{uriShowPoint, Source::Intent, "unkwnown.app"s}, {true, "showPoint", 1}},
        {{uriShowPoint, Source::Intent, "white.app"s}, {true, "showPoint"}},

        {{uriShowWebView, Source::Intent, "unkwnown.app"s}, {true, ""}},
        {{uriShowWebView, Source::Intent, "com.white.app"s}, {true, ""}},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(IncompatibleIntents)
{
    const std::vector<ParseUriTest> tests{
        {{uriAddPoint, Source::Naviprovider}, false},
        {{uriBuildRoute, Source::ProtectedBackgroundIntent}, false},
        {{uriShowPoint, Source::BackgroundIntent}, false},
        {{uriShowWebView, Source::SilentPush}, false},
        {{uriRemoteAuth, Source::BackgroundIntent}, false},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(RemoteAuth)
{
    setLimitExceeded(false);

    const std::vector<ParseUriTest> tests{
        {{uriRemoteAuth, Source::Intent}, resultRemoteAuthUntrusted},
        {{uriRemoteAuth + "&trusted=true", Source::Intent}, resultRemoteAuthUntrusted},

        {{uriRemoteAuth, Source::BackgroundIntent}, false},
        {{uriRemoteAuth, Source::ProtectedBackgroundIntent}, false},

        {{uriRemoteAuth, Source::Push}, resultRemoteAuthTrusted},
        {{uriRemoteAuth + "&trusted=false", Source::Push}, resultRemoteAuthTrusted},

        {{uriRemoteAuth, Source::SilentPush}, false},

        {{uriRemoteAuth, Source::Scheme}, resultRemoteAuthUntrusted},

        {{uriRemoteAuth, Source::Headunit}, resultRemoteAuthTrusted},

        {{uriRemoteAuth, Source::MetricaPush}, resultRemoteAuthUntrusted},

        {{uriRemoteAuth, Source::Vins},
         TestResult(resultRemoteAuthTrusted).expectVoiceModeLocked()},

        {{uriRemoteAuth, Source::Naviprovider}, {false}},

        {{uriRemoteAuth, Source::Internal}, resultRemoteAuthTrusted},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(SetSetting)
{
    const std::vector<ParseUriTest> tests{
        {{uriSetSetting, Source::Intent},
         {true, "setSetting", {{"name", "mySetting"}, {"value", "myValue"}}, 1}},
        {{"yandexnavi://set_setting?value=MissedName", Source::Intent}, {false, "", 1}},
        {{uriSetSetting, Source::BackgroundIntent}, false},
        {{uriSetSetting + "&client=6", Source::BackgroundIntent}, true},
        {{uriSetSetting + "&client=7", Source::BackgroundIntent}, true},
        {{uriSetSetting + "&client=7&signature=XXX", Source::BackgroundIntent}, true},
        {{uriSetSetting + "&client=141", Source::BackgroundIntent}, false},
        {{uriSetSettingSignedByTaxi, Source::BackgroundIntent},
         {true,
          "setSetting",
          {{"name", "mySetting"},
           {"value", "myValue"},
           {"client", "6"},
           {"signature", "N/htJ..."}}}},
        {{uriSetSettingSignedByYandex, Source::BackgroundIntent}, false},
        {{uriSetSetting, Source::ProtectedBackgroundIntent}, false},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(ShowWebView)
{
    const std::vector<ParseUriTest> tests{
        {{uriShowWebView, Source::Intent}, {true, ""}},
        {{uriShowWebViewWithLink + "https%3A%2F%2Fyandex.ru/promo/", Source::Intent},
         {true, "showWebView"}},  // white-listed
        {{uriShowWebViewWithLink + "https%3A%2F%2Fyandex.ru/promo/..", Source::Intent}, {true, ""}},
        {{uriShowWebViewSignedByYandex, Source::Intent}, {true, "showWebView"}},
        {{uriShowWebViewSignedBy123, Source::Intent}, {true, ""}},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(SignatureChecks)
{
    const std::vector<ParseUriTest> tests{
        {{uriBuildRoute, Source::Intent}, {true, "route", 1}},
        {{uriBuildRouteSigned, Source::Intent}, {true, "route"}},
        {{uriBuildRoute + "&client=xxx&signature=yyy", Source::Intent},
         {true, ""}},  // invalid clientId
        {{uriBuildRoute + "&client=9999&signature=yyy", Source::Intent},
         {true, ""}},  // no public key for client id
        {{uriBuildRoute + "&client=xxx", Source::Intent}, {true, ""}},             // no signature
        {{uriBuildRoute + "&signature=yyy", Source::Intent}, {true, "route", 1}},  // no client id
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(Schemes)
{
    // https://wiki.yandex-team.ru/users/tiki/navi/schemes/

    const std::vector<ParseUriTest> tests{
        {"build_route_on_map?lat_to=55.758192&lon_to=37.642817", {true, "route", 1}},
        {"yandexnavi:build_route_on_map?lat_to=55.758192&lon_to=37.642817", {true, "route", 1}},
        {"yandexnavi://build_route_on_map?lat_to=55.758192&lon_to=37.642817", {true, "route", 1}},
        {"vins:build_route_on_map?lat_to=55.758192&lon_to=37.642817", {true, "route", 1}},
        {"geo:55,37", {true, "showPoint", 1}},
        {"http://maps.google.com/maps/?q=55,37", {true, "showPoint", 1}},
        {"https://ll=-37.42%2C-55.31416&z=10", {true, "showPoint", 1}},
        {"yandex-auth:", {true, "showAuth"}},
        {"unknown:build_route_on_map?lat_to=55.758192&lon_to=37.642817", false},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(ShowUI)
{
    const std::vector<ParseUriTest> tests{
        {"yandexnavi://show_ui", TestResult(true, "showUI").expectScreens("/")},
        {"yandexnavi://show_ui/bookmarks", TestResult(true, "showUI").expectScreens("/Bookmarks")},
        {"yandexnavi://show_ui/map", TestResult(true, "showUI").expectScreens("/Map")},
        {"yandexnavi://show_ui/map/parkings",
         TestResult(true, "showUI").expectScreens("/Map/Parkings")},
        {"yandexnavi://show_ui/menu", TestResult(true, "showUI").expectScreens("/Menu")},
        {"yandexnavi://show_ui/menu/fines",
         TestResult(true, "showUI").expectScreens("/Menu/Fines")},
        {"yandexnavi://show_ui/menu/search",
         TestResult(true, "showUI").expectScreens("/Menu/Search")},
        {"yandexnavi://show_ui/menu/settings",
         TestResult(true, "showUI").expectScreens("/Menu/Settings")},
        {"yandexnavi://show_ui/menu/statistics",
         TestResult(true, "showUI").expectScreens("/Menu/Statistics")},
        {"yandexnavi://show_ui/unknown", TestResult(true, "showUI").expectScreens("/")},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_CASE(ForwardingToTaximeter)
{
    setIsIntentForwardedSuccessfully(true);

    const std::vector<ParseUriTest> tests {
        {{uriBuildRoute, Source::Intent}, TestResult(true, "forwarded", 0)},
        {{uriMapSearch, Source::Intent}, TestResult(true, "forwarded", 0)}
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_SUITE_END()

BOOST_FIXTURE_TEST_SUITE(scheme_parser_in_auto, SchemeParserInAutoFixture)

BOOST_AUTO_TEST_CASE(ClearAnonymousData)
{
    const std::vector<ParseUriTest> tests{
        {{uriClearAnonymousData, Source::Naviprovider}, {true, "clearAnonymousData"}},
        {{uriClearAnonymousDataSignedByLauncher, Source::ProtectedBackgroundIntent},
         {true, "clearAnonymousData"}},
    };

    testParseUri(tests);
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex
