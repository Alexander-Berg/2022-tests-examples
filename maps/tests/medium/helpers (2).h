#pragma once

#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>
#include <yandex/maps/wiki/social/common.h>
#include <yandex/maps/wiki/social/moderation_time_intervals.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <yandex/maps/wiki/unittest/query_helpers.h>

#include <maps/libs/chrono/include/time_point.h>
#include <pqxx/pqxx>

#include <library/cpp/testing/unittest/registar.h>

namespace std {

template <typename T>
std::ostream& operator<<(std::ostream& os, const std::optional<T>& optValue)
{
    if (!optValue) {
        return os << "std::nullopt";
    }

    return os << *optValue;
}

std::ostream& operator<<(std::ostream& os, const std::nullopt_t);

}

namespace maps::wiki::social::tests {

struct DbFixture: public unittest::ArcadiaDbFixture
{
    pqxx::connection conn;
    const std::string SCHEMA = "social";
    unittest::QueryHelpers queryHelpers;

    DbFixture()
        : conn(connectionString())
        , queryHelpers(conn)
    {}

    pqxx::result applyQuery(const std::string& query) {
        return queryHelpers.applyQueryToSchema(SCHEMA, query);
    }

    // Suspicious users
    std::vector<uint64_t> suspiciousUsersIds() {
        auto rows = queryHelpers.applyQueryToSchema(
            SCHEMA,
            "SELECT uid FROM suspicious_users"
        );

        std::vector<uint64_t> result;
        for (const auto& row: rows) {
            result.push_back(row[0].as<uint64_t>());
        }
        std::sort(result.begin(), result.end());
        return result;
    }

    template<typename T, pqxx::row::size_type COLUMN = 0>
    T execForSuspiciousUser(TUid uid, const std::string& statement) {
        auto rows = queryHelpers.applyQueryToSchema(
            SCHEMA,
            "SELECT " + statement + " FROM suspicious_users WHERE uid = " + std::to_string(uid)
        );
        REQUIRE(!rows.empty(), "Statement returned nothing.");
        REQUIRE(
            COLUMN < rows.columns(),
            "Statement returned less columns (" + std::to_string(rows.columns()) + ") "
            "than expected (at least" + std::to_string(COLUMN + 1) + ").");

        return rows[0][COLUMN].as<T>();
    }
};

template <typename ContainerOfValuesWithMemberFunctionId>
TIds getIds(const ContainerOfValuesWithMemberFunctionId& container)
{
    TIds result;
    for (const auto& value: container) {
        result.insert(value.id());
    }
    return result;
}

std::string operator""_hours_ago(unsigned long long hours);

const ModerationTimeIntervals TEST_MODERATION_TIME_INTERVALS = {
    .supervisorDelay          = std::chrono::minutes(5),
    .superModeratorDelay      = std::chrono::hours(24),
    .moderatorOldTaskAge      = std::chrono::hours(24),
    .superModeratorOldTaskAge = std::chrono::hours(36)
};

void approveTask(
    pqxx::transaction_base& txn,
    TId taskId,
    TUid closedBy,
    chrono::TimePoint closedAt);
} // namespace maps::wiki::social::tests
