#include "../boost_workaround.h"

#include "../config.h"
#include "../export_masstransit.h"
#include "../json2ymtrdf.h"
#include "../revision2json.h"
#include "../tools.h"

#include <maps/libs/pgpool/include/pgpool3.h>
#include <yandex/maps/shell_cmd.h>
#include <yandex/maps/wiki/masstransit/dump.h>
#include <yandex/maps/wiki/mds_dataset/dataset_gateway.h>
#include <yandex/maps/wiki/mds_dataset/export_metadata.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/unittest/unittest.h>

#include <boost/filesystem.hpp>

#define BOOST_AUTO_TEST_MAIN
#define BOOST_TEST_DYN_LINK
#include <boost/test/test_tools.hpp>
#include <boost/test/unit_test.hpp>

#include <fstream>
#include <string>
#include <utility>
#include <vector>

namespace fs = boost::filesystem;
namespace rev = maps::wiki::revision;
using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::export_mtr;

namespace maps {
namespace {

const std::string CONFIG_PATH = "./tests/services.local.export-mtr-tests.xml";
const std::string DATA_OBJECTS_PATH = "./tests/1.json";
const std::string TAG = "export_mtr";

const int USER_ID = 224829124; // naplavkov

class ExportMtrFixture : public unittest::DatabaseFixture {
public:
    ExportMtrFixture()
        : unittest::DatabaseFixture(CONFIG_PATH, TAG)
        , params_(1 /*task id*/, importObjects() /*head commit id*/, "trunk", IsTested::Yes)
        , cfg_(params_, configXml())
    {
        fs::remove_all(config().tempDir());
        fs::create_directory(config().tempDir());
        auto txn = pool().masterWriteableTransaction();
        txn->exec("DROP SCHEMA IF EXISTS " + config().schema() + " CASCADE");
        txn->commit();
    }

    ~ExportMtrFixture()
    {
        try {
            clearMds();
            fs::remove_all(config().tempDir());
        }
        catch (...) {
        }
    }

    const Config& config() const { return cfg_; }

private:
    Params params_;
    const Config cfg_;

    rev::DBID importObjects()
    {
        std::ostringstream cmd;
        cmd << "revisionapi"
            << " --cfg=" << CONFIG_PATH << " --cmd=import"
            << " --branch=0"
            << " --path=" << DATA_OBJECTS_PATH << " --user-id=" << USER_ID;
        auto res = shell::runCmd(cmd.str());
        REQUIRE(res.exitCode == 0,
                "Can't import test objects to db: " << res.stdErr);
        return getHeadCommitId();
    }

    rev::DBID getHeadCommitId()
    {
        auto txn = pool().masterReadOnlyTransaction();
        rev::RevisionsGateway gtw(*txn);
        return gtw.headCommitId();
    }

    void clearMds()
    {
        mds::Mds mdsClient(config().mdsConfig());
        mds_dataset::DatasetWriter<mds_dataset::ExportMetadata> writer(
            mdsClient, pool());
        writer.deleteDataset(config().tag());
    }
};

void checkFiles(const std::string& dirPath)
{
    size_t size = 0;
    for (fs::directory_iterator i(dirPath), end; i != end; ++i) {
        size += fileSize(i->path().string());
    }
    BOOST_CHECK_GT(size, 0);
}

size_t selectCount(pqxx::transaction_base& txn, const std::string& table)
{
    return txn.exec("SELECT COUNT(*) FROM " + table)
        .front()
        .front()
        .as<size_t>();
}

} // anonymous namespace
} // maps

BOOST_FIXTURE_TEST_SUITE(export_mtr_tests, ExportMtrFixture)

BOOST_AUTO_TEST_CASE(test_revisionapi)
{
    fs::create_directory(config().jsonDir());
    revision2json(config());
    checkFiles(config().jsonDir());
}

BOOST_AUTO_TEST_CASE(test_json2ymapsdf_n_masstransit)
{
    fs::create_directory(config().jsonDir());
    fs::copy_file(fs::current_path() / DATA_OBJECTS_PATH,
                  fs::path(config().jsonDir()) / "00000000.json");
    BOOST_REQUIRE(json2YMtrDF(config(), Json2YMtrDfMode::Upload));

    static const std::vector<std::pair<const std::string, int>>
        TABLE_TO_COUNT_MAP = {{"route", 1},
                              {"route_nm", 1},
                              {"route_source", 1},
                              {"stop", 8},
                              {"stop_nm", 4},
                              {"stop_source", 4},
                              {"thread", 2},
                              {"thread_el", 3},
                              {"thread_source", 2},
                              {"thread_stop", 6},
                              {"thread_thread_el", 6},
                              {"transition", 4},
                              {"transport_system", 1},
                              {"transport_system_nm", 1}};
    auto txn = config().ymtrdfPool().masterReadOnlyTransaction();
    for (const auto& tableToCount : TABLE_TO_COUNT_MAP) {
        BOOST_CHECK_EQUAL(
            selectCount(*txn, config().schema() + "." + tableToCount.first),
            tableToCount.second);
    }

    fs::create_directory(config().dumpDir());
    mtr::dump(*txn, config().schema(), config().dumpDir());
    checkFiles(config().dumpDir());
}

BOOST_AUTO_TEST_CASE(test_archive)
{
    fs::create_directory(config().dumpDir());
    fs::copy_file(fs::current_path() / DATA_OBJECTS_PATH,
                  fs::path(config().dumpDir()) / "somefile1");
    fs::copy_file(fs::current_path() / DATA_OBJECTS_PATH,
                  fs::path(config().dumpDir()) / "somefile2");
    archive(config().dumpDir(), config().resultFile());
    BOOST_CHECK_GT(fileSize(config().resultFile()), 0);
}

BOOST_AUTO_TEST_CASE(test_md5)
{
    auto file = fs::current_path() / DATA_OBJECTS_PATH;
    makeMd5(file.string(), config().md5File());
    BOOST_CHECK_GT(fileSize(config().md5File()), 0);
}

BOOST_AUTO_TEST_CASE(test_publish)
{
    auto file = fs::current_path() / DATA_OBJECTS_PATH;
    BOOST_CHECK_EQUAL(publish(config(),
                              mds_dataset::DatasetStatus::Available,
                              {file.string()}).size(),
                      1);
}

BOOST_AUTO_TEST_CASE(test_export_masstransit_impl)
{
    BOOST_CHECK(exportMasstransit(config()).status == ExportStatus::SUCCESS);
    BOOST_CHECK(!fs::exists(config().tempDir()));
}

BOOST_AUTO_TEST_SUITE_END()
