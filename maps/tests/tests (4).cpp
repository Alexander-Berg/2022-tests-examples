#include <maps/wikimap/mapspro/libs/masstransit/masstransit.h>
#include <maps/wikimap/mapspro/libs/masstransit/db_helper.h>

#include <yandex/maps/wiki/masstransit/geobase.h>
#include <yandex/maps/wiki/masstransit/convert.h>

#include <yandex/maps/wiki/unittest/localdb.h>
#include <maps/libs/common/include/file_utils.h>
#include <yandex/maps/shell_cmd.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>

#include <boost/test/unit_test.hpp>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::masstransit;

namespace {

const std::string SCHEMA = "ymapsdf_masstransit";
const std::string OUT_SCHEMA = "ymapsdf_masstransit_out";
const std::string TAG = "masstransit_conversion";

class MasstransitFixture {
public:
    MasstransitFixture()
    {
        log8::setLevel(log8::Level::FATAL);

        setGeodataBinPath(BinaryPath("geobase/data/v6/geodata6.bin"));
    }
};

void createDir(const std::string& path)
{
    auto removeAndCreateCmd = "rm -fdr " + path + "; mkdir " + path;
    maps::shell::runCmd(removeAndCreateCmd);
}

std::string dataPath(const std::string& path)
{
    return ArcadiaSourceRoot() + "/maps/wikimap/mapspro/libs/masstransit/tests/" + path;
}

pgpool3::Pool& pool()
{
    static wiki::unittest::RandomDatabaseFixture fixture;
    return fixture.pool();
}

void populateYmapsdfSchema(const std::string& sqlDataPath)
{
    DBHelper ymapsdfDB(pool(), SCHEMA);
    ymapsdfDB.dropSchema();
    ymapsdfDB.createSchema();
    ymapsdfDB.execWrite(maps::common::readFileToString(sqlDataPath));
    ymapsdfDB.finalizeSchema();
}

void ymapsdf2mtrTest(
    const std::string& sourceYmapsdfSchema,
    const std::string& targetMtrDirectory)
{
    Params params;
    params.threadCount = 1;
    params.batchSize = 10;
    params.precision = 9;

    Masstransit mtrData(params);
    mtrData.readFromYmapsdf(pool(), sourceYmapsdfSchema);
    mtrData.writeToMtr(targetMtrDirectory);
}

void
compareFiles(const std::string& inPath, const std::string& outPath)
{
    for (const std::string& file : {
            "aliases",
            "boardings",
            "calendar",
            "connector_geometry",
            "connectors",
            "frequency",
            "geometry",
            "l10n",
            "routes",
            "stop_closures",
            "stops",
            "thread_closures",
            "thread_stops",
            "threads",
            "timetable",
            "transitions",
            "travel_time"
        })
    {
        auto checkCmd = "cat " + outPath + file + " | LC_ALL=C sort | diff " + inPath + file + " -";
        auto res = maps::shell::runCmd(checkCmd);
        BOOST_CHECK_MESSAGE(!res.exitCode && res.stdOut.empty() && res.stdErr.empty(),
            checkCmd + " failed" + "\n" + res.stdOut + "\n" + res.stdErr);
    }
}

} // namespace

BOOST_FIXTURE_TEST_SUITE(masstransit_tests, MasstransitFixture)

BOOST_AUTO_TEST_CASE(test_circular_thread)
{
    const std::string TEST_OUTPUT = "circular_thread_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("circular_thread.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("circular_thread_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_CASE(test_passageway)
{
    const std::string TEST_OUTPUT = "passageway_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("passageway.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("passageway_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_CASE(test_boarding)
{
    const std::string TEST_OUTPUT = "boardings_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("boardings.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("boardings_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_CASE(test_calendar)
{
    const std::string TEST_OUTPUT = "calendar_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("calendar.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("calendar_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_CASE(test_waterway_alias)
{
    const std::string TEST_OUTPUT = "waterway_alias_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("waterway_alias.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("waterway_alias_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_CASE(test_connector)
{
    const std::string TEST_OUTPUT = "connector_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("connector.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("connector_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_CASE(test_connector_real)
{
    const std::string TEST_OUTPUT = "connector_real_out/";
    createDir(TEST_OUTPUT);
    populateYmapsdfSchema(dataPath("connector_real.sql"));
    ymapsdf2mtrTest(SCHEMA, TEST_OUTPUT);
    compareFiles(dataPath("connector_real_canon/"), TEST_OUTPUT);
}

BOOST_AUTO_TEST_SUITE_END()
