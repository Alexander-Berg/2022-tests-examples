#include <yandex_io/libs/glagol_sdk/avahi_wrapper/avahi_browse_client.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace glagol;

Y_UNIT_TEST_SUITE(makeInfo) {
    Y_UNIT_TEST(testMakeInfo)
    {
        AvahiStringList* list = avahi_string_list_new("platform=yandex", "value=data", "green=red=dark", nullptr);
        auto info = makeInfo("hostname", 1111, "1.1.1.1", list);
        avahi_string_list_free(list);

        UNIT_ASSERT_EQUAL(info.hostname, "hostname");
        UNIT_ASSERT_EQUAL(info.port, 1111);
        UNIT_ASSERT_EQUAL(info.address, "1.1.1.1");
        UNIT_ASSERT_EQUAL(info.txt["platform"], "yandex");
        UNIT_ASSERT_EQUAL(info.txt["value"], "data");
        UNIT_ASSERT_EQUAL(info.txt["green"], "red=dark");
    }
}
