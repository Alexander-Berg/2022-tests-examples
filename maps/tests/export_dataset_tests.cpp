#include <yandex/maps/wiki/mds_dataset/dataset.h>
#include <yandex/maps/wiki/mds_dataset/exception.h>
#include <yandex/maps/wiki/mds_dataset/export_metadata.h>
#include <yandex/maps/wiki/mds_dataset/filter.h>
#include <yandex/maps/wiki/mds_dataset/dataset_gateway.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <yandex/maps/mrc/unittest/local_server.h>
#include <yandex/maps/mds/mds.h>
#include <maps/libs/log8/include/log8.h>

#include <boost/test/unit_test.hpp>

#include <boost/filesystem.hpp>

#include <chrono>
#include <fstream>
#include <set>

namespace fs = boost::filesystem;

namespace maps {
namespace wiki {
namespace mds_dataset {
namespace tests {
namespace {

const std::string MDS_HOST = "127.0.0.1";
const std::string NAMESPACE = "mpro-dataset";
const std::string AUTH_HEADER = "Basic bXByby1kYXRhc2V0OjhmZTU5ZGNjMzUzMzc4ODdkNzIxOWE4M2IwNWI4ZWRk";
const std::string DUMMY_DATA = "lorem ipsum";

struct SetLogLevelFixture
{
    SetLogLevelFixture()
    {
        log8::setLevel(log8::Level::FATAL);
    }
};

BOOST_GLOBAL_FIXTURE(SetLogLevelFixture);

class Fixture
    : public unittest::ArcadiaDbFixture
    , public mrc::unittest::MdsStubFixture
{
public:
    Fixture()
        : unittest::ArcadiaDbFixture()
        , mrc::unittest::MdsStubFixture()
        , mdsClient(makeMdsConfig(getMdsPort()))
    {
    }

    mds::Mds mdsClient;

private:
    static mds::Configuration makeMdsConfig(std::uint16_t mdsPort)
    {
        mds::Configuration conf(MDS_HOST, NAMESPACE, AUTH_HEADER);
        conf.setTimeout(std::chrono::milliseconds(5000));
        conf.setWritePort(mdsPort);
        conf.setReadPort(mdsPort);
        return conf;
    }
};

void createFile(const std::string& filename, const std::string& text)
{
    std::ofstream file(filename);
    file << text;
}

Timestamp getCurrentTs()
{
    return std::chrono::system_clock::now();
}

} // namespace

BOOST_FIXTURE_TEST_SUITE(mds_dataset, Fixture)

BOOST_AUTO_TEST_CASE(test_create_read_delete_export_dataset)
{
    DatasetWriter<ExportMetadata> dsWriter(mdsClient, pool());

    DatasetID id = "test-dataset-1";
    Region region = "russia";
    ExportMetadata metadata(
        BasicMetadata(id, region, DatasetStatus::Available),
        Subset::Ymapsdf,
        IsTested::Yes);
    const std::string file = "dataset-file.txt";
    createFile(file, DUMMY_DATA);

    auto ds = dsWriter.createDataset(metadata, {file});
    BOOST_ASSERT(ds.fileLinks().size() == 1);
    BOOST_CHECK_EQUAL(ds.fileLinks()[0].name(), fs::path(file).filename());

    auto readTxn = pool().slaveTransaction();
    auto readDs = DatasetReader<ExportMetadata>::dataset(*readTxn, id, region);
    BOOST_CHECK(readDs == ds);
    BOOST_ASSERT(readDs.fileLinks().size() == 1);

    dsWriter.deleteDataset(id, region);
    fs::remove(file);
}

BOOST_AUTO_TEST_CASE(test_export_dataset_multiple_files)
{
    DatasetWriter<ExportMetadata> dsWriter(mdsClient, pool());

    DatasetID id = "test-dataset-2";
    ExportMetadata metadata(
        BasicMetadata(id, DatasetStatus::Available),
        Subset::Ymapsdf,
        IsTested::Yes);
    std::vector<std::string> files {
        "dataset-file-1.txt",
        "dataset-file-2.txt",
        "dataset-file-3.txt"
    };
    for (const auto& file : files) {
        createFile(file, DUMMY_DATA);
    }

    auto ds = dsWriter.createDataset(metadata, files);
    BOOST_ASSERT(ds.fileLinks().size() == 3);

    auto readTxn = pool().slaveTransaction();
    auto readDs = DatasetReader<ExportMetadata>::dataset(*readTxn, id);
    BOOST_CHECK(readDs == ds);

    std::set<std::string> expected, got;
    for (size_t i = 0; i < files.size(); ++i) {
        expected.insert(fs::path(fs::path(files[i]).filename()).string());
        got.insert(ds.fileLinks()[i].name());
    }
    BOOST_CHECK_EQUAL_COLLECTIONS(
            expected.begin(), expected.end(),
            got.begin(), got.end());

    dsWriter.deleteDataset(id);
    for (const auto& file : files) {
        fs::remove(file);
    }
}

BOOST_AUTO_TEST_CASE(test_export_dataset_error_cases)
{
    DatasetWriter<ExportMetadata> dsWriter(mdsClient, pool());

    DatasetID id = "test-dataset-3";
    ExportMetadata metadata(
        BasicMetadata(id, DatasetStatus::Available),
        Subset::Ymapsdf,
        IsTested::Yes);

    // Try creating dataset without files
    BOOST_CHECK_THROW(
        dsWriter.createDataset(metadata, {}),
        MdsDatasetError);

    // Try creating dataset without non-existing file
    BOOST_CHECK_THROW(
        dsWriter.createDataset(metadata, {"non-existing-file-42.gz"}),
        InvalidDatasetFile);

    std::string file = "dataset-file.txt";
    createFile(file, DUMMY_DATA);

     // Try creating dataset with invalid metadata
    ExportMetadata invalidMetadata1(
        BasicMetadata(id, static_cast<DatasetStatus>(43)),
        Subset::Ymapsdf,
        IsTested::Yes);
    BOOST_CHECK_THROW(
        dsWriter.createDataset(invalidMetadata1, {file}),
        InvalidMetadata);

    ExportMetadata invalidMetadata2(
        BasicMetadata(id, DatasetStatus::Available),
        static_cast<Subset>(34),
        IsTested::Yes);
    BOOST_CHECK_THROW(
        dsWriter.createDataset(invalidMetadata2, {file}),
        InvalidMetadata);

    // Create a valid dataset
    dsWriter.createDataset(metadata, {file});

    // Try creating a duplicate dataset
    BOOST_CHECK_THROW(
        dsWriter.createDataset(metadata, {file}),
        maps::Exception);

    dsWriter.deleteDataset(id);
    fs::remove(file);
}

BOOST_AUTO_TEST_CASE(test_export_dataset_not_found)
{
    DatasetWriter<ExportMetadata> dsWriter(mdsClient, pool());

    auto readTxn = pool().slaveTransaction();
    BOOST_CHECK_THROW(
        DatasetReader<ExportMetadata>::dataset(*readTxn, "non-existing-dataset"),
        DatasetNotFound);
}

BOOST_AUTO_TEST_CASE(test_get_export_dataset_by_subset)
{
    DatasetWriter<ExportMetadata> dsWriter(mdsClient, pool());

    const std::string file = "dataset-file.txt";
    createFile(file, DUMMY_DATA);

    auto now = getCurrentTs();
    auto hourAgo = now - std::chrono::hours(1);
    auto minuteAgo = now - std::chrono::minutes(1);

    // Create several datasets
    DatasetID id4 = "test-dataset-4";
    ExportMetadata metadata4(
        BasicMetadata(id4, DatasetStatus::Available, hourAgo),
        Subset::Ymapsdf,
        IsTested::Yes);
    dsWriter.createDataset(metadata4, {file});

    DatasetID id5 = "test-dataset-5";
    Region region5 = "russia";
    ExportMetadata metadata5(
        BasicMetadata(id5, region5, DatasetStatus::Available, now),
        Subset::Service,
        IsTested::Yes);
    dsWriter.createDataset(metadata5, {file});

    DatasetID id6 = "test-dataset-6";
    ExportMetadata metadata6(
        BasicMetadata(id6, DatasetStatus::Available, now),
        Subset::Ymapsdf,
        IsTested::Yes);
    dsWriter.createDataset(metadata6, {file});

    auto readTxn = pool().slaveTransaction();
    // Get all datasets
    {
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn);

        BOOST_ASSERT(gotDatasets.size() == 3);
        std::set<DatasetID> expected{id4, id5, id6}, got;
        for (const auto& ds : gotDatasets) {
            got.insert(ds.id());
        }
        BOOST_CHECK_EQUAL_COLLECTIONS(
                expected.begin(), expected.end(),
                got.begin(), got.end());
    }

    // Get only region=russia datasets
    {
        ExportFilter filter(*readTxn);
        filter.byRegion(region5);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter);

        BOOST_ASSERT(gotDatasets.size() == 1);
        BOOST_CHECK_EQUAL(gotDatasets.front().id(), id5);
    }

    // Get only Ymapsdf datasets
    {
        ExportFilter filter(*readTxn);
        filter.bySubset(Subset::Ymapsdf);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter);

        BOOST_ASSERT(gotDatasets.size() == 2);
        std::set<DatasetID> expected{id4, id6}, got;
        for (const auto& ds : gotDatasets) {
            got.insert(ds.id());
        }
        BOOST_CHECK_EQUAL_COLLECTIONS(
                expected.begin(), expected.end(),
                got.begin(), got.end());
    }

    // Delete one dataset and get only Available datasets
    dsWriter.deleteDataset(id6);
    {
        ExportFilter filter(*readTxn);
        filter.byStatus(DatasetStatus::Available);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter);

        BOOST_ASSERT(gotDatasets.size() == 2);
        std::set<DatasetID> expected{id4, id5}, got;
        for (const auto& ds : gotDatasets) {
            got.insert(ds.id());
        }
        BOOST_CHECK_EQUAL_COLLECTIONS(
                expected.begin(), expected.end(),
                got.begin(), got.end());
    }

    // Get datasets not older than one minute ago
    {
        ExportFilter filter(*readTxn);
        filter.createdAfter(minuteAgo);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter);

        BOOST_ASSERT(gotDatasets.size() == 2);
        std::set<DatasetID> expected{id5, id6}, got;
        for (const auto& ds : gotDatasets) {
            got.insert(ds.id());
        }
        BOOST_CHECK_EQUAL_COLLECTIONS(
                expected.begin(), expected.end(),
                got.begin(), got.end());
    }

    // Combined filter: get only Available Ymapsdf datasets
    {
        ExportFilter filter(*readTxn);
        filter.byStatus(DatasetStatus::Available).bySubset(Subset::Ymapsdf);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter);

        BOOST_ASSERT(gotDatasets.size() == 1);
        BOOST_CHECK_EQUAL(gotDatasets[0].id(), id4);
    }

    // Filter that selects no datasets at all
    {
        ExportFilter filter(*readTxn);
        filter.byStatus(DatasetStatus::Available)
              .bySubset(Subset::Ymapsdf)
              .createdAfter(minuteAgo);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter);
        BOOST_CHECK(gotDatasets.empty());
    }

    // Request datasets with limits = 2: the most recent are selected
    {
        ExportFilter filter(*readTxn);
        auto gotDatasets = DatasetReader<ExportMetadata>::datasets(*readTxn, filter, 2, 0);

        BOOST_ASSERT(gotDatasets.size() == 2);
        std::set<DatasetID> expected{id5, id6}, got;
        for (const auto& ds : gotDatasets) {
            got.insert(ds.id());
        }
        BOOST_CHECK_EQUAL_COLLECTIONS(
                expected.begin(), expected.end(),
                got.begin(), got.end());
    }

    dsWriter.deleteDataset(id4);
    dsWriter.deleteDataset(id5, region5);
    fs::remove(file);
}

BOOST_AUTO_TEST_CASE(test_create_read_delete_export_dataset_multiple_region)
{
    DatasetWriter<ExportMetadata> dsWriter(mdsClient, pool());

    DatasetID id = "test-dataset-7";
    Region region1 = "russia";
    Region region2 = "cis1";
    ExportMetadata metadata1(
        BasicMetadata(id, region1, DatasetStatus::Available),
        Subset::Ymapsdf,
        IsTested::Yes);
    ExportMetadata metadata2(
        BasicMetadata(id, region2, DatasetStatus::Available),
        Subset::Ymapsdf,
        IsTested::Yes);
    const std::string file1 = "dataset-file-1.txt";
    const std::string file2 = "dataset-file-2.txt";
    createFile(file1, DUMMY_DATA);
    createFile(file2, DUMMY_DATA);

    auto ds1 = dsWriter.createDataset(metadata1, {file1});
    BOOST_CHECK_EQUAL(ds1.fileLinks().size(), 1);
    BOOST_ASSERT(ds1.fileLinks().size() == 1);
    BOOST_CHECK_EQUAL(ds1.fileLinks()[0].name(), fs::path(file1).filename());

    auto ds2 = dsWriter.createDataset(metadata2, {file2});
    BOOST_CHECK_EQUAL(ds2.fileLinks().size(), 1);
    BOOST_CHECK_EQUAL(ds2.fileLinks()[0].name(), fs::path(file2).filename());

    auto readTxn1 = pool().slaveTransaction();
    auto readDs1 = DatasetReader<ExportMetadata>::dataset(*readTxn1, id, region1);
    BOOST_CHECK(readDs1 == ds1);
    BOOST_ASSERT(readDs1.fileLinks().size() == 1);

    dsWriter.deleteDataset(id, region1);

    auto readTxn2 = pool().slaveTransaction();
    auto readDs2 = DatasetReader<ExportMetadata>::dataset(*readTxn2, id, region2);
    BOOST_CHECK(readDs2 == ds2);
    BOOST_ASSERT(readDs2.fileLinks().size() == 1);

    dsWriter.deleteDataset(id, region2);

    fs::remove(file1);
    fs::remove(file2);
}


BOOST_AUTO_TEST_SUITE_END()

} // tests
} // mds_dataset
} // wiki
} // maps
