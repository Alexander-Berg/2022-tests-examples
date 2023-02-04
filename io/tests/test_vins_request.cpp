#include <yandex_io/capabilities/alice/interfaces/vins_request.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace YandexIO;
using namespace testing;

Y_UNIT_TEST_SUITE(VinsRequestTest) {
    Y_UNIT_TEST(testSetIsPrefetch) {
        auto request = std::make_shared<VinsRequest>(Json::Value(), VinsRequest::createSoftwareDirectiveEventSource());
        ASSERT_FALSE(request->isPrefetch());
        request->setIsPrefetch(true);
        ASSERT_TRUE(request->isPrefetch());
    }
}
