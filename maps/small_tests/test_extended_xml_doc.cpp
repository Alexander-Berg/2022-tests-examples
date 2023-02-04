#include <yandex/maps/wiki/common/extended_xml_doc.h>
#include <yandex/maps/wiki/common/pgpool3_helpers.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

namespace {

const TString SERVICES_CONFIG_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/mapspro/cfg/services/services.development.xml";

} // namespace

Y_UNIT_TEST_SUITE(extended_xml_doc) {

Y_UNIT_TEST(test_load_config)
{
    ExtendedXmlDoc doc(SERVICES_CONFIG_PATH);
    UNIT_ASSERT_STRINGS_EQUAL(doc.node("/config/extends").value<std::string>(), "services-base.xml");
}

Y_UNIT_TEST(test_load_unknown_value_quiet)
{
    ExtendedXmlDoc doc(SERVICES_CONFIG_PATH);
    UNIT_ASSERT(doc.node("/config/xxx", true).isNull());
}

Y_UNIT_TEST(test_load_unknown_value_nonquiet)
{
    ExtendedXmlDoc doc(SERVICES_CONFIG_PATH);
    UNIT_CHECK_GENERATED_EXCEPTION(doc.node("/config/xxx", false), maps::Exception);
}

Y_UNIT_TEST(test_invalid_pool_holder)
{
    ExtendedXmlDoc doc(SERVICES_CONFIG_PATH);
    UNIT_CHECK_GENERATED_EXCEPTION(PoolHolder(doc, "xxx", "xxx"), maps::Exception);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
