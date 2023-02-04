#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/pgpool3utils/include/yandex/maps/pgpool3utils/dynamic_pool_holder.h>
#include <maps/libs/xml/include/xml.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>


namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_config)
{

Y_UNIT_TEST(test_config)
{
    maps::json::Value config = maps::json::Value::fromFile(SRC_("data/test_conf"));
    std::string expected =
        "<database> name=\"maps_feedback_test\">"
            "<host "
                "dbname=\"maps_feedback_test\" "
                "host=\"testhost1\" "
                "port=\"6432\" "
                "user=\"maps_feedback_test\" "
                "pass=\"password\""
            "/>"
            "<host "
                "dbname=\"maps_feedback_test\" "
                "host=\"testhost2\" "
                "port=\"6432\" "
                "user=\"maps_feedback_test\" "
                "pass=\"password\""
            "/>"
            "<host "
                "dbname=\"maps_feedback_test\" "
                "host=\"testhost3\" "
                "port=\"6432\" "
                "user=\"maps_feedback_test\" "
                "pass=\"password\""
            "/>"
            "<pool "
                "timeout=\"10\" "
                "pingPeriod=\"30\" "
                "writePoolOverflow=\"4\" "
                "writePoolSize=\"2\" "
                "readPoolSize=\"6\" "
                "readPoolOverflow=\"9\" "
                "treatMasterAsSlave=\"1\""
            "/>"
        "</database>";
    const std::string dbConfig = dbConfigFromJson(config);
    UNIT_ASSERT_VALUES_EQUAL(dbConfig, expected);
    UNIT_ASSERT_NO_EXCEPTION(maps::xml3::Doc::fromString(dbConfig));
    const maps::xml3::Doc dbConfigDoc = maps::xml3::Doc::fromString(dbConfig);
    UNIT_ASSERT_NO_EXCEPTION(maps::pgp3utils::DynamicPoolHolder(dbConfigDoc.node("/database")));
}

} // test_config suite

} // namespace maps::wiki::feedback::api::tests

