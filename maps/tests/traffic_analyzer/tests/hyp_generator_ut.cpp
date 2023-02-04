#include <maps/wikimap/mapspro/tools/traffic_analyzer/lib/graph.h>
#include <maps/wikimap/mapspro/tools/traffic_analyzer/lib/hyp_generator.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace mwt = maps::wiki::traffic_analyzer;
namespace bg = boost::gregorian;

using shortIdToFunclassMap = std::unordered_map<uint32_t, uint32_t>;

const std::string ANY_VERSION = "18.01.31";

const bg::date_period ONE_DAY_PERIOD(
    bg::date(2018, bg::Jan, 30), bg::date(2018, bg::Jan, 31));


class GraphFunclassStub : public mwt::IGraph
{
public:
    GraphFunclassStub(shortIdToFunclassMap roadIdToFunclass) :
        roadIdToFunclass_(std::move(roadIdToFunclass)) {}

    const std::string& version() const override { return ANY_VERSION; }

    mwt::GraphRoadInfo roadInfo(uint32_t roadId) const override
    {
        return {roadIdToFunclass_.at(roadId), {}};
    }

    std::vector<mwt::TwoWayRoadIds>
    getTwoWayRoads(const maps::geolib3::BoundingBox&) const override
    {
        return {};
    }

private:
    shortIdToFunclassMap roadIdToFunclass_;
};


TEST(hyp_generator, single)
{
    mwt::TwoWayRoadIds road{1, 2, 1000, 2000};

    GraphFunclassStub graph(shortIdToFunclassMap{{road.fwd, 7}, {road.bkwd, 7}});

    mwt::PersIdToTracks persIdToTracks(ONE_DAY_PERIOD,
        MapPersIdToTracks{{road.fwdPers, 90}, {road.bkwdPers, 10}});

    {
        // succesful generation
        mwt::OnewayHypothesisGenerator generator(
            graph, persIdToTracks, mwt::OnewayGenParams(0.899, 7, 99));

        auto hyps = generator.generate({road});
        ASSERT_TRUE(hyps.size() == 1);

        const auto& hyp = hyps.front();
        ASSERT_EQ(hyp.roadId, road.fwd);
        ASSERT_EQ(hyp.roadTraffic.bothEdgeTracks, 100);
        ASSERT_DOUBLE_EQ(hyp.roadTraffic.matchRatio, 0.9);
        ASSERT_TRUE(hyp.roadTraffic.graphVersion == ANY_VERSION);
        ASSERT_TRUE(hyp.roadTraffic.days == ONE_DAY_PERIOD);
    }
    {
        // check alarm match ratio
        mwt::OnewayHypothesisGenerator generator(
            graph, persIdToTracks, mwt::OnewayGenParams(0.92, 7, 99));
        auto hyps = generator.generate({road});
        ASSERT_TRUE(hyps.empty());
    }
    {
        // check total tracks
        mwt::OnewayHypothesisGenerator generator(
            graph, persIdToTracks, mwt::OnewayGenParams(0.899, 7, 101));
        auto hyps = generator.generate({road});
        ASSERT_TRUE(hyps.empty());
    }
    {
        // check funclass
        mwt::OnewayHypothesisGenerator generator(
            graph, persIdToTracks, mwt::OnewayGenParams(0.899, 6, 99));
        auto hyps = generator.generate({road});
        ASSERT_TRUE(hyps.empty());
    }
    {
        // check multiple days
        const bg::date_period twoDaysPeriod(bg::date(2018, bg::Jan, 30),
                                            bg::date(2018, bg::Feb, 01));

        mwt::PersIdToTracks persIdToTracksTwoDays(
            twoDaysPeriod,
            MapPersIdToTracks{
                {road.fwdPers, 90},
                {road.bkwdPers, 10}
            }
        );

        {
            // insufficient number of tracks
            mwt::OnewayHypothesisGenerator generator(
                graph, persIdToTracksTwoDays, mwt::OnewayGenParams(0.899, 7, 51));
            auto hyps = generator.generate({road});
            ASSERT_TRUE(hyps.empty());
        }
        {
            // sufficient number of tracks
            mwt::OnewayHypothesisGenerator generator(
                graph, persIdToTracksTwoDays, mwt::OnewayGenParams(0.899, 7, 50));
            auto hyps = generator.generate({road});
            ASSERT_TRUE(hyps.size() == 1);

            const auto& hyp = hyps.front();
            ASSERT_EQ(hyp.roadTraffic.bothEdgeTracks, 100);
            ASSERT_DOUBLE_EQ(hyp.roadTraffic.matchRatio, 0.9);
            ASSERT_TRUE(hyp.roadTraffic.days == twoDaysPeriod);
        }
    }

}

TEST(hyp_generator, single_reversed)
{
    mwt::TwoWayRoadIds road{1, 2, 1000, 2000};

    GraphFunclassStub graph(shortIdToFunclassMap{{road.fwd, 7}, {road.bkwd, 7}});

    mwt::PersIdToTracks persIdToTracks(ONE_DAY_PERIOD,
        MapPersIdToTracks{{road.fwdPers, 10}, {road.bkwdPers, 90}});

    mwt::OnewayHypothesisGenerator generator(
        graph, persIdToTracks, mwt::OnewayGenParams(0.899, 7, 99));

    auto hyps = generator.generate({road});
    ASSERT_TRUE(hyps.size() == 1);

    const auto& hyp = hyps.front();
    ASSERT_EQ(hyp.roadId, road.bkwd);
    ASSERT_EQ(hyp.roadTraffic.bothEdgeTracks, 100);
    ASSERT_DOUBLE_EQ(hyp.roadTraffic.matchRatio, 0.9);
}

TEST(hyp_generator, single_hypothesis_for_twoway_road)
{
    mwt::TwoWayRoadIds road{1, 2, 1000, 2000};

    GraphFunclassStub graph(shortIdToFunclassMap{{road.fwd, 7}, {road.bkwd, 7}});

    mwt::PersIdToTracks persIdToTracks(ONE_DAY_PERIOD,
        MapPersIdToTracks{{road.fwdPers, 50}, {road.bkwdPers, 50}});

    mwt::OnewayHypothesisGenerator generator(
        graph, persIdToTracks, mwt::OnewayGenParams(0.5, 7, 99));

    auto hyps = generator.generate({road});
    ASSERT_TRUE(hyps.size() == 1);
    ASSERT_EQ(hyps.front().roadId, road.fwd);
}
