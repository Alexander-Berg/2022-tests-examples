#pragma once

#include <maps/analyzer/libs/tie_here_jams/include/here_data_reader.h>
#include <maps/analyzer/libs/geoinfo/include/geoid.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/system/fs.h>

#include <optional>


const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/tie_here_jams/tests/data/";
const TString COVERAGE_DIR = "coverage5";

const std::string ROAD_GRAPH_DATA_PATH = BinaryPath("maps/data/test/graph3/road_graph.fb");
const std::string EDGES_RTREE_PATH = BinaryPath("maps/data/test/graph3/rtree.fb");
const std::string EDGES_PERSISTENT_INDEX_PATH = BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");

const std::string STORAGE_FILE = TEST_DATA_ROOT + "tied_here_test_graph.mms";
const std::string JAMS_XML_FILE = TEST_DATA_ROOT + "jams.xml";
const std::string RESULT_JAMS_FILE = TEST_DATA_ROOT + "jams.bin";
const std::string BAN_FILE = TEST_DATA_ROOT + "ban.yson";

const std::string STREETS_SHP_FILE = TEST_DATA_ROOT + "Streets.shp";
const std::string STREETS_DBF_FILE = TEST_DATA_ROOT + "Streets.dbf";
const std::string TRAFFIC_DBF_FILE = TEST_DATA_ROOT + "Traffic.dbf";

struct GeoIdFixture : public NUnitTest::TBaseFixture {
    GeoIdFixture(): coveragePath(ConstRef(COVERAGE_DIR)) {
        NFs::MakeDirectory(coveragePath.c_str());
        NFs::SymLink(BinaryPath("maps/data/test/geoid/geoid.mms.1"), (TFsPath(coveragePath.native()) / "geoid.mms.1").GetPath());
        geoId = maps::geoinfo::GeoId(coveragePath, "geoid");
    }

    const maps::geoinfo::GeoId& getGeoId() const {
        ASSERT(geoId.has_value());
        return *geoId;
    }

    std::filesystem::path coveragePath;
    std::optional<maps::geoinfo::GeoId> geoId;
};

const auto EXPECT_EQUAL_POLYLINES = [](
    const maps::analyzer::tie_here_jams::Polyline& lhs, const maps::analyzer::tie_here_jams::Polyline& rhs
) {
    const auto& lhsPoints = lhs.points();
    const auto& rhsPoints = rhs.points();
    EXPECT_EQ(lhsPoints.size(), rhsPoints.size());
    for (size_t idx = 0; idx < lhsPoints.size(); ++idx) {
        EXPECT_NEAR(lhsPoints[idx].x(), rhsPoints[idx].x(), 1e-5);
        EXPECT_NEAR(lhsPoints[idx].y(), rhsPoints[idx].y(), 1e-5);
    }
};

const maps::analyzer::tie_here_jams::DirectedPoint2Polyline CANON_DIRECTED_POINT_TO_POLYLINE =
{
    {
        {"00001", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.79138499,55.60461487}, {37.791372,55.60462686}, {37.79134677,55.60474827}, {37.79137015,55.60488988}, {37.79137393,55.60491281}, {37.79146705,55.60516338}, {37.79153276,55.60534024}, {37.79153695,55.60555318}, {37.79149647,55.60582048}, {37.79115004,55.60660184}
        })
    },
    {
        {"00002", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.75478021,55.59156714}, {37.75480385,55.59156224}, {37.75488172,55.59151806}, {37.75511792,55.59135038}, {37.75543777,55.59112332}
        })
    },
    {
        {"00003", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.67261451,55.63477447}, {37.67282045,55.63488}, {37.67302112,55.63499516}, {37.67318096,55.63491868}, {37.67307451,55.63485527}, {37.67299974,55.63481449}, {37.67285264,55.63474333}
        })
    },
    {
        {"00004", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.68023643,55.63258771}, {37.68017792,55.63246777}, {37.68008522,55.6322775}, {37.67996971,55.63214863}, {37.67991263,55.63209846}, {37.67983661,55.63204109}
        })
    },
    {
        {"00005", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.72087391,55.61427791}, {37.72060434,55.61423935}, {37.72008324,55.61527586}
        })
    },
    {
        {"00006", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.66244274,55.64521509}, {37.66282219,55.64508727}, {37.66285504,55.64503249}, {37.66286912,55.6450058}, {37.66287684,55.64498078}, {37.66287709,55.64494633}, {37.66286938,55.64488082}
        })
    },
    {
        {"00006", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.66286938,55.64488082}, {37.66273342,55.64489092}, {37.66273652,55.64495182}, {37.66273183,55.64498622}, {37.66272185,55.64502059}, {37.66270149,55.64505303}, {37.66267969,55.64507298}, {37.66264424,55.64509959}, {37.66244274,55.64521509}
        })
    },
    {
        {"00007", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.71919761,55.61755058}, {37.72006589,55.61770376}, {37.7194955,55.61881633}, {37.71935075,55.61908187}
        })
    },
    {
        {"00007", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.71996045,55.61604024}, {37.71970564,55.61600018}, {37.71894422,55.61750586}, {37.71919761,55.61755058}
        })
    },
    {
        {"00008", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.67804045,55.64357006}, {37.67782227,55.64364994}, {37.67768196,55.64353343}, {37.68014682,55.64262349}
        })
    },
    {
        {"00008", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.68014682,55.64262349}, {37.68065737,55.64243268}, {37.68138215,55.64216177}, {37.68185313,55.64198194}, {37.68231338,55.64180211}, {37.68332315,55.64139705}
        })
    },
    {
        {"00009", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.74703056,55.59473198}, {37.74633989,55.59475264}
        })
    },
    {
        {"00009", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.74667793,55.59611755}, {37.74666418,55.59601009}, {37.7466843,55.59591898}, {37.74703056,55.59473198}
        })
    },
    {
        {"00010", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.66507063,55.63566689}, {37.66548168,55.63564719}, {37.66584411,55.63562984}
        })
    },
    {
        {"00010", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.66571092,55.63648685}, {37.66584411,55.63562984}
        })
    },
    {
        {"00011", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.69940634,55.6199408}, {37.6993869,55.62000308}, {37.70129143,55.62017688}, {37.70119244,55.62043441}
        })
    },
    {
        {"00011", maps::analyzer::tie_here_jams::Direction::Negative},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.70119244,55.62043441}, {37.70371506,55.62070343}, {37.70434622,55.62079957}, {37.70437421,55.62080385}, {37.70441436,55.62081692}
        })
    },
    {
        {"00012", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.67099689,55.64206345}, {37.67124424,55.64227577}, {37.6698888,55.64281204}
        })
    },
    {
        {"00013", maps::analyzer::tie_here_jams::Direction::Positive},
        maps::analyzer::tie_here_jams::Polyline(maps::geolib3::PointsVector{
            {37.67804045,55.64357006}, {37.67782227,55.64364994}, {37.67768196,55.64353343}, {37.68014682,55.64262349}, {37.68065737,55.64243268}, {37.68138215,55.64216177}, {37.68185313,55.64198194}, {37.68231338,55.64180211}, {37.68332315,55.64139705}, {37.6835658,55.64129102}, {37.68474723,55.64080839}, {37.68563345,55.64044638}, {37.68742232,55.63970739}
        })
    }
};
