#include <yandex/maps/navikit/format.h>
#include <yandex/maps/navikit/route_point.h>
#include <yandex/maps/navikit/uri.h>
#include <yandex/maps/navi/intent_parser/action_visitor.h>
#include <yandex/maps/navi/intent_parser/intent_parser.h>
#include <yandex/maps/navi/mapkit_init.h>
#include <yandex/maps/navi/mocks/mock_action_visitor.h>

#include <yandex/maps/mapkit/directions/directions_factory.h>
#include <yandex/maps/mapkit/geometry/ostream_helpers.h>
#include <yandex/maps/mapkit/geometry/point.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/base64.h>
#include <yandex/maps/runtime/network/url_helpers.h>

#include <boost/optional.hpp>
#include <boost/optional/optional_io.hpp>
#include <boost/test/unit_test.hpp>
#include <gmock/gmock.h>

#include <memory>
#include <string>

namespace navikit = yandex::maps::navikit;
namespace navi = yandex::maps::navi;
namespace runtime = yandex::maps::runtime;

using namespace yandex::maps::mapkit::directions;
using namespace yandex::maps::mapkit::geometry;
using namespace yandex::maps::navi::intent_parser;
using namespace yandex::maps::runtime::network;
using namespace ::testing;

using yandex::maps::mapkit::RequestPointType;

namespace std {

ostream& operator<<(ostream& stream, const navikit::RoutePoint& item)
{
    stream << "(" << item.location.latitude << "; " <<
            item.location.longitude << "; " <<
            item.title << "; " <<
            item.subtitle << ")";
    return stream;
}

}

namespace {

const bool isVerificationEnabled = true;

class MyActionVisitor : public MockActionVisitor {
    using super = MockActionVisitor;

public:
    virtual void onRouteAction(
        const boost::optional<navikit::RoutePoint>& to,
        const std::vector<navikit::RoutePoint>& via,
        const boost::optional<navikit::RoutePoint>& from,
        const RouteActionOptions& options) override
    {
        super::onRouteAction(to, via, from, options);

        this->action = "route";
        this->via = via;
        this->routeActionOptions = options;
    }

    virtual void onRequestRouteAction(
        const boost::optional<navikit::RoutePoint>& to,
        const std::vector<navikit::RoutePoint>& via,
        const boost::optional<navikit::RoutePoint>& from,
        const RouteActionOptions& options) override
    {
        super::onRequestRouteAction(to, via, from, options);

        this->action = "requestRoute";
        this->via = via;
        this->routeActionOptions = options;
    }

    std::string action;
    std::vector<navikit::RoutePoint> via;
    RouteActionOptions routeActionOptions;
};

void expectZeroTimesCall(const std::shared_ptr<MockActionVisitor>& visitor)
{
    EXPECT_CALL(*visitor, onMapSearchAction(_, _)).Times(0);
    EXPECT_CALL(*visitor, onRouteAction(_, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onRequestRouteAction(_, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onAddPointAction(_, _, _, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onTrafficAction(_)).Times(0);
    EXPECT_CALL(*visitor, onCarparksEnabledAction(_)).Times(0);
    EXPECT_CALL(*visitor, onCarparksRouteAction()).Times(0);
    EXPECT_CALL(*visitor, onShowPointAction(_, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onShowUIAction(_, _)).Times(0);
    EXPECT_CALL(*visitor, onShowWebViewAction(_, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onSetRouteAction(_)).Times(0);
    EXPECT_CALL(*visitor, onShowTutorialAction(_)).Times(0);
    EXPECT_CALL(*visitor, onShowLandingPageAction(_, _)).Times(0);
    EXPECT_CALL(*visitor, onSetPlaceAction(_, _)).Times(0);
    EXPECT_CALL(*visitor, onAddExperiments(_, _)).Times(0);
    EXPECT_CALL(*visitor, onResetExperiments(_, _)).Times(0);
    EXPECT_CALL(*visitor, onForceUpdate(_)).Times(0);
    EXPECT_CALL(*visitor, onExternalConfirmationAction(_)).Times(0);
    EXPECT_CALL(*visitor, onAddBookmarkAction(_, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onSetSoundSchemeAction(_, _)).Times(0);
    EXPECT_CALL(*visitor, onClearAnonymousDataAction()).Times(0);
    EXPECT_CALL(*visitor, onOpenPlatformSettingsAction()).Times(0);
    EXPECT_CALL(*visitor, onShowUserPosition()).Times(0);
    EXPECT_CALL(*visitor, onShowRouteOverview()).Times(0);
    EXPECT_CALL(*visitor, onClearRoute()).Times(0);
    EXPECT_CALL(*visitor, onSetSetting(_, _)).Times(0);
    EXPECT_CALL(*visitor, onDialPhone(_)).Times(0);
    EXPECT_CALL(*visitor, onOpenUriWithFallback(_, _, _, _)).Times(0);
    EXPECT_CALL(*visitor, onAskAlice(_)).Times(0);
    EXPECT_CALL(*visitor, onShowAuth()).Times(0);
    EXPECT_CALL(*visitor, onDownloadOfflineCache(_, _)).Times(0);
    EXPECT_CALL(*visitor, onClearOfflineCache()).Times(0);
    EXPECT_CALL(*visitor, onRunMapPerfTest(_, _, _)).Times(0);
    EXPECT_CALL(*visitor, onUpdatePassengers(_)).Times(0);
    EXPECT_CALL(*visitor, onShowPassengers(_, _)).Times(0);
}

class IntentParserTestFixture {
protected:
    IntentParserTestFixture()
    {
        runtime::async::ui()->spawn([this] { setUp(); }).wait();
    }

    virtual ~IntentParserTestFixture()
    {
        runtime::async::ui()->spawn([this] { tearDown(); }).wait();
    }

    void setUp()
    {
        navi::initMapkit(false);

        routeSerializer = getDirections()->createDrivingRouter()->routeSerializer();
        visitor = std::make_shared<MyActionVisitor>();
        intentParser = createIntentParser(routeSerializer.get());
    }

    void tearDown()
    {
        routeSerializer.reset();
        visitor.reset();
        intentParser.reset();
    }

    std::unique_ptr<driving::RouteSerializer> routeSerializer;
    std::shared_ptr<MyActionVisitor> visitor;
    std::unique_ptr<IntentParser> intentParser;
};

}

BOOST_FIXTURE_TEST_SUITE(IntentParserTests, IntentParserTestFixture)

BOOST_AUTO_TEST_CASE(MapSearchActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string TEXT = "Москва, Льва Толстого 16";
        const std::string INTENT = navikit::format(
            "map_search?text=%s", paramEscape(TEXT));

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onMapSearchAction(TEXT, _)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(RouteActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const double LAT_FROM = 37.623593;
        const double LON_FROM = 55.754945;
        const std::string TITLE_FROM = paramEscape("Title from");
        const std::string SUBTITLE_FROM = paramEscape("Subtitle from");

        const double LAT_VIA_0 = 38.623593;
        const double LON_VIA_0 = 56.754945;
        const std::string TITLE_VIA_0 = paramEscape("Title via 0");
        const std::string SUBTITLE_VIA_0 = paramEscape("Subtitle via 0");

        const double LAT_VIA_1 = 39.623593;
        const double LON_VIA_1 = 57.754945;
        const std::string TITLE_VIA_1 = paramEscape("Title via 1");
        const std::string SUBTITLE_VIA_1 = paramEscape("Subtitle via 1");

        const double LAT_TO = 40.623593;
        const double LON_TO = 58.754945;
        const std::string TITLE_TO = paramEscape("Title to");
        const std::string SUBTITLE_TO  = paramEscape("Subtitle to");

        const std::string CONTEXT_TO = paramEscape(
            "djF8MzcuNDg5NTQ0LDU1LjYwMjQ1Njsz"
            "Ny40OTIwNTUsNTUuNjA0MjF8MzcuNDg5NDY3LDU1LjYwMjQ2OSwxMDkzNDQx"
            "MDgzXzBfLDAuNjc7MzcuNDkyODUxLDU1LjYwNDkzMSwxMDkzNDQxMDgzXzEs"
            "MC4xNjszNy40OTMxMDMsNTUuNjA0NjAxLDEwOTM0NDEwODNfMiwwLjE7Mzcu"
            "NDkxOTc0LDU1LjYwNjczNCwxMDkzNDQxMDgzXzMsMC4wNg");

        const std::string INTENT = navikit::format("build_route_on_map?"
            "lat_from=%.6lf&lon_from=%.6lf&title_from=%s&subtitle_from=%s"
            "&lat_via_0=%.6lf&lon_via_0=%.6lf&title_via_0=%s&subtitle_via_0=%s"
            "&lat_via_1=%.6lf&lon_via_1=%.6lf&title_via_1=%s&subtitle_via_1=%s&type_via_1=%s"
            "&lat_to=%.6lf&lon_to=%.6lf&title_to=%s&subtitle_to=%s"
            "&context_to=%s",
            LAT_FROM,  LON_FROM,  TITLE_FROM,  SUBTITLE_FROM,
            LAT_VIA_0, LON_VIA_0, TITLE_VIA_0, SUBTITLE_VIA_0,
            LAT_VIA_1, LON_VIA_1, TITLE_VIA_1, SUBTITLE_VIA_1, "via",
            LAT_TO,    LON_TO,    TITLE_TO,    SUBTITLE_TO,
            CONTEXT_TO);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onRouteAction(_, _, _, _)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();

    BOOST_CHECK_EQUAL(visitor->action, "route");
    BOOST_CHECK_EQUAL(visitor->via.size(), 2);
    BOOST_REQUIRE(visitor->routeActionOptions.viaTypes.size() == 2);
    BOOST_CHECK(visitor->routeActionOptions.viaTypes.at(0) == RequestPointType::Waypoint);
    BOOST_CHECK(visitor->routeActionOptions.viaTypes.at(1) == RequestPointType::Viapoint);
}

BOOST_AUTO_TEST_CASE(AddPointActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const int CATEGORY = 0;
        const boost::optional<std::string> WHERE = std::string("Средний ряд");
        const boost::optional<std::string> COMMENT = std::string("Все плохо");
        const std::string INTENT =
                navikit::format("add_point?category=%d&where=%s&comment=%s",
                    CATEGORY, paramEscape(*WHERE), paramEscape(*COMMENT));

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onAddPointAction(CATEGORY, WHERE, COMMENT, _, false, /* forcePublish= */ false)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(TrafficActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const bool TRAFFIC_ON = true;
        const std::string INTENT =
                navikit::format("traffic?traffic_on=%d", TRAFFIC_ON);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onTrafficAction(TRAFFIC_ON)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(CarparksRouteActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "carparks_route";

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onCarparksRouteAction()).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ShowPointActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const double LAT = 37.623593;
        const double LON = 55.754945;
        const boost::optional<int> ZOOM = 14;
        const Point POINT{LAT, LON};
        const boost::optional<std::string> DESC = std::string("Test");
        const boost::optional<bool> NO_BALLOON = true;
        const std::string INTENT = navikit::format(
            "show_point_on_map?lat=%.6lf&lon=%.6lf&zoom=%d&desc=%s&no-balloon=%d",
            LAT, LON, *ZOOM, paramEscape(*DESC), *NO_BALLOON);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onShowPointAction(_, ZOOM, DESC, NO_BALLOON)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ShowUIActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "show_ui/menu/settings?carparks_enabled=1";

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onCarparksEnabledAction(true)).Times(1);
        EXPECT_CALL(*visitor, onShowUIAction(_, _)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ShowWebViewActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const boost::optional<std::string> TITLE = std::string("Welcome");
        const std::string INTENT =
            "show_web_view?link=https%3A%2F%2Fyandex.ru&title=" + *TITLE;
        const boost::optional<std::string> TEMPLATE = boost::none;

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onShowWebViewAction(
            "https://yandex.ru", TITLE, AuthType::NoAuth, TEMPLATE)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(SetRouteActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "set_route?route_bytes=CQAAAAMAAAAxXzC2Aw"
            "AACroB2gW2AQo5ChEJtCVUU1j0KEASBjHCoG1pbhIRCWHUzVmEXlVAEgYxwqBta"
            "W4aEQkMit2ZyT1iQBIGMTUwwqBtKkcKCjE3LjA2LjE5LTASOQoL%2F7PxhQEAAQ"
            "ABAAERAAAAAAAAAAAZAAAAAAAAAAAiCwgAEYmnyNAL8%2Bc%2FKgsIABEnKHlHD"
            "cnkPzIMEAAYACAAKAAwADgAOiIKCwgBEWHUzVmEXlVAEQAAAAAAAAAAGAEgACgA"
            "MAA4AEAAIigKEgnNt7Lhx8tCQBEAAF45ut1LQBISCdBD8VPqy0JAER34%2F%2Fj"
            "Y3UtAMvQBCqgB4gWkAQgAEjgKEQlYsfQg4EkRQBIGMcKgbWluEhEJm6W1xvgyFU"
            "ASBjHCoG1pbhoQCX%2BGafBcNEFAEgUzNMKgbRowCAISFFRpbXVyYSBGcnVuemU"
            "gU3RyZWV0GhRUaW11cmEgRnJ1bnplIFN0cmVldCIAKhIKBw0cx7FAEAIKBw1VVY"
            "VBEAEyBgoECAMYADoYChYIAhIGCAEQAxgDEgQIARAHEgQIARAHIigKEgnNt7Lhx"
            "8tCQBEAAF45ut1LQBISCQAAlHzUy0JAEeVg%2BirB3UtAKh0SGwoMCMrv7CMSBX"
            "ymAuACEgsI1KOTNRIEW9kBcTKLAgq9AeIFuQEIABI4ChEJGcGpXj%2FWHUASBjH"
            "CoG1pbhIRCfpFYanqUlNAEgYxwqBtaW4aEAkAAAC8gi9SQBIFNzLCoG0aaAgFEh"
            "JLb21zb21vbHNreSBBdmVudWUaGExlZnQsIEtvbXNvbW9sc2t5IEF2ZW51ZSIAK"
            "AEyMgow0L3QsCDQmtC%2B0LzRgdC%2B0LzQvtC70YzRgdC60LjQuSDQv9GA0L7R"
            "gdC%2F0LXQutGCKgkKBw1VVYVBEAMyBgoECAMYAiIoChIJAACUfNTLQkARAABeO"
            "brdS0ASEgkAAHjI4MtCQBEAADZ6zt1LQCofEh0KDAjM9ewjEgWkA%2B4BXBINCK"
            "ygkzUSBrIFiAOaATLHAQqAAeIFfQgAEjgKEQnEx044iUTmPxIGMMKgc2VjEhEJk"
            "YEmgEQNB0ASBjHCoG1pbhoQCbKhDP%2FDY0NAEgUzOMKgbRoSCBMaDEV4aXQgKH"
            "JpZ2h0KSIAKgkKBw1VVYVBEAEyBgoECAEYAjoYChYIABIECAEQBRIECAEQBRIGC"
            "AEQBhgGIigKEgkAAHjI4MtCQBEAADZ6zt1LQBISCf7ImJvoy0JAEZydyc7Y3UtA"
            "KhgSFgoJCLr77CMSAt4DEgkIgKqTNRIC9gQAAAAAAAAAAAAAAAAAAgAAAHJ1AA%"
            "3D%3D";

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onSetRouteAction(_)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(SetRouteActionArchiveErrorTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::vector<uint8_t> SERIALIZED_ROUTE = {'a'};
        std::string ROUTE_BASE64 = runtime::base64Encode(SERIALIZED_ROUTE); //YQ==
        const std::string INTENT =
            "set_route?route_bytes=" + paramEscape(ROUTE_BASE64);

        expectZeroTimesCall(visitor);
        BOOST_CHECK(!intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(SetRouteActionWrongBase64Test)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "set_route?route_bytes=a";

        expectZeroTimesCall(visitor);
        BOOST_CHECK(!intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ShowLandingPageActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string TITLE = "Hello";
        const std::string DESCRIPTION = "World";

        const std::string INTENT = navikit::format(
            "show_landing_page?title=%s&desc=%s", TITLE, DESCRIPTION);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onShowLandingPageAction(TITLE, DESCRIPTION)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(SetPlaceTest)
{
    runtime::async::ui()->spawn([this]
    {
        const double LAT = 55.733761;
        const double LON = 37.587757;
        const std::string TYPE = "work";
        const int CLIENT_ID = 3;

        const std::string INTENT = navikit::format(
            "set_place?type=%s&lat=%.6lf&lon=%.6lf&client=%d",
            TYPE, LAT, LON, CLIENT_ID);

        expectZeroTimesCall(visitor);
        // 0 times because this is not YaAuto
        EXPECT_CALL(*visitor, onSetPlaceAction(PlaceType::Work, _)).Times(0);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ShowTutorialTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "show_tutorial?tutorial_type=chat";

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onShowTutorialAction(TutorialGroup::Chat)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(AddExperimentsTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT =
            "add_exp/MAPS_UI?navi_stationary_following_not_allowed=enabled";
        const auto PATH = boost::make_optional<std::string>("MAPS_UI");
        const std::map<std::string, std::string> PARAMS = {
            {"navi_stationary_following_not_allowed", "enabled"}
        };

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onAddExperiments(PATH, PARAMS)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ResetExperimentsTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT =
            "reset_exp/MAPS_UI?navi_stationary_following_not_allowed";
        const auto PATH = boost::make_optional<std::string>("MAPS_UI");
        const std::vector<std::string> PARAMS = {
            "navi_stationary_following_not_allowed"
        };

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onResetExperiments(PATH, PARAMS)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ForceUpdateTest)
{
    runtime::async::ui()->spawn([this]
    {
        const int VERSION = 270;
        const std::string INTENT = navikit::format("update?from=%d", VERSION);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onForceUpdate(VERSION)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ExternalConfirmationTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "external_confirmation?confirmed=1";

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onExternalConfirmationAction(true)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(AddBookmarkTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string TITLE = "Soaring bridge";
        const boost::optional<std::string> DESCRIPTION = boost::none;
        const double LAT = 55.749376;
        const double LON = 37.629520;
        const boost::optional<std::string> URI = boost::none;
        const std::string INTENT = navikit::format(
            "add_bookmark?title=%s&lat=%.6lf&lon=%.6lf", TITLE, LAT, LON);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onAddBookmarkAction(TITLE, DESCRIPTION, _, URI)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(SetSoundSchemeTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string SCHEME = "starwars_light";
        const std::string INTENT = navikit::format(
            "set_sound_scheme?scheme=%s", SCHEME);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onSetSoundSchemeAction(SCHEME, false)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(NotRecognizedTest)
{
    runtime::async::ui()->spawn([this]
    {
        const std::string INTENT = "show_point_on_map?lat=not_a_value";

        expectZeroTimesCall(visitor);
        BOOST_CHECK(!intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(RouteRequestIsCorrectTest)
{
    runtime::async::ui()->spawn([this]
    {
        const double LAT_FROM = 37.623593;
        const double LON_FROM = 55.754945;

        const double LAT_VIA_0 = 38.623593;
        const double LON_VIA_0 = 56.754945;

        const double LAT_VIA_1 = 39.623593;
        const double LON_VIA_1 = 57.754945;

        const double LAT_TO = 40.623593;
        const double LON_TO = 58.754945;

        const std::string CONTEXT_TO = paramEscape(
            "djF8MzcuNDg5NTQ0LDU1LjYwMjQ1Njsz"
            "Ny40OTIwNTUsNTUuNjA0MjF8MzcuNDg5NDY3LDU1LjYwMjQ2OSwxMDkzNDQx"
            "MDgzXzBfLDAuNjc7MzcuNDkyODUxLDU1LjYwNDkzMSwxMDkzNDQxMDgzXzEs"
            "MC4xNjszNy40OTMxMDMsNTUuNjA0NjAxLDEwOTM0NDEwODNfMiwwLjE7Mzcu"
            "NDkxOTc0LDU1LjYwNjczNCwxMDkzNDQxMDgzXzMsMC4wNg");

        const std::string INTENT = navikit::format("request_route?"
            "lat_from=%.6lf&lon_from=%.6lf"
            "&lat_via_0=%.6lf&lon_via_0=%.6lf"
            "&lat_via_1=%.6lf&lon_via_1=%.6lf&type_via_1=%s"
            "&lat_to=%.6lf&lon_to=%.6lf"
            "&context_to=%s"
            "&avoid_tolls=1",
            LAT_FROM,  LON_FROM,
            LAT_VIA_0, LON_VIA_0,
            LAT_VIA_1, LON_VIA_1, "via",
            LAT_TO,    LON_TO,
            CONTEXT_TO);

        expectZeroTimesCall(visitor);
        EXPECT_CALL(*visitor, onRequestRouteAction(_, _, _, _)).Times(1);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();

    BOOST_CHECK_EQUAL(visitor->action, "requestRoute");
    BOOST_CHECK_EQUAL(visitor->via.size(), 2);
    BOOST_REQUIRE(visitor->routeActionOptions.viaTypes.size() == 2);
    BOOST_CHECK(visitor->routeActionOptions.viaTypes.at(0) == RequestPointType::Waypoint);
    BOOST_CHECK(visitor->routeActionOptions.viaTypes.at(1) == RequestPointType::Viapoint);
}

BOOST_AUTO_TEST_CASE(RouteRequestIsIncorrectTest)
{
    runtime::async::ui()->spawn([this]
    {
        const double LAT_TO = 40.623593;
        const double LON_TO = 58.754945;

        const std::string INTENT = navikit::format("request_route?&lat_to=%.6lf&lon_to=%.6lf",
            LAT_TO, LON_TO);

        expectZeroTimesCall(visitor);
        BOOST_CHECK(!intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_CASE(ClearAnonymousDataActionTest)
{
    runtime::async::ui()->spawn([this]
    {
        const int CLIENT_ID = 3;
        const std::string INTENT = navikit::format(
            "clear_anonymous_data?client=%d", CLIENT_ID);

        expectZeroTimesCall(visitor);
        // 0 times because this is not YaAuto
        EXPECT_CALL(*visitor, onClearAnonymousDataAction()).Times(0);
        BOOST_CHECK(intentParser->parseIntent(navikit::Uri(INTENT), visitor.get(), isVerificationEnabled));
    }).wait();
}

BOOST_AUTO_TEST_SUITE_END()
