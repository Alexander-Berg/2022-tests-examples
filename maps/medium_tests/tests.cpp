#include <yandex/maps/wiki/common/extended_xml_doc.h>
#include <yandex/maps/wiki/common/pgpool3_helpers.h>

#include <maps/libs/common/include/exception.h>

#include <maps/libs/local_postgres/include/instance.h>

#include <library/cpp/testing/unittest/registar.h>

#include <boost/format.hpp>

#include <fstream>

namespace maps::wiki::common::tests {

namespace {

const boost::format TEMPLATE(R"(<?xml version='1.0' encoding='utf-8'?>
<config>
    <common>
        <databases>
            <database id="core" name="%2%">
                <write host="localhost" port="%1%" user="%3%" pass="%4%"/>
                <read host="localhost" port="%1%" user="%3%" pass="%4%"/>
                <pools nearestDC="1" failPingPeriod="5" pingPeriod="30" timeout="5">
                    <core writePoolSize="4" writePoolOverflow="6" readPoolSize="6" readPoolOverflow="9"/>
                </pools>
            </database>
        </databases>
    </common>
</config>)");

const std::string TEST_CONFIG_FILENAME = "config.xml";

} // namespace

Y_UNIT_TEST_SUITE(other) {

Y_UNIT_TEST(test_ping_database)
{
    local_postgres::Database instance;

    auto config = boost::format(TEMPLATE)
        % instance.port()
        % instance.dbname()
        % instance.user()
        % instance.password();

    std::ofstream file(TEST_CONFIG_FILENAME);
    file << config.str();
    file.close();

    ExtendedXmlDoc doc(TEST_CONFIG_FILENAME);

    PoolHolder holder(doc, "core", "core");

    auto work = holder.pool().slaveTransaction();
    auto r = work->exec("select 1 as xxx");
    UNIT_ASSERT_VALUES_EQUAL(r.affected_rows(), 1);
    const auto& field = r[0][0];
    UNIT_ASSERT_VALUES_EQUAL(field.as<int>(), 1);
    UNIT_ASSERT_STRINGS_EQUAL(field.name(), std::string("xxx"));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
