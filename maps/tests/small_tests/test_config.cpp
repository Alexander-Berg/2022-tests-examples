#include <maps/wikimap/ugc/libs/common/config.h>
#include <maps/libs/pgpool3utils/include/yandex/maps/pgpool3utils/dynamic_pool_holder.h>
#include <maps/libs/xml/include/xml.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>


namespace maps::wiki::ugc::tests {

namespace {

const std::string EXPECTED =
    "<database> name=\"maps_ugc_test\">"
        "<host "
            "dbname=\"maps_ugc_test\" "
            "host=\"testhost1\" "
            "port=\"6432\" "
            "user=\"maps_ugc_test\" "
            "pass=\"password\""
        "/>"
        "<host "
            "dbname=\"maps_ugc_test\" "
            "host=\"testhost2\" "
            "port=\"6432\" "
            "user=\"maps_ugc_test\" "
            "pass=\"password\""
        "/>"
        "<host "
            "dbname=\"maps_ugc_test\" "
            "host=\"testhost3\" "
            "port=\"6432\" "
            "user=\"maps_ugc_test\" "
            "pass=\"password\""
        "/>"
        "<pool "
            "timeout=\"111\" "
            "pingPeriod=\"5\" "
            "writePoolOverflow=\"4\" "
            "writePoolSize=\"2\" "
            "readPoolSize=\"222\" "
            "readPoolOverflow=\"333\" "
            "treatMasterAsSlave=\"1\""
        "/>"
    "</database>";

} // namespace

Y_UNIT_TEST_SUITE(test_config)
{

Y_UNIT_TEST(test2_config)
{
    maps::json::Value config = maps::json::Value::fromFile(SRC_("data/test2_conf"));

    const auto dbConfigs = dbConfigsFromJson(config);
    UNIT_ASSERT_VALUES_EQUAL(dbConfigs.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(dbConfigs.front(), EXPECTED);
    UNIT_ASSERT_VALUES_EQUAL(dbConfigs.back(), EXPECTED);

    UNIT_ASSERT_NO_EXCEPTION(maps::xml3::Doc::fromString(dbConfigs.front()));
    const maps::xml3::Doc dbConfigDoc = maps::xml3::Doc::fromString(dbConfigs.front());
    UNIT_ASSERT_NO_EXCEPTION(maps::pgp3utils::DynamicPoolHolder(dbConfigDoc.node("/database")));
}

} // test_config suite

} // namespace maps::wiki::ugc::tests

