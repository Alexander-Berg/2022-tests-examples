#include <maps/libs/concurrent/include/atomic_shared_ptr.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <memory>

namespace maps::concurrent::tests {

Y_UNIT_TEST_SUITE(test_atomic_shared_ptr) {

Y_UNIT_TEST(initial_is_nullptr) {
    AtomicSharedPtr<size_t> instance;
    ASSERT_EQ(instance.load(), nullptr);
}

Y_UNIT_TEST(object_is_replaced_synchronously) {
    AtomicSharedPtr<size_t> instance(std::shared_ptr<size_t>(new size_t(0u)));
    std::shared_ptr<size_t> loadedPtr = instance.load();

    instance.store(std::shared_ptr<size_t>(new size_t(10u)));
    std::shared_ptr<size_t> loadedAfterStoringPtr = instance.load();

    ASSERT_EQ(*loadedPtr, 0u);
    ASSERT_EQ(*loadedAfterStoringPtr, 10u);
    ASSERT_EQ(*instance.load(), 10u);
}

}

} // maps::concurrent::tests

