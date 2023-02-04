#include <yandex/maps/runtime/bindings/internal/serialization.h>
#include <yandex/maps/runtime/internal/test_support/test_types.h>

#include <boost/test/unit_test.hpp>

#include <vector>

using namespace yandex::maps::runtime;

BOOST_AUTO_TEST_CASE(generation_test)
{
    auto object = bindings::internal::generate<internal::test_support::TestStructure>();

    std::vector<uint8_t> data = bindings::internal::serialize(object);
    std::vector<uint8_t> dataCopy(data);

    object = bindings::internal::deserialize<internal::test_support::TestStructure>(std::move(data));

    data = bindings::internal::serialize(object);

    BOOST_CHECK(data == dataCopy);
}
