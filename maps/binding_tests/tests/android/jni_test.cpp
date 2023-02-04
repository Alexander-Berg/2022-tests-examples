#include <yandex/maps/runtime/android/jni.h>
#include <yandex/maps/runtime/android/version.h>
#include <yandex/maps/runtime/bindings/traits.h>
#include <yandex/maps/runtime/bindings/android/internal/direct_conversion.h>

#include <boost/test/unit_test.hpp>

using namespace yandex::maps::runtime::android;

struct TestClass {};
struct ReturnClass {};

struct ReturnClassFieldTrait {
    static constexpr const char* const name = "objectField";
    static constexpr const char* const typeName =
        "Lcom/yandex/runtime/test_support/TestClass$ReturnClass;";
    using type = ReturnClass;
};

namespace yandex::maps::runtime::bindings {

template <>
struct BindingTraits<TestClass> : public BaseBindingTraits {
    static constexpr const char* const javaUndecoratedName =
        "com/yandex/runtime/test_support/TestClass";
};

template <>
struct BindingTraits<ReturnClass> : public BaseBindingTraits {
    static constexpr const char* const javaUndecoratedName =
        "Lcom/yandex/runtime/test_support/TestClass$ReturnClass;";
};

namespace android::internal {

template <>
struct ToNative<ReturnClass, jobject, void>
{
    static ReturnClass from(jobject) { return ReturnClass{}; }
};

} // namespace android::internal

} // namespace yandex::maps::runtime::bindings
BOOST_AUTO_TEST_CASE(reference_table_overflow)
{
    for (int i = 0; i < 2048; ++i) {
        auto testClass = findClass("com/yandex/runtime/test_support/TestClass");
        auto testObject = createObject(testClass.get(), "()V");
        BOOST_CHECK_THROW(
            callMethod<int>(testObject.get(), "throwException", "()I"),
            JavaException
        );
        // Ensure that such objects don't leak
        auto dummyObject [[maybe_unused]] = callMethod<JniObject>(testObject.get(), "getObjectValue", "()Ljava/lang/Integer;");
        BOOST_CHECK_EQUAL(callMethod<int>(testObject.get(), "getIntValue", "()I"), 42);
    }
}

BOOST_AUTO_TEST_CASE(extractField_onMultipleObjectFieldQueries_wontOverflowLocalReferenceTable)
{
    using ::yandex::maps::runtime::bindings::android::internal::extractField;
    auto testClass = findClass("com/yandex/runtime/test_support/TestClass");
    auto testObject = createObject(testClass.get(), "()V");
    for (int i = 0; i < 1024; ++i) {
        auto dummyField [[maybe_unused]] = extractField<TestClass, ReturnClassFieldTrait>(
            testObject.get()
        );
    }
    BOOST_CHECK(true);
}

BOOST_AUTO_TEST_CASE(ObtainVm)
{
    auto vmVersion = vm();
    if (vmVersion == Vm::Dalvik) {
        BOOST_TEST_MESSAGE("Dalvik");
    } else {
        BOOST_CHECK_EQUAL(static_cast<int>(vmVersion), static_cast<int>(Vm::Art));
        BOOST_TEST_MESSAGE("Art");
    }
}

BOOST_AUTO_TEST_CASE(ObtainVersion)
{
    auto ver = version();
    BOOST_TEST_MESSAGE(static_cast<int>(ver));
}
