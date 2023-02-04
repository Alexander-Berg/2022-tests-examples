#include "tests/to_native_to_platform_common.h"

#include <yandex/maps/runtime/bindings/platform.h>
#include <yandex/maps/runtime/bindings/traits.h>

#include <boost/test/unit_test.hpp>

#include <Eigen/Geometry>

#include <cstdint>
#include <initializer_list>
#include <memory>

using namespace yandex::maps::runtime;
using namespace yandex::maps::runtime::internal::test_support;

namespace {

// Following template method must be defined in including .cpp file:
//
// template <typename Value>
// Value convert(const Value& value);

template <typename Value>
void testItemConversion(const Value& v)
{
    BOOST_CHECK_EQUAL(v, convert(v));

    BOOST_CHECK_EQUAL(boost::optional<Value>(v),
        convert(boost::optional<Value>(v)));

    BOOST_CHECK_EQUAL(boost::make_optional(false, v),
        convert(boost::make_optional(false, v)));
}
template <typename Value>
void testItemConversion(const std::shared_ptr<Value>& v)
{
    BOOST_CHECK_EQUAL(v, convert(v));

    std::shared_ptr<Value> emptyPtr;
    BOOST_CHECK_EQUAL(emptyPtr, convert(emptyPtr));
}

template <typename Value>
void testVectorConversion(std::initializer_list<Value> values)
{
    testItemConversion(
        std::make_shared<bindings::Vector<Value>>(std::vector<Value>(values)));

    std::vector<boost::optional<Value>> optionals(values.size());
    for (const auto& v : values) {
        optionals.push_back(v);
    }
    testItemConversion(
        std::make_shared<bindings::Vector<boost::optional<Value>>>(optionals));

    // Mix with boost::none-s
    auto length = optionals.size();
    for (decltype(length) i = 0; i < length; ++i) {
        optionals.insert(optionals.begin() + i * 2, boost::none);
    }
    testItemConversion(
        std::make_shared<bindings::Vector<boost::optional<Value>>>(optionals));

    // Test boost::none-s only
    testItemConversion(
        std::make_shared<bindings::Vector<boost::optional<Value>>>(
            std::vector<boost::optional<Value>>(7, boost::none)));
}
template <typename Value>
void testVectorConversion(std::initializer_list<std::shared_ptr<Value>> bridgedValues)
{
    auto sharedVector = std::make_shared<bindings::SharedVector<Value>>();
    for (const auto& value : bridgedValues) {
        sharedVector->push_back_shared(std::move(value));
    }
    testItemConversion(sharedVector);

    // Mix with empty shared_ptr-s
    auto length = sharedVector.size();
    for (decltype(length) i = 0; i < length; ++i) {
        sharedVector.insert(sharedVector.begin() + i * 2, nullptr);
    }
    testItemConversion(sharedVector);

    // Test empty shared_ptr-s only
    for (auto& value : sharedVector) {
        value = nullptr;
    }
    testItemConversion(sharedVector);
}

template <typename Value>
void testMapConversion(std::initializer_list<std::pair<const std::string, Value>> entries)
{
    testItemConversion(
        std::make_shared<bindings::StringDictionary<Value>>(
            std::map<std::string, Value>(entries)));

    std::map<std::string, boost::optional<Value>> optionals;
    for (const auto& e : entries) {
        optionals[e.first] = e.second;
    }
    testItemConversion(
        std::make_shared<bindings::StringDictionary<boost::optional<Value>>>(optionals));

    // Add some boost::none-s
    optionals["optional_key_1"] = boost::none;
    optionals["optional_key_2"] = boost::none;
    optionals["optional_key_3"] = boost::none;
    testItemConversion(
        std::make_shared<bindings::StringDictionary<boost::optional<Value>>>(optionals));

    // Test boost::none-s only
    optionals = { { "key_1", boost::none }, { "key_2", boost::none }, { "key_3", boost::none } };
    testItemConversion(
        std::make_shared<bindings::StringDictionary<boost::optional<Value>>>(optionals));
}

template <typename Value>
void testConversion(const Value& v)
{
    testItemConversion(v);
    testVectorConversion<Value>({ v, v, v, v, v, v, v });
    testMapConversion<Value>({ { "key_1", v }, { "key_2", v }, { "key_3", v } });
}

} // namespace

void runToNativeToPlatformTests()
{
    using Bytes = std::vector<std::uint8_t>;

    testConversion(true);
    testConversion(-5);
    testConversion(5U);
    testConversion(-123456789012LL);
    testConversion(123456789012ULL);
    testConversion(5.3f);
    testConversion(-7.99);

    testConversion(TestEnum::Second);
    testConversion(TestBitfieldEnum::Eight);
    testConversion(TestBitfieldEnum::Two | TestBitfieldEnum::Eight);

    testConversion(std::string("some string"));
    testConversion(Eigen::Vector2f(0.357f, 0.499f));
    testConversion(TimeInterval(100));
    testConversion(AbsoluteTimestamp(TimeInterval(456)));
    testConversion(Color::fromARGB(0x0f0f000f));

    // Collections of byte-vectors are not supported!
    testItemConversion(Bytes{'a', 'b', 'z'});

    testConversion(std::make_shared<TestStructure>(
        true,
        "text",
        boost::none,
        bindings::Vector<int>{ 1, 2, 3, 5 },
        TimeInterval(123),
        AbsoluteTimestamp(TimeInterval(456))));
    testConversion(LiteTestStructure(
        true,
        "text",
        boost::none,
        TimeInterval(123),
        AbsoluteTimestamp(TimeInterval(456))));
    testConversion(OptionsTestStructure(
        true,
        std::string("text"),
        boost::none,
        AbsoluteTimestamp(TimeInterval(456))));
    testConversion(std::make_shared<FullTestStructure>());
    testConversion(FullLiteTestStructure());
    testConversion(FullOptionsTestStructure());
}
