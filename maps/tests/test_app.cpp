#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

namespace maps::automotive::store_internal {

struct AppApi: public AppContextPostgresFixture
{
    AppApi()
        : managerProd(makeUserInfo("manager-prod"))
    {}

    yacare::tests::UserInfoFixture managerProd;
};

static bool operator==(const App& appA, const App& appB)
{
    if (appA.app_name() != appB.app_name()) {
        return false;
    }

    std::unordered_set<TString> setA;
    std::unordered_set<TString> setB;
    std::copy(
        appA.tags().begin(),
        appA.tags().end(),
        std::inserter(setA, setA.begin()));
    std::copy(
        appB.tags().begin(),
        appB.tags().end(),
        std::inserter(setB, setB.begin()));
    return setA == setB;
}

namespace {

const std::string API_PREFIX = "/store/1.x/app";
const TString APP_NAME_A = "app.name_a1";
const TString APP_NAME_B = "app.name_b2";
const TString INVALID_APP_NAME = "app.name$";
const TString GROUPABLE_TAG = "groupable";
const TString CORE_TAG = "core";

bool isASubsetOf(const Apps& subset, const Apps& superset)
{
    for (const auto& app: subset.apps()) {
        if (std::find(superset.apps().begin(), superset.apps().end(), app) ==
            superset.apps().end()) {
            return false;
        }
    }

    return true;
}

} // namespace

TEST_F(AppApi, GetDefault)
{
    auto response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    ASSERT_NE(printToString(Apps{}), TString(response.body));
}

TEST_F(AppApi, AddEmpty)
{
    auto response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    auto defaultBody = response.body;

    Apps apps;
    auto appsBody = printToString(apps);
    response = mockPut(API_PREFIX, appsBody);
    ASSERT_EQ(400, response.status);

    response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(defaultBody, response.body);

    response = mockGet(API_PREFIX + "/" + APP_NAME_A);
    ASSERT_EQ(404, response.status);

    response = mockGet(API_PREFIX + "/" + APP_NAME_B);
    ASSERT_EQ(404, response.status);
}

TEST_F(AppApi, AddOne)
{
    Apps apps;
    auto app = apps.add_apps();
    app->set_app_name(APP_NAME_A);
    *app->add_tags() = GROUPABLE_TAG;
    *app->add_tags() = CORE_TAG;
    auto appsBody = printToString(apps);
    auto response = mockPut(API_PREFIX, appsBody);
    ASSERT_EQ(204, response.status);

    response = mockGet(API_PREFIX + "/" + APP_NAME_A);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(printToString(*app), TString(response.body));

    response = mockGet(API_PREFIX + "/" + APP_NAME_B);
    ASSERT_EQ(404, response.status);

    response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);

    Apps responseApps;
    response = mockGet(API_PREFIX);
    parseFromString(response.body, /*out*/ responseApps);
    ASSERT_TRUE(isASubsetOf(apps, responseApps));
}

TEST_F(AppApi, AddTwo)
{
    Apps apps;
    auto appA = apps.add_apps();
    appA->set_app_name(APP_NAME_A);
    *appA->add_tags() = GROUPABLE_TAG;
    auto appB = apps.add_apps();
    appB->set_app_name(APP_NAME_B);
    *appB->add_tags() = CORE_TAG;
    auto appsBody = printToString(apps);
    auto response = mockPut(API_PREFIX, appsBody);
    ASSERT_EQ(204, response.status);

    response = mockGet(API_PREFIX + "/" + APP_NAME_A);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(printToString(*appA), TString(response.body));

    response = mockGet(API_PREFIX + "/" + APP_NAME_B);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(printToString(*appB), TString(response.body));

    Apps responseApps;
    response = mockGet(API_PREFIX);
    parseFromString(response.body, /*out*/ responseApps);
    ASSERT_TRUE(isASubsetOf(apps, responseApps));
}

TEST_F(AppApi, AddInvalid)
{
    auto response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    auto defaultBody = response.body;

    Apps apps;
    auto appA = apps.add_apps();
    appA->set_app_name(APP_NAME_A);
    *appA->add_tags() = GROUPABLE_TAG;
    auto appB = apps.add_apps();
    appB->set_app_name(INVALID_APP_NAME);
    *appB->add_tags() = CORE_TAG;
    auto appsBody = printToString(apps);
    response = mockPut(API_PREFIX, appsBody);
    ASSERT_EQ(400, response.status);

    response = mockGet(API_PREFIX + "/" + APP_NAME_A);
    ASSERT_EQ(404, response.status);

    response = mockGet(API_PREFIX + "/" + INVALID_APP_NAME);
    ASSERT_EQ(400, response.status);

    response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(defaultBody, response.body);
}

TEST_F(AppApi, AddTagsToExisting)
{
    Apps apps;
    auto appA = apps.add_apps();
    appA->set_app_name(APP_NAME_A);

    auto appsBody = printToString(apps);
    auto response = mockPut(API_PREFIX, appsBody);
    ASSERT_EQ(204, response.status);

    Apps responseApps;
    response = mockGet(API_PREFIX);
    parseFromString(response.body, /*out*/ responseApps);
    ASSERT_TRUE(isASubsetOf(apps, responseApps));

    response = mockGet(API_PREFIX + "/" + APP_NAME_A);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(printToString(*appA), TString(response.body));

    *appA->add_tags() = GROUPABLE_TAG;
    appsBody = printToString(apps);
    response = mockPut(API_PREFIX, appsBody);
    ASSERT_EQ(204, response.status);

    response = mockGet(API_PREFIX);
    parseFromString(response.body, /*out*/ responseApps);
    ASSERT_TRUE(isASubsetOf(apps, responseApps));

    response = mockGet(API_PREFIX + "/" + APP_NAME_A);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(printToString(*appA), TString(response.body));
}

TEST_F(AppApi, AppNames)
{
    const struct {
        std::string appName;
        bool pass;
    } appNames[] = {
        {"a.b", true},
        {"a.b1", true},
        {"a.b_1.c_1", true},
        {"", false},
        {"a", false},
        {".a", false},
        {"a.", false},
        {"a.1", false},
        {"a$", false},
        {".", false},
        {"a..b", false},
        {"a-b", false},
    };

    for(const auto& names: appNames) {
        auto response = mockGet(API_PREFIX + "/" + names.appName);
        if (names.pass) {
            ASSERT_EQ(404, response.status);
        } else {
            ASSERT_EQ(400, response.status);
        }
    }
}

TEST_F(AppApi, Delete)
{
    auto response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    auto defaultBody = response.body;

    ASSERT_EQ(405, mockDelete(API_PREFIX).status);
    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + APP_NAME_A).status);
    ASSERT_EQ(400, mockDelete(API_PREFIX + "/" + INVALID_APP_NAME).status);

    Apps apps;
    auto appA = apps.add_apps();
    appA->set_app_name(APP_NAME_A);
    *appA->add_tags() = GROUPABLE_TAG;
    auto appB = apps.add_apps();
    appB->set_app_name(APP_NAME_B);
    *appB->add_tags() = CORE_TAG;

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(apps)).status);

    ASSERT_EQ(200, mockGet(API_PREFIX + "/" + APP_NAME_A).status);

    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + APP_NAME_A).status);

    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + APP_NAME_A).status);

    response = mockGet(API_PREFIX + "/" + APP_NAME_B);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(printToString(*appB), TString(response.body));

    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + APP_NAME_B).status);

    response = mockGet(API_PREFIX);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(defaultBody, response.body);
}

} // maps::automotive::store_internal
