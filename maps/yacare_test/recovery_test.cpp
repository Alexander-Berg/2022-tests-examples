#include <yandex/maps/proto/recovery/actions.pb.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <library/cpp/testing/unittest/registar.h>

namespace mh = maps::http;
namespace pr = yandex::maps::proto::recovery;

Y_UNIT_TEST_SUITE(RuntimeRecovery)
{

Y_UNIT_TEST(NoContent)
{
    mh::MockRequest request(
            mh::GET, mh::URL("http://127.0.0.1/recovery/v2/actions").addParam("lang", "ru_RU"));
    request.headers["User-Agent"] = "ru.yandex.unknownapp/9.1.5676000 datasync/105.1.1 runtime/202.2.1 android/7.1.2 (Xiaomi; Redmi 5 Plus; ru_RU)";
    auto response = yacare::performTestRequest(request);
    UNIT_ASSERT(204 == response.status);
}

Y_UNIT_TEST(TestAppPaths)
{
    mh::MockRequest request(
            mh::GET, mh::URL("http://localhost/recovery/v2/actions").addParam("lang", "ru_RU"));
    request.headers["User-Agent"] = "com.yandex.maps.testapp/208.19.0.2555 mapkit/208.19.0 runtime/201.3.0 android/10 (Google; Pixel 2; ru_RU)";
    auto response = yacare::performTestRequest(request);
    UNIT_ASSERT(200 == response.status);

    pr::Actions actions;
    Y_PROTOBUF_SUPPRESS_NODISCARD actions.ParseFromString(TString(response.body));

    UNIT_ASSERT(1 == actions.to_be_removed_size());

    const auto& path = actions.to_be_removed(0);
    UNIT_ASSERT_EQUAL(path.path(), "recovery_test/tiles.sqlite");
    UNIT_ASSERT_EQUAL(path.location(), pr::Actions::Path::EXTERNAL);
}

Y_UNIT_TEST(DataSyncPaths)
{
    mh::MockRequest request(
            mh::GET, mh::URL("http://localhost/recovery/v2/actions").addParam("lang", "ru_RU"));
    request.headers["User-Agent"] = "com.yandex.maps.datasync/208.19.0.2555 mapkit/208.19.0 runtime/201.3.0 android/10 (Google; Pixel 2; ru_RU)";
    auto response = yacare::performTestRequest(request);
    UNIT_ASSERT(200 == response.status);

    pr::Actions actions;
    Y_PROTOBUF_SUPPRESS_NODISCARD actions.ParseFromString(TString(response.body));

    UNIT_ASSERT(1 == actions.to_be_removed_size());

    const auto& path = actions.to_be_removed(0);
    UNIT_ASSERT_EQUAL(path.path(), "recovery_test/config.sqlite");
    UNIT_ASSERT_EQUAL(path.location(), pr::Actions::Path::CACHE);
}

Y_UNIT_TEST(MrcTestAppPaths)
{
    mh::MockRequest request(
            mh::GET, mh::URL("http://localhost/recovery/v2/actions").addParam("lang", "ru_RU"));
    request.headers["User-Agent"] = "com.yandex.maps.mrctestapp/1.0.1 mapkit/208.19.0 runtime/201.3.0 android/10 (Google; Pixel 2; ru_RU)";

    auto response = yacare::performTestRequest(request);
    UNIT_ASSERT(200 == response.status);

    pr::Actions actions;
    Y_PROTOBUF_SUPPRESS_NODISCARD actions.ParseFromString(TString(response.body));

    UNIT_ASSERT(1 == actions.to_be_removed_size());

    const auto& path = actions.to_be_removed(0);
    UNIT_ASSERT_EQUAL(path.path(), "recovery_test/test.sqlite");
    UNIT_ASSERT_EQUAL(path.location(), pr::Actions::Path::EXTERNAL);
}

} // Y_UNIT_TEST_SUITE_F
