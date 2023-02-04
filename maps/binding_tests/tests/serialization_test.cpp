#include <yandex/maps/runtime/bindings/internal/serialization.h>
#include <yandex/maps/runtime/bindings/platform.h>
#include <yandex/maps/runtime/internal/test_support/test_types.h>

#include <boost/serialization/map.hpp>
#include <boost/serialization/string.hpp>
#include <boost/serialization/vector.hpp>
#include <boost/test/unit_test.hpp>

#include <vector>

using namespace yandex::maps::runtime;

template <class T>
void testSerialization(T object)
{
    std::vector<uint8_t> data = bindings::internal::serialize(object);
    std::vector<uint8_t> dataCopy(data);

    object = bindings::internal::deserialize<T>(std::move(data));

    data = bindings::internal::serialize(object);

    BOOST_CHECK(data == dataCopy);

};

internal::test_support::TestStructure testStructure() {
    return internal::test_support::TestStructure{
        true,
        "text",
        boost::none,
        {0, 1, 2, 3},
        TimeInterval(123),
        AbsoluteTimestamp(TimeInterval(456))
    };
}

BOOST_AUTO_TEST_CASE(basic_serialization_test)
{
    testSerialization(testStructure());
}

BOOST_AUTO_TEST_CASE(basic_shared_serialization_test)
{
    testSerialization(
        std::make_shared<internal::test_support::TestStructure>(testStructure()));
}

BOOST_AUTO_TEST_CASE(basic_unique_serialization_test)
{
    testSerialization(
        std::make_unique<internal::test_support::TestStructure>(testStructure()));
}

BOOST_AUTO_TEST_CASE(std_vector_serialization_test)
{
    std::vector<internal::test_support::TestStructure> array(4, testStructure());
    testSerialization(array);
}

BOOST_AUTO_TEST_CASE(vector_serialization_test)
{
    bindings::Vector<internal::test_support::TestStructure> vec;
    vec.push_back(testStructure());
    testSerialization(vec);
}

BOOST_AUTO_TEST_CASE(shared_vector_serialization_test)
{
    bindings::SharedVector<internal::test_support::TestStructure> vec;
    vec.push_back(testStructure());
    testSerialization(vec);
}

BOOST_AUTO_TEST_CASE(cbdict_serialization_test) 
{
    bindings::StringDictionary<internal::test_support::TestStructure> dictionary;
    dictionary["object1"] = testStructure();
    testSerialization(dictionary);
}
