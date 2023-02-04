#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/context.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <maps/libs/common/include/file_utils.h>

namespace maps::automotive::store_internal {

struct DbFixture: public testing::TestWithParam<std::string>
{
    DbFixture()
        : iamMock(""), clusterMock("")
    {
        db.createExtension("hstore");
        db.applyMigrations(appSourcePath("migrations"));
    }

    Config setupMocks(const std::string& env) {
        iamMock = http::addMock(
            "https://gw.db.yandex-team.ru/iam/v1/tokens",
            [](const http::MockRequest&) {
                return std::string(R"({"iamToken": "token"})");
            });

        auto cfg = getConfig(env);
        clusterMock = http::addMock(
            std::string("https://gw.db.yandex-team.ru/managed-postgresql/v1/clusters/")
                + cfg.postgres().yc().cluster_id() + "/hosts",
            [](const http::MockRequest&) {
                return std::string(R"({"hosts": [{"name": "localhost"}]})");
            });

        auto& pg = *cfg.mutable_postgres();
        pg.mutable_yc()->set_dummy_jwt(true);
        pg.set_port(db.port());
        pg.set_name(TString(db.dbname()));
        pg.set_user(TString(db.user()));
        return cfg;
    }

public:
    local_postgres::Database db;
    http::MockHandle iamMock, clusterMock;
};

TEST_P(DbFixture, load)
{
    auto cfg = setupMocks(GetParam());
    auto tankerMock = http::addMock(
        "https://tanker-beta.yandex-team.ru/api/legacy/keysets/json/",
        [](const http::MockRequest&) {
            return std::string("{}");
        });
    AppContext::init(std::move(cfg));
    EXPECT_FALSE(g_ctx->s3config().paths().firmwares().empty());
    EXPECT_FALSE(g_ctx->s3config().paths().packages().empty());
}
INSTANTIATE_TEST_SUITE_P(
    ContextLoad,
    DbFixture,
    testing::Values("unittest", "development", "testing", "production"));

TEST_F(DbFixture, noStartWithoutTanker)
{
    auto cfg = setupMocks("unittest");
    auto tankerFailsMock = http::addMock(
        "https://tanker-beta.yandex-team.ru/api/legacy/keysets/json/",
        [](const http::MockRequest&) {
            return http::MockResponse().withStatus(500);
        });
    EXPECT_THROW(
        AppContext::init(std::move(cfg)),
        LogicError);
}

}
