#include "single_item_provider.h"

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE(SingleItemProviderTest) {

    Y_UNIT_TEST(testSingleItemProvider) {
        auto provider = SingleItemProvider("some_cache_path", "url");
        auto json = provider.toJson();
        UNIT_ASSERT(json.isString());
        UNIT_ASSERT_EQUAL(json.asString(), "url");
        UNIT_ASSERT_EQUAL(provider.getAllDemoItems().front().sound.url, "url");
        provider.updateDemoItems(parseJson("\"new_url\""));
        json = provider.toJson();
        UNIT_ASSERT_EQUAL(json.asString(), "new_url");
        UNIT_ASSERT_EQUAL(provider.getAllDemoItems().front().sound.url, "new_url");
    }

};
