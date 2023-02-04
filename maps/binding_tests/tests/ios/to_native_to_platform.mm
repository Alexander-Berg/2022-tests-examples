#import <YandexMapsMobile/Internal/YRTTestSupportTestTypes_Private.h>
#import <YandexMapsMobile/YRTTestSupportTestTypes.h>

#import <yandex/maps/runtime/bindings/ios/dictionary_to_native.h>
#import <yandex/maps/runtime/bindings/ios/dictionary_to_platform.h>
#import <yandex/maps/runtime/bindings/ios/point_to_native.h>
#import <yandex/maps/runtime/bindings/ios/point_to_platform.h>
#import <yandex/maps/runtime/bindings/ios/to_native.h>
#import <yandex/maps/runtime/bindings/ios/to_platform.h>
#import <yandex/maps/runtime/bindings/ios/vector_to_native.h>
#import <yandex/maps/runtime/bindings/ios/vector_to_platform.h>

#include <boost/test/unit_test.hpp>

using namespace yandex::maps::runtime;

namespace {

template <typename Value>
Value convert(const Value& value)
{
    return bindings::ios::toNative<Value>(bindings::ios::toPlatform(value));
}

} // namespace

#include "tests/to_native_to_platform_common_inl.h"

BOOST_AUTO_TEST_CASE(ios_to_native_and_to_platform_tests)
{
    runToNativeToPlatformTests();
}

namespace {

template <typename NativeType, typename PlatformType = id>
void checkNullConvertThrows()
{
    BOOST_CHECK_THROW(
        bindings::ios::toNative<NativeType>(PlatformType()),
        RuntimeError);
}

template <typename NativeNumericType>
void checkNumericNullConvertThrows()
{
    checkNullConvertThrows<NativeNumericType>();
    checkNullConvertThrows<NativeNumericType, NSNumber *>();
}

} // namespace

BOOST_AUTO_TEST_CASE(ios_to_native_and_to_platform_null_check_tests)
{
    checkNumericNullConvertThrows<bool>();
    checkNumericNullConvertThrows<char>();
    checkNumericNullConvertThrows<short>();
    checkNumericNullConvertThrows<unsigned short>();
    checkNumericNullConvertThrows<int>();
    checkNumericNullConvertThrows<unsigned int>();
    checkNumericNullConvertThrows<long>();
    checkNumericNullConvertThrows<unsigned long>();
    checkNumericNullConvertThrows<long long>();
    checkNumericNullConvertThrows<unsigned long long>();
    checkNumericNullConvertThrows<float>();

    checkNullConvertThrows<TimeInterval, NSNumber *>();
    checkNullConvertThrows<AbsoluteTimestamp, NSDate *>();
    checkNullConvertThrows<RelativeTimestamp, NSDate *>();
    checkNullConvertThrows<Color, UIColor *>();

    checkNullConvertThrows<std::string, NSString *>();

    checkNullConvertThrows<Eigen::Vector2f, NSValue *>();
    checkNullConvertThrows<std::vector<std::uint8_t>, NSData *>();

    checkNullConvertThrows<internal::test_support::TestBitfieldEnum, NSNumber *>();
    checkNullConvertThrows<internal::test_support::TestEnum, NSNumber *>();
}
