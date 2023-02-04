#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/automotive/updater/lib/stats.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <pqxx/pqxx>

using namespace maps::automotive::updater;
using namespace maps::automotive;
using namespace ::testing;

extern std::unique_ptr<stats::HeadunitStats> g_headunitStats;

namespace maps::automotive::updater {

namespace {

class LocalPostgresFixture : public Fixture {
public:
    LocalPostgresFixture() : Fixture()
    {
        postgres_.createExtension("hstore");
        postgres_.applyMigrations(
            std::string(ArcadiaSourceRoot()) +
            "/maps/automotive/store_internal/migrations");
        headunitStats_ = std::make_unique<stats::HeadunitStats>(
            postgres_.connectionString());
        std::swap(headunitStats_, g_headunitStats);
    }

    ~LocalPostgresFixture() { std::swap(headunitStats_, g_headunitStats); }

    void testStats(
        const std::string& handle,
        const std::string& body,
        const std::string& query,
        const std::vector<std::vector<std::string>>& values)
    {
        mockPost(handle, body);
        g_headunitStats->drain();
        checkQueryResults(query, values);
    }

    void checkQueryResults(
        const std::string& query,
        const std::vector<std::vector<std::string>>& values)
    {
        pqxx::connection conn(postgres_.connectionString());
        pqxx::work txn(conn);
        auto result = txn.exec(query);
        EXPECT_EQ(values.size(), result.size());

        for (size_t i = 0; i < values.size(); ++i) {
            EXPECT_EQ(result[i].size(), values[i].size());
            for (pqxx::row::size_type j = 0; j < result[i].size(); ++j) {
                EXPECT_EQ(result[i][j].as<std::string>(""), values[i][j]);
            }
        }
    }

private:
    maps::local_postgres::Database postgres_;
    std::unique_ptr<stats::HeadunitStats> headunitStats_;
};

const auto HANDLE = "/updater/2.x/"
                    "updates?type=taxi&mcu=astar&vendor=vendor&model=exp&"
                    "branch=testing&lang=ru_RU&headid=12345";

const auto CASKA_HANDLE = "/updater/2.x/"
                          "updates?type=taxi&mcu=t3_caska&vendor=vendor&"
                          "branch=testing&model=exp&lang=ru_RU&headid=12345";

const auto HANDLE_1X_PACKAGES =
    "/updater/1.x/"
    "updates?type=taxi&mcu=astar&vendor=vendor&model=exp&"
    "branch=testing&lang=ru_RU&headid=12345";

const auto HANDLE_1X_FIRMWARE =
    "/updater/1.x/"
    "firmware_updates?type=taxi&mcu=astar&vendor=vendor&model=exp&"
    "branch=testing&lang=ru_RU&headid=12345";

const auto EMPTY_BODY = R"(
    software {}
    firmware {}
)";

const auto FULL_BODY = R"(
    software { 
        packages { 
            name: "some.app"
            version {
                code: 123
            }
        }
        packages {
            name: "other.app"
            version {
                code: 456
                text: "beta"
            }
        }
    }
    firmware {
        properties {
            key: "p_key_a"
            value: "p_value_a"
        }
        properties {
            key: "p_key_b"
            value: "p_value_b"
        }
    }
    feature {
        name: "f_key_a"
        version {
            major: 1
        }
    }
    feature {
        name: "f_key_b"
        version {
            major: 2
            minor: 3
        }
    }
)";

const auto CHANGED_BODY = R"(
    software { 
        packages {
            name: "other.app"
            version {
                code: 4567
                text: "release"
            }
        }
    }
    firmware {
        properties {
            key: "p_key_b"
            value: "p_value_b2"
        }
    }
    feature {
        name: "f_key_a"
        version {
            major: 10
            minor: 20
        }
    }
)";

const auto FULL_PACKAGES_BODY_1X = R"(
    [{ "package": "some.app", "version": { "code": 10, "text": "beta" }},
     { "package": "other.app", "version": { "code": 20, "text": "" }}]
)";

const auto CHANGED_PACKAGES_BODY_1X = R"(
    [{ "package": "some.app", "version": { "code": 30, "text": "release" }}]
)";

const auto FULL_FIRMWARE_PROPERTIES_1X = R"( 
    { 
        "ro.build.date.utc": "0",
        "property_a": "value_a",
        "property_b": "value_b"
    }
)";

const auto CHANGED_FIRMWARE_PROPERTIES_1X = R"( 
    { 
        "ro.build.date.utc": "1"
    }
 )";

const auto HEADUNIT_QUERY = "select headid, uuid, scope, lang "
                            " from headunit;";

const auto HEADUNIT_FW_ID_QUERY = "select headid, firmware_version "
                                 " from headunit;";

const auto PACKAGES_QUERY = "select headunit.headid, headunit.uuid, "
                            " package_name, version_code, version_name "
                            " from headunit_packages "
                            " join headunit on headunit.\"id\" = "
                            " headunit_packages.headunit_id "
                            " order by headunit_packages.package_name";

const auto ATTRIBUTES_QUERY = "select headunit.headid, headunit.uuid, "
                              " attr_name, attr_type, value "
                              " from headunit_attrs "
                              " join headunit on headunit.\"id\" = "
                              " headunit_attrs.headunit_id "
                              " order by headunit_attrs.attr_name";

} // namespace

TEST_F(LocalPostgresFixture, HeadunitStatsHeadunitUpdate)
{
    testStats(
        HANDLE,
        EMPTY_BODY,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    testStats(
        CASKA_HANDLE,
        EMPTY_BODY,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"t3_caska", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});
}

TEST_F(LocalPostgresFixture, HeadunitStatsHeadunitPackages)
{
    testStats(
        HANDLE,
        FULL_BODY,
        PACKAGES_QUERY,
        {{"12345", "", "other.app", "456", "beta"},
         {"12345", "", "some.app", "123", ""}});

    testStats(
        HANDLE,
        CHANGED_BODY,
        PACKAGES_QUERY,
        {{"12345", "", "other.app", "4567", "release"}});

    testStats(HANDLE, EMPTY_BODY, PACKAGES_QUERY, {});
}

TEST_F(LocalPostgresFixture, HeadunitStatsHeadunitAttributes)
{
    testStats(
        HANDLE,
        FULL_BODY,
        ATTRIBUTES_QUERY,
        {{"12345", "", "f_key_a", "FEATURE", "1.0"},
         {"12345", "", "f_key_b", "FEATURE", "2.3"},
         {"12345", "", "p_key_a", "PROPERTY", "p_value_a"},
         {"12345", "", "p_key_b", "PROPERTY", "p_value_b"}});

    testStats(
        HANDLE,
        CHANGED_BODY,
        ATTRIBUTES_QUERY,
        {{"12345", "", "f_key_a", "FEATURE", "10.20"},
         {"12345", "", "p_key_b", "PROPERTY", "p_value_b2"}});

    testStats(HANDLE, EMPTY_BODY, ATTRIBUTES_QUERY, {});
}

TEST_F(LocalPostgresFixture, HeadunitStatsPackages1x)
{
    testStats(
        HANDLE_1X_PACKAGES,
        FULL_PACKAGES_BODY_1X,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    checkQueryResults(
        PACKAGES_QUERY,
        {{"12345", "", "other.app", "20", ""},
         {"12345", "", "some.app", "10", "beta"}});

    testStats(
        HANDLE_1X_PACKAGES,
        CHANGED_PACKAGES_BODY_1X,
        PACKAGES_QUERY,
        {{"12345", "", "some.app", "30", "release"}});
}

TEST_F(LocalPostgresFixture, HeadunitStatsProperties1x)
{
    testStats(
        HANDLE_1X_FIRMWARE,
        FULL_FIRMWARE_PROPERTIES_1X,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    checkQueryResults(
        ATTRIBUTES_QUERY,
        {
            {"12345", "", "property_a", "PROPERTY", "value_a"},
            {"12345", "", "property_b", "PROPERTY", "value_b"},
            {"12345", "", "ro.build.date.utc", "PROPERTY", "0"},
        });

    testStats(
        HANDLE_1X_FIRMWARE,
        CHANGED_FIRMWARE_PROPERTIES_1X,
        ATTRIBUTES_QUERY,
        {
            {"12345", "", "ro.build.date.utc", "PROPERTY", "1"},
        });
}

TEST_F(LocalPostgresFixture, HeadunitStatsPackagesProperties1x)
{
    testStats(
        HANDLE_1X_FIRMWARE,
        FULL_FIRMWARE_PROPERTIES_1X,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    testStats(
        HANDLE_1X_PACKAGES,
        FULL_PACKAGES_BODY_1X,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    checkQueryResults(
        ATTRIBUTES_QUERY,
        {
            {"12345", "", "property_a", "PROPERTY", "value_a"},
            {"12345", "", "property_b", "PROPERTY", "value_b"},
            {"12345", "", "ro.build.date.utc", "PROPERTY", "0"},
        });

    checkQueryResults(
        PACKAGES_QUERY,
        {{"12345", "", "other.app", "20", ""},
         {"12345", "", "some.app", "10", "beta"}});

    testStats(
        HANDLE_1X_FIRMWARE,
        CHANGED_FIRMWARE_PROPERTIES_1X,
        ATTRIBUTES_QUERY,
        {
            {"12345", "", "ro.build.date.utc", "PROPERTY", "1"},
        });

    checkQueryResults(
        PACKAGES_QUERY,
        {{"12345", "", "other.app", "20", ""},
         {"12345", "", "some.app", "10", "beta"}});
}

TEST_F(LocalPostgresFixture, PackagesAndFirmwareUpdates)
{
    testStats(
        HANDLE_1X_FIRMWARE,
        CHANGED_FIRMWARE_PROPERTIES_1X,
        HEADUNIT_FW_ID_QUERY,
        {{"12345", "1"}});

    testStats(
        HANDLE_1X_PACKAGES,
        FULL_PACKAGES_BODY_1X,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    checkQueryResults(
        HEADUNIT_FW_ID_QUERY,
        {{"12345", "1"}});

    testStats(
        HANDLE_1X_FIRMWARE,
        FULL_FIRMWARE_PROPERTIES_1X,
        HEADUNIT_FW_ID_QUERY,
        {{"12345", "0"}});

    testStats(
        HANDLE_1X_PACKAGES,
        FULL_PACKAGES_BODY_1X,
        HEADUNIT_QUERY,
        {{"12345",
          "",
          R"("mcu"=>"astar", "type"=>"taxi", "model"=>"exp", "branch"=>"testing", "vendor"=>"vendor")",
          "ru_RU"}});

    checkQueryResults(
        HEADUNIT_FW_ID_QUERY,
        {{"12345", "0"}});
}

} // namespace maps::automotive::updater
