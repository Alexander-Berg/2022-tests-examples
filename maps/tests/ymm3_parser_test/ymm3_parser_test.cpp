#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <library/cpp/geobase/lookup.hpp>
#include <yandex/maps/proto/datacollect/track.pb.h>

#include <parsers/ymm_3x_parser.h>
#include <config.h>

#include <sstream>

const auto GEODATA_PATH = BinaryPath("maps/analyzer/data/geobase/geodata5.bin");

namespace datacollect = yandex::maps::proto::datacollect;

namespace {

void addTrackPoint(
        datacollect::Track* track,
        uint64_t time,
        double lat,
        double lon,
        float speed,
        float heading)
{
    datacollect::TrackPoint* tp = track->add_point();
    tp->set_time(time);
    datacollect::Location* l = tp->mutable_location();
    l->set_speed(speed);
    l->set_heading(heading);
    yandex::maps::proto::common2::geometry::Point* p = l->mutable_point();
    p->set_lat(lat);
    p->set_lon(lon);
}

uint64_t now()
{
    const boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
    const static boost::posix_time::ptime referencePoint(boost::gregorian::date(1970, 1, 1));
    return (now - referencePoint).total_seconds();
}

}

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(parse_correct_request_test)
{
    Config config("common_config.xml", "config.xml");
    maps::geoinfo::GeoId geoId = getGeoIdTrf();
    maps::concurrent::AtomicSharedPtr<NGeobase::TLookup> geobase(
        std::make_shared<NGeobase::TLookup>(NGeobase::TLookup::TInitTraits().Datafile(GEODATA_PATH))
    );
    auto inspector = std::make_shared<Inspector>(&config, geoId, geobase);

    Ymm3xParser parser(inspector);

    datacollect::Track testTrack;
    const std::string APP_ID = "test.app";
    const std::string MIID = "29384562934856";
    const std::string UserAgent = APP_ID + "/2.0 datasync/3.1.5 runtime/7.1.5 android/JELLY BEAN 4.3 (samsung; Samsung Galaxy`S3; ru_RU)";
    addTrackPoint(&testTrack, now(), 55.947288, 38.27516, 60.5f, 45.5f);
    TString ts;
    REQUIRE(testTrack.SerializeToString(&ts), "can't serialize test track");
    std::string body = ts;

    Parser::Env env = {
        { "QUERY_STRING", "" },
        { "REQUEST_METHOD", "POST" },
        { "CONTENT_LENGTH", std::to_string(body.size()) },
        { "CONTENT_TYPE", "application/x-www-form-urlencoded" },
        { "HTTP_USER_AGENT", UserAgent },
        { "HTTP_X_YANDEX_JA3", *config.validFingerprints().cbegin() }
    };

    yacare::QueryParams input = {
        { "clid", {APP_ID} },
        { "uuid" , { MIID }},
        { "miid" , { MIID }},
    };

    InspectionContext context;
    context.setClientIp("127.0.0.1");

    std::vector<SignalData> signals;

    parser.parse(env, input, body, &context, &signals);


    BOOST_CHECK_EQUAL(signals.size(), 1);
    const SignalData& signal = signals[0];
    // BOOST_CHECK(signal.status().accepted());

    BOOST_CHECK_EQUAL(signal.attr<std::string>("latitude"), "55.947288");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("longitude"), "38.275160");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("avg_speed"), "60.500000");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("direction"), "45.500000");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("clid"), APP_ID);
    BOOST_CHECK_EQUAL(signal.attr<std::string>("uuid"), MIID);
    BOOST_CHECK_EQUAL(signal.attr<std::string>("miid"), MIID);

    const auto& observedRegions = config.observedRegions();
    const auto& observedRegionsStats = context.observedRegionsStats(*observedRegions);

    BOOST_CHECK_EQUAL(observedRegionsStats.size(), 1); // current region is 1, cached parents with trf end up being 1, 10000
    BOOST_CHECK_EQUAL(observedRegionsStats.at(10000).total, 1); // Earth
    BOOST_CHECK_EQUAL(observedRegionsStats.at(10000).good, 0); // signal is bad - speed is out of range
}

BOOST_AUTO_TEST_SUITE_END()
