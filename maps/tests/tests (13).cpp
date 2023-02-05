#include <yandex/maps/wiki/masstransit/dump.h>

#include "../search_path_guard.h"

#include <maps/libs/pgpool/include/pgpool3.h>
#include <yandex/maps/shell_cmd.h>
#include <yandex/maps/wiki/unittest/unittest.h>

#define BOOST_AUTO_TEST_MAIN
#define BOOST_TEST_DYN_LINK
#include <boost/test/test_tools.hpp>
#include <boost/test/unit_test.hpp>

#include <fstream>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::mtr;

namespace maps {
namespace {

const std::string CONFIG_PATH
    = "../../cfg/services/services.local.unit_tests.xml";
const std::string DATA_PATH = "./tests/data.sql";
const std::string SCHEMA = "ymtrdf";
const std::string SCHEMA_DIR = "/usr/share/yandex/maps/ymtrdf/";
const std::string TAG = "masstransit";

class MasstransitFixture : public unittest::DatabaseFixture {
public:
    MasstransitFixture() : unittest::DatabaseFixture(CONFIG_PATH, TAG)
    {
        auto txn = pool().masterWriteableTransaction();
        {
            SearchPathGuard searchPathGuard{*txn, SCHEMA};
            createSchema(*txn);
            insertData(*txn);
            finalizeSchema(*txn);
        }
        txn->commit();
    }

private:
    std::string readFile(const std::string& file)
    {
        std::ifstream from(file);
        std::ostringstream to;
        std::copy(std::istreambuf_iterator<char>(from),
                  std::istreambuf_iterator<char>(),
                  std::ostreambuf_iterator<char>(to));
        return to.str();
    }

    void createSchema(pqxx::transaction_base& txn)
    {
        std::ostringstream query;
        query << "DROP SCHEMA IF EXISTS " << SCHEMA << " CASCADE;"
              << "CREATE SCHEMA " << SCHEMA << ";"
              << readFile(SCHEMA_DIR + "ymtrdf_create.sql");
        txn.exec(query.str());
    }

    void insertData(pqxx::transaction_base& txn)
    {
        txn.exec(readFile(DATA_PATH));
    }

    void finalizeSchema(pqxx::transaction_base& txn)
    {
        std::ostringstream query;
        query << readFile(SCHEMA_DIR + "ymtrdf_finalize.sql")
              << readFile(SCHEMA_DIR + "ymtrdf_integrity.sql");
        txn.exec(query.str());
    }
};

} // anonymous namespace
} // namespace maps

BOOST_FIXTURE_TEST_SUITE(masstransit_tests, MasstransitFixture)

BOOST_AUTO_TEST_CASE(test_search_path_guard)
{
    auto txn = pool().masterReadOnlyTransaction();
    auto currentSearchPath = [&txn] {
        return txn->exec("SHOW search_path;").front().front().c_str();
    };
    const std::string initSearchPath = currentSearchPath();
    {
        SearchPathGuard searchPathGuard(*txn, SCHEMA);
        BOOST_CHECK(initSearchPath != currentSearchPath());
    }
    BOOST_CHECK(initSearchPath == currentSearchPath());
}

BOOST_AUTO_TEST_CASE(test_dump)
{
    {
        auto txn = pool().masterReadOnlyTransaction();
        dump(*txn, SCHEMA, "./tests");
    }
    for (const std::string path : {"./tests/aliases",
                                   "./tests/calendar",
                                   "./tests/frequency",
                                   "./tests/geometry",
                                   "./tests/l10n",
                                   "./tests/routes",
                                   "./tests/stops",
                                   "./tests/threads",
                                   "./tests/thread_stops",
                                   "./tests/timetable",
                                   "./tests/transitions"}) {
        auto checkCmd = "cmp " + path + " " + path + ".canon";
        auto res = maps::shell::runCmd(checkCmd);
        BOOST_CHECK_MESSAGE(!res.exitCode && res.stdOut.empty()
                            && res.stdErr.empty(),
                            checkCmd + " failed");
    }
}

BOOST_AUTO_TEST_SUITE_END()
