#include <yandex/maps/runtime/bindings/android/dictionary_to_native.h>
#include <yandex/maps/runtime/bindings/android/dictionary_to_platform.h>
#include <yandex/maps/runtime/bindings/android/to_native.h>
#include <yandex/maps/runtime/bindings/android/to_platform.h>
#include <yandex/maps/runtime/bindings/android/vector_to_native.h>
#include <yandex/maps/runtime/bindings/android/vector_to_platform.h>
#include <yandex/maps/runtime/internal/test_support/internal/android/test_types_binding.h>

#include <boost/test/unit_test.hpp>

using namespace yandex::maps::runtime;

namespace {

template <typename Value>
Value convert(const Value& value)
{
    return bindings::android::toNative<Value>(
        bindings::android::toPlatform(value));
}

} // namespace

#include "tests/to_native_to_platform_common_inl.h"

BOOST_AUTO_TEST_CASE(android_to_native_and_to_platform_tests)
{
    runToNativeToPlatformTests();
}

namespace {

template <typename NativeType, typename PlatformType = android::JniObject>
void checkNullConvertThrows()
{
    BOOST_CHECK_THROW(
        bindings::android::toNative<NativeType>(PlatformType()),
        RuntimeError);
}

} // namespace

BOOST_AUTO_TEST_CASE(android_to_native_and_to_platform_null_check_tests)
{
    checkNullConvertThrows<bool>();
    checkNullConvertThrows<int>();
    checkNullConvertThrows<unsigned int>();
    checkNullConvertThrows<std::int64_t>();
    checkNullConvertThrows<std::uint64_t>();
    checkNullConvertThrows<float>();
    checkNullConvertThrows<double>();
    checkNullConvertThrows<TimeInterval>();
    checkNullConvertThrows<AbsoluteTimestamp>();
    checkNullConvertThrows<RelativeTimestamp>();
    checkNullConvertThrows<Color>();
    checkNullConvertThrows<internal::test_support::TestBitfieldEnum>();

    checkNullConvertThrows<std::string, jstring>();

    checkNullConvertThrows<Eigen::Vector2f>();
    checkNullConvertThrows<std::vector<std::uint8_t>>();

    checkNullConvertThrows<internal::test_support::TestEnum>();
}
