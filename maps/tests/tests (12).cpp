#include <maps/wikimap/mapspro/services/dataset-explorer/lib/json.h>

#include <yandex/maps/wiki/mds_dataset/dataset.h>
#include <yandex/maps/wiki/mds_dataset/export_metadata.h>
#include <yandex/maps/wiki/mds_dataset/dataset_gateway.h>
#include <yandex/maps/wiki/unittest/localdb.h>
#include <yandex/maps/mrc/unittest/local_server.h>
#include <yandex/maps/shell_cmd.h>
#include <maps/libs/json/include/value.h>
#include <yandex/maps/mds/mds.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/filesystem.hpp>

#include <chrono>
#include <fstream>
#include <map>
#include <set>
#include <vector>

namespace fs = boost::filesystem;
namespace md = maps::wiki::mds_dataset;

namespace maps {
namespace wiki {
namespace dataset_explorer {
namespace tests {
namespace {

const std::string MDS_HOST = "127.0.0.1";
const std::string NAMESPACE = "mpro-dataset";
const std::string AUTH_HEADER = "Basic bXByby1kYXRhc2V0OjhmZTU5ZGNjMzUzMzc4ODdkNzIxOWE4M2IwNWI4ZWRk";
const std::string DUMMY_DATA = "lorem ipsum";

const std::string SCHEMAS_DIR = ArcadiaSourceRoot() + "/maps/wikimap/mapspro/schemas/dataset-explorer/";
const std::string JSON_VALIDATOR = BinaryPath("maps/tools/json-validator/json-validator");

const std::string TMP_DATASETS_DIR = "tmp-datasets";

void validateJson(const std::string& json, const std::string& schemaPath)
{
    auto command = JSON_VALIDATOR + " -s " + schemaPath;
    auto result = shell::runCmd(command, json, std::chrono::milliseconds(-1));
    REQUIRE(result.exitCode == 0, "json-validator failed:"
                << " stdOut: " << result.stdOut
                << " stdErr: " << result.stdErr);
}

mds::Configuration makeMdsConfig(std::uint16_t mdsPort)
{
    mds::Configuration conf(MDS_HOST, NAMESPACE, AUTH_HEADER);
    conf.setTimeout(std::chrono::milliseconds(5000));
    conf.setWritePort(mdsPort);
    conf.setReadPort(mdsPort);
    return conf;
}

class Fixture
    : public unittest::MapsproDbFixture
    , public mrc::unittest::MdsStubFixture
{
public:
    Fixture()
        : unittest::MapsproDbFixture()
        , mrc::unittest::MdsStubFixture()
        , mdsClient(makeMdsConfig(getMdsPort()))
    {
        fs::create_directories(TMP_DATASETS_DIR);
    }

    virtual ~Fixture()
    {
        fs::remove_all(TMP_DATASETS_DIR);
    }

    mds::Mds mdsClient;
};

void createFile(const std::string& filename, const std::string& text)
{
    std::ofstream file(filename);
    file << text;
}

} // namespace

Y_UNIT_TEST_SUITE(dataset_explorer) {

Y_UNIT_TEST(test_find_region)
{
    const std::string BASE_URL = "http://storage-int.mds.yandex.net:80/get-mpro-dataset/";

    UNIT_ASSERT_STRINGS_EQUAL(findRegion(
        BASE_URL + "112030/export/20160208_085631_0_20392304/json.tar.gz"),
        "");

    UNIT_ASSERT_STRINGS_EQUAL(findRegion(
        BASE_URL + "50206/export_turkey_mpro/20170224_000066_1_59961/ymapsdf2.dump.tar.gz"),
        "turkey_mpro");

    UNIT_ASSERT_STRINGS_EQUAL(findRegion(
        BASE_URL + "50206/export_and_turkey_mpro/20170224_000066_1_59961/ymapsdf2.dump.tar.gz"),
        "and_tr");

    UNIT_ASSERT_STRINGS_EQUAL(findRegion(
        BASE_URL + "50206/export/20170124_107509_372_40105570/export_mtr.tar.gz"),
        "");

    UNIT_ASSERT_STRINGS_EQUAL(findRegion(
        BASE_URL + "50206/export_and_mpro/20170227_000067_0_61145/ymapsdf2.dump.tar.gz"),
        "and_mpro");
}

Y_UNIT_TEST(test_dataset_explorer)
{
    Fixture fixture;

    md::DatasetWriter<md::ExportMetadata> dsWriter(fixture.mdsClient, fixture.pool());

    md::DatasetID id = "test-dataset";
    md::ExportMetadata metadata(
        md::BasicMetadata(id, md::DatasetStatus::Available),
        md::Subset::Ymapsdf,
        md::IsTested::Yes);

    std::vector<std::string> files {
        "dataset-file-1.txt",
        "dataset-file-2.txt",
        "dataset-file-3.txt"
    };

    std::vector<std::string> filePaths;
    std::transform(
        files.begin(),
        files.end(),
        std::back_inserter(filePaths),
        [](const std::string& name) {
            return (fs::path(TMP_DATASETS_DIR) / name).string();
        });

    for (const auto& file : filePaths) {
        createFile(file, DUMMY_DATA);
    }

    std::vector<md::Dataset<md::ExportMetadata>> datasets {
        dsWriter.createDataset(std::move(metadata), std::move(filePaths))
    };

    int64_t offset = 0;
    int32_t limit = 10;
    auto jsonResponse = makeJsonDatasetsList(datasets, offset, limit);

    validateJson(jsonResponse,
        SCHEMAS_DIR + "get_export_datasets_response.schema.json");

    auto resp = json::Value::fromString(jsonResponse);

    UNIT_ASSERT_VALUES_EQUAL(resp["offset"].as<int64_t>(), offset);
    UNIT_ASSERT_VALUES_EQUAL(resp["limit"].as<int32_t>(), limit);
    UNIT_ASSERT_VALUES_EQUAL(resp["datasets"].size(), 1);

    json::Value jsonDataset = resp["datasets"][0];
    UNIT_ASSERT_VALUES_EQUAL(jsonDataset["name"].as<std::string>(), id);
    UNIT_ASSERT_VALUES_EQUAL(jsonDataset["status"].as<std::string>(), "available");
    UNIT_ASSERT_VALUES_EQUAL(jsonDataset["files"].size(), 3);

    std::vector<std::string> fileNames;
    for (const auto& jsonFile: jsonDataset["files"]) {
        fileNames.push_back(jsonFile["name"].as<std::string>());
    }

    UNIT_ASSERT_EQUAL(fileNames, files);
}

} // Y_UNIT_TEST_SUITE

} // tests
} // dataset_explorer
} // wiki
} // maps

