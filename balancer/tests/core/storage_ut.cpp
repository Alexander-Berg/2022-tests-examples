#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/contrib/cone/cone.hh>
#include <balancer/serval/core/storage.h>

Y_UNIT_TEST_SUITE(Storage) {
    Y_UNIT_TEST(Static) {
        auto ctor = []{ return 1; };
        {
            auto a = NSv::StaticData(1, ctor);
            auto b = NSv::StaticData(1, ctor);
            auto c = NSv::StaticData(2, ctor);
            UNIT_ASSERT_VALUES_EQUAL(++*a, 2);
            UNIT_ASSERT_VALUES_EQUAL(*b, 2);
            UNIT_ASSERT_VALUES_EQUAL(*c, 1);
        }
        auto d = NSv::StaticData(1, ctor);
        UNIT_ASSERT_VALUES_EQUAL(*d, 1);
    }

    Y_UNIT_TEST(ThreadLocal) {
        NSv::TThreadLocal<int> tls;
        cone::thread ts[16];
        for (auto& t : ts) {
            t = [&]() {
                NSv::TThreadLocalRoot root;
                UNIT_ASSERT_VALUES_EQUAL(tls.GetOrCreate(), 0);
                *tls = 1;
                UNIT_ASSERT_VALUES_EQUAL(*tls, 1);
                tls.Reset();
                UNIT_ASSERT_VALUES_EQUAL(tls.GetOrCreate(), 0);
                return true;
            };
        }
        for (auto& t : ts)
            UNIT_ASSERT(t->wait(cone::rethrow));
    }
}
