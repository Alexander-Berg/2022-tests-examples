#include <yandex_io/libs/signals/live_data.h>

#include <yandex_io/libs/base/named_callback_queue.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(LiveData) {
    Y_UNIT_TEST(testLiveDataSetGet)
    {
        LiveData<int> ld;
        UNIT_ASSERT_EQUAL(ld.value(), 0);

        ld.setValue(5);
        UNIT_ASSERT_EQUAL(ld.value(), 5);
    }

    Y_UNIT_TEST(testLiveDataOpLet)
    {
        LiveData<std::string> ld;
        UNIT_ASSERT_EQUAL(ld.value(), "");

        ld = "mama myla ramu";
        UNIT_ASSERT_EQUAL(ld.value(), "mama myla ramu");

        std::string text = ld;
        UNIT_ASSERT_EQUAL(text, "mama myla ramu");
    }

    Y_UNIT_TEST(testLiveDataDefaultValue)
    {
        LiveData<int> ld(3);
        UNIT_ASSERT_EQUAL(ld.value(), 3);
    }

    Y_UNIT_TEST(testLiveDataInterfaceDeclaration)
    {
        LiveData<ILiveData<int>> ld1(77);
        ILiveData<int>& ld1Ref = ld1; // cast cast to interface
        UNIT_ASSERT_EQUAL(ld1Ref.value(), 77);

        LiveData<ILiveData<int>> ld2(99);
        ILiveData<int>& ld2Ref = ld2; // cast cast to interface
        UNIT_ASSERT_EQUAL(ld2Ref.value(), 99);
    }

    Y_UNIT_TEST(testLiveDataChanged)
    {
        LiveData<int> ld(8);
        UNIT_ASSERT_EQUAL(ld.value(), 8);

        std::vector<int> ldHistory;
        ld.connect([&](int v) { ldHistory.push_back(v); }, Lifetime::immortal);

        ld.setValue(16);
        UNIT_ASSERT_EQUAL(ld.value(), 16);

        ld.setValue(32);
        UNIT_ASSERT_EQUAL(ld.value(), 32);

        UNIT_ASSERT_EQUAL(ldHistory.size(), 3);
        UNIT_ASSERT_EQUAL(ldHistory[0], 8);
        UNIT_ASSERT_EQUAL(ldHistory[1], 16);
        UNIT_ASSERT_EQUAL(ldHistory[2], 32);
    }

    Y_UNIT_TEST(testLiveDataChangedAsync)
    {
        LiveData<int> ld(8);
        UNIT_ASSERT_EQUAL(ld.value(), 8);

        std::atomic<bool> pauseCallbackQueue{true};
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("test");
        callbackQueue->add([&] { waitUntil([&]() { return !pauseCallbackQueue.load(); }); });

        std::vector<int> ldHistory;
        ld.connect([&](int v) { ldHistory.push_back(v); }, Lifetime::immortal, callbackQueue);

        ld.setValue(16);
        UNIT_ASSERT_EQUAL(ld.value(), 16);

        ld.setValue(32);
        UNIT_ASSERT_EQUAL(ld.value(), 32);

        pauseCallbackQueue = false;
        std::atomic<bool> finish{false};
        callbackQueue->add([&] { finish = true; });
        waitUntil([&]() { return finish.load(); });

        UNIT_ASSERT_EQUAL(ldHistory.size(), 3);
        UNIT_ASSERT_EQUAL(ldHistory[0], 8);
        UNIT_ASSERT_EQUAL(ldHistory[1], 16);
        UNIT_ASSERT_EQUAL(ldHistory[2], 32);
    }

    Y_UNIT_TEST(testLiveDataLockUnlock)
    {
        LiveData<int> ld(8);
        UNIT_ASSERT_EQUAL(ld.value(), 8);

        std::vector<int> ldHistory;
        ld.connect([&](int v) { ldHistory.push_back(v); }, Lifetime::immortal);
        UNIT_ASSERT_EQUAL(ldHistory.size(), 1);
        UNIT_ASSERT_EQUAL(ldHistory[0], 8);

        {
            std::lock_guard lock(ld);
            ld.setValue(16);
            UNIT_ASSERT_EQUAL(ld.value(), 16);
            UNIT_ASSERT_EQUAL(ldHistory.size(), 1);

            ld.setValue(32);
            UNIT_ASSERT_EQUAL(ld.value(), 32);
            UNIT_ASSERT_EQUAL(ldHistory.size(), 1);
        }

        UNIT_ASSERT_VALUES_EQUAL(ldHistory.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(ldHistory[0], 8);  // First before lock after initial connect
        UNIT_ASSERT_VALUES_EQUAL(ldHistory[1], 32); // Last value after unlock
    }
}
