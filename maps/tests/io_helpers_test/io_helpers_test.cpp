#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/common/include/io_helpers.h>
#include <maps/analyzer/libs/common/tests/proto/mock.pb.h>

#include <boost/test/unit_test.hpp>

namespace ma = maps::analyzer;
namespace tproto = maps::analyzer::common::tests::proto;

BOOST_AUTO_TEST_CASE( proto_serialization )
{
    std::string fieldStr = "fieldA";
    int fieldInt = 1;
    tproto::MockData mockData;
    {
        tproto::MockInnerData mockInnerData;
        mockInnerData.set_field_str(fieldStr.c_str());
        mockInnerData.set_field_int(fieldInt);
        *mockData.mutable_inner_data() = mockInnerData;
    }
    const auto coded = ma::serializeProtobuf(mockData);
    const auto decoded = ma::parseProtobuf<tproto::MockData>(coded);
    BOOST_CHECK_EQUAL(static_cast<std::string>(decoded.inner_data().field_str()), fieldStr);
    BOOST_CHECK_EQUAL(decoded.inner_data().field_int(), fieldInt);
}

BOOST_AUTO_TEST_CASE( time_serialization )
{
    std::string isoTime = "20170101T235959";
    BOOST_CHECK_EQUAL(isoTime, ma::serializeISOTime(ma::parseISOTime(isoTime)));
}

BOOST_AUTO_TEST_CASE( time_period_serialization )
{
    std::string isoTime = "20170101T000000 20170101T235959";
    BOOST_CHECK_EQUAL(isoTime, ma::serializeTimePeriod(ma::parseTimePeriod(isoTime)));
}
