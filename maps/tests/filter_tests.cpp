#include <yandex/maps/wiki/mds_dataset/filter.h>
#include <yandex/maps/wiki/mds_dataset/export_metadata.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <maps/libs/log8/include/log8.h>
#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace mds_dataset {
namespace tests {

struct SetLogLevelFixture
{
    SetLogLevelFixture()
    {
        log8::setLevel(log8::Level::FATAL);
    }
};

BOOST_GLOBAL_FIXTURE(SetLogLevelFixture);

BOOST_FIXTURE_TEST_CASE(test_export_filter_1, unittest::ArcadiaDbFixture)
{
    auto txn = pool().slaveTransaction();
    {
        ExportFilter filter(*txn);
        filter.byId("test-dataset-id");
        filter.byStatus(DatasetStatus::Available);

        const std::string expected =
            "WHERE mds_dataset.export_meta.id = 'test-dataset-id'\n"
            "AND mds_dataset.export_meta.status = 0";

        BOOST_CHECK_EQUAL(filter.toSql(), expected);
    }
    {
        ExportFilter filter(*txn);
        filter.byId("test-dataset-id")
              .byStatus(DatasetStatus::Incomplete)
              .bySubset(Subset::Masstransit);

        const std::string expected =
            "WHERE mds_dataset.export_meta.id = 'test-dataset-id'\n"
            "AND mds_dataset.export_meta.status = 1\n"
            "AND mds_dataset.export_meta.subset = 2";

        BOOST_CHECK_EQUAL(filter.toSql(), expected);
    }
    {
        ExportFilter filter(*txn);
        filter.byId("test-dataset-id")
              .byStatus(DatasetStatus::Incomplete)
              .bySubset(Subset::Masstransit)
              .byRegion("cis1");

        const std::string expected =
            "WHERE mds_dataset.export_meta.id = 'test-dataset-id'\n"
            "AND mds_dataset.export_meta.status = 1\n"
            "AND mds_dataset.export_meta.subset = 2\n"
            "AND mds_dataset.export_meta.region = 'cis1'";;

        BOOST_CHECK_EQUAL(filter.toSql(), expected);
    }

    BOOST_CHECK_EQUAL(
        ExportFilter{*txn}.tested(true).toSql(),
        "WHERE mds_dataset.export_meta.tested = true");

    BOOST_CHECK_EQUAL(
        ExportFilter{*txn}.tested(false).toSql(),
        "WHERE mds_dataset.export_meta.tested = false");
}

} // tests
} // mds_dataset
} // wiki
} // maps
