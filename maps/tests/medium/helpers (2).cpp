#include "helpers.h"

namespace std {

std::ostream& operator<<(std::ostream& os, const std::nullopt_t)
{
    return os << "std::nullopt";
}

}

namespace maps::wiki::social::tests {

std::string operator""_hours_ago(unsigned long long hours)
{
    return chrono::formatSqlDateTime(
        std::chrono::system_clock::now() - std::chrono::hours(hours)
    );
}

void approveTask(
    pqxx::transaction_base& txn,
    TId taskId,
    TUid closedBy,
    chrono::TimePoint closedAt)
{
    txn.exec(
        "UPDATE social.task "
        "SET "
            "closed_by = " + std::to_string(closedBy) + ", "
            "closed_at = '" + chrono::formatSqlDateTime(closedAt) + "', "
            "close_resolution = 'approve' "
        "WHERE event_id = " + std::to_string(taskId));
}

} // namespace maps::wiki::social::tests
