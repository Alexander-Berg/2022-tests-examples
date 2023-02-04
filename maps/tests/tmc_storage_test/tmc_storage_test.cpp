#include "../common.h"

#include <maps/analyzer/libs/tie_here_jams/include/tmc_storage.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/system/fs.h>
#include <util/folder/path.h>

#include <fstream>
#include <iterator>
#include <string>
#include <unordered_set>

namespace tie_here_jams = maps::analyzer::tie_here_jams;


const std::vector<tie_here_jams::DirectedPoint> DIRECTED_POINTS = {
    {
        {"00001", tie_here_jams::Direction::Positive},
        {"00002", tie_here_jams::Direction::Negative},
        {"00003", tie_here_jams::Direction::Positive},
        {"00004", tie_here_jams::Direction::Positive},
        {"00005", tie_here_jams::Direction::Negative},
        {"00006", tie_here_jams::Direction::Positive},
        {"00006", tie_here_jams::Direction::Negative},
        {"00007", tie_here_jams::Direction::Positive},
        {"00007", tie_here_jams::Direction::Negative},
        {"00008", tie_here_jams::Direction::Positive},
        {"00008", tie_here_jams::Direction::Negative},
        {"00009", tie_here_jams::Direction::Positive},
        {"00009", tie_here_jams::Direction::Negative},
        {"00010", tie_here_jams::Direction::Positive},
        {"00010", tie_here_jams::Direction::Negative},
        {"00011", tie_here_jams::Direction::Positive},
        {"00011", tie_here_jams::Direction::Negative},
        {"00012", tie_here_jams::Direction::Positive},
        {"00013", tie_here_jams::Direction::Positive}
    }
};

const std::vector<std::vector<ui64>> ETALON_EDGES = {
    {
        10873262682359929558ULL, 10873262682359929558ULL, 10873262682359929558ULL, 16378444311340386993ULL,
        16378444311340386993ULL, 13435475319702971697ULL, 13435475319702971697ULL, 13435475319702971697ULL,
        13435475319702971697ULL
    },
    { 877355209943993629ULL, 877355209943993629ULL, 877355209943993629ULL, 416700464508219914ULL },
    {
        16411487673927225021ULL, 15154879470557438630ULL, 15154879470557438630ULL, 15154879470557438630ULL,
        15598270916575274046ULL, 11526115684705027436ULL, 11526115684705027436ULL
    },
    { 16087285915805411313ULL, 16087285915805411313ULL, 16087285915805411313ULL, 16087285915805411313ULL },
    { 1453334295354103554ULL, 5487149749186450710ULL, 14967731191842903291ULL },
    { 18037132934790512899ULL, 18037132934790512899ULL, 18037132934790512899ULL, 18037132934790512899ULL },
    { 3760090715597246240ULL, 3055173846579826771ULL },
    {
        1482355527026518573ULL, 5328411372760114335ULL, 5328411372760114335ULL, 5328411372760114335ULL,
        5328411372760114335ULL, 15222403211383155846ULL
    },
    { 5711182668803450855ULL, 5711182668803450855ULL },
    { 16442024230067758102ULL },
    {
        2261094319347613036ULL, 2261094319347613036ULL, 10980411800186778285ULL,
        10980411800186778285ULL, 16912403937708126063ULL
    },
    { 18326771655387889362ULL },
    { 6266515978358478331ULL, 6266515978358478331ULL, 6266515978358478331ULL },
    { 14502290403438491725ULL, 8884781982235698775ULL },
    { 5684880390749850443ULL },
    { 8922808941084935492ULL, 15028059096492132683ULL, 8734798266079440440ULL },
    {
        3685020537424854354ULL, 14077452553050185220ULL, 14077452553050185220ULL, 12138087720477429399ULL,
        12138087720477429399ULL
    },
    { 2136261151415466781ULL, 2136261151415466781ULL, 2239897677913592626ULL },
    {
        16442024230067758102ULL, 2261094319347613036ULL, 2261094319347613036ULL, 10980411800186778285ULL,
        10980411800186778285ULL, 16912403937708126063ULL, 16912403937708126063ULL, 11983376645419235762ULL,
        11983376645419235762ULL, 4112077417437598828ULL, 4112077417437598828ULL, 4112077417437598828ULL
    }
};

const std::vector<std::vector<ui64>> ETALON_EDGES_GROUPS_FOR_REGIONS = {
    {
        10873262682359929558ULL, 10873262682359929558ULL, 10873262682359929558ULL, 16378444311340386993ULL,
        16378444311340386993ULL, 13435475319702971697ULL, 13435475319702971697ULL, 13435475319702971697ULL,
        13435475319702971697ULL, 877355209943993629ULL, 877355209943993629ULL, 877355209943993629ULL,
        416700464508219914ULL
    }, // region 1
    {
        16411487673927225021ULL, 15154879470557438630ULL, 15154879470557438630ULL, 15154879470557438630ULL,
        15598270916575274046ULL, 11526115684705027436ULL, 11526115684705027436ULL,
        16087285915805411313ULL, 16087285915805411313ULL, 16087285915805411313ULL, 16087285915805411313ULL,
        1453334295354103554ULL, 5487149749186450710ULL, 14967731191842903291ULL,
        18037132934790512899ULL, 18037132934790512899ULL, 18037132934790512899ULL, 18037132934790512899ULL,
        3760090715597246240ULL, 3055173846579826771ULL,
        1482355527026518573ULL, 5328411372760114335ULL, 5328411372760114335ULL, 5328411372760114335ULL,
        5328411372760114335ULL, 15222403211383155846ULL,
        5711182668803450855ULL, 5711182668803450855ULL,
        16442024230067758102ULL,
        2261094319347613036ULL, 2261094319347613036ULL, 10980411800186778285ULL,
        10980411800186778285ULL, 16912403937708126063ULL,
        14502290403438491725ULL, 8884781982235698775ULL,
        8922808941084935492ULL, 15028059096492132683ULL, 8734798266079440440ULL,
        3685020537424854354ULL, 14077452553050185220ULL, 14077452553050185220ULL, 12138087720477429399ULL,
        12138087720477429399ULL,
        2136261151415466781ULL, 2136261151415466781ULL, 2239897677913592626ULL,
        16442024230067758102ULL, 2261094319347613036ULL, 2261094319347613036ULL, 10980411800186778285ULL,
        10980411800186778285ULL, 16912403937708126063ULL, 16912403937708126063ULL, 11983376645419235762ULL,
        11983376645419235762ULL, 4112077417437598828ULL, 4112077417437598828ULL, 4112077417437598828ULL
    } // region 213
};

void checkEdges(const tie_here_jams::MatchedTMCStorageReader& reader)
{
    auto etalonEdgesIterator = ETALON_EDGES.begin();

    for (const auto& [pointId, direction] : DIRECTED_POINTS) {
        const auto* edges = reader.getEdges(pointId, direction);
        EXPECT_EQ(edges->size(), etalonEdgesIterator->size());
        for (size_t idx = 0; idx < edges->size(); ++idx) {
            EXPECT_EQ((*edges)[idx].value(), (*etalonEdgesIterator)[idx]);
        }
        ++etalonEdgesIterator;
    }
}

void checkRegionId(const tie_here_jams::MatchedTMCStorageReader& reader)
{
    for (const auto& edgeLongId : ETALON_EDGES_GROUPS_FOR_REGIONS[0]) {
        EXPECT_EQ(reader.getRegionId(maps::road_graph::LongEdgeId(edgeLongId)), 1);
    }

    for (const auto& edgeLongId : ETALON_EDGES_GROUPS_FOR_REGIONS[1]) {
        EXPECT_EQ(reader.getRegionId(maps::road_graph::LongEdgeId(edgeLongId)), 213);
    }
}

void checkPolylines(const tie_here_jams::MatchedTMCStorageReader& reader)
{
    for (const auto& [directedPoint, canonPolyline] : CANON_DIRECTED_POINT_TO_POLYLINE) {
        auto polyline = reader.getPolyline(directedPoint.pointId, directedPoint.direction);
        EXPECT_EQUAL_POLYLINES(canonPolyline, polyline.value());
    }
}

Y_UNIT_TEST_SUITE(TmcStorageTest)
{
    Y_UNIT_TEST(MatchedTMCStorageReaderGetEdges)
    {
        tie_here_jams::MatchedTMCStorageReader reader;
        reader.read(STORAGE_FILE);
        checkEdges(reader);
    }

    Y_UNIT_TEST(MatchedTMCStorageReaderGetRegionId)
    {
        tie_here_jams::MatchedTMCStorageReader reader;
        reader.read(STORAGE_FILE);
        checkRegionId(reader);
    }

    Y_UNIT_TEST(MatchedTMCStorageReaderGetPolyline)
    {
        tie_here_jams::MatchedTMCStorageReader reader;
        reader.read(STORAGE_FILE);
        checkPolylines(reader);
    }

    Y_UNIT_TEST_F(MatchedTMCStorageBuilderTest, GeoIdFixture)
    {

        maps::road_graph::Graph graph{ROAD_GRAPH_DATA_PATH};
        maps::succinct_rtree::Rtree edgesRTree{EDGES_RTREE_PATH, graph};
        maps::road_graph::PersistentIndex persistentIndex{EDGES_PERSISTENT_INDEX_PATH};

        std::string outputPath("tmp.mms");
        tie_here_jams::MatchedTMCStorageBuilder matchedTMCStorageBuilder;
        matchedTMCStorageBuilder.save(
            outputPath, CANON_DIRECTED_POINT_TO_POLYLINE, graph, edgesRTree, persistentIndex, getGeoId(), 50.0
        );

        tie_here_jams::MatchedTMCStorageReader reader;
        reader.read(outputPath);

        checkEdges(reader);
        checkRegionId(reader);
        checkPolylines(reader);
    }
}
