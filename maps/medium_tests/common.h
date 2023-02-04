#pragma once

#include <maps/wikimap/feedback/api/src/samsara_importer/lib/importer.h>

namespace maps::wiki::feedback::api::samsara_importer::tests {

const samsara::Client& samsaraClientStatic();

class TestGlobals : public IGlobals
{
public:
    explicit TestGlobals(pgpool3::Pool& dbPool);

    pgpool3::Pool& pool() const override;
    const avatars::Client& avatarsClient() const override;
    const samsara::Client& samsaraClient() const override;
    const ClassificationToRedirectRule& samsaraToTrackerRules() const override;
    bool dryRun() const override { return false; }

private:
    pgpool3::Pool& dbPool_;
    ClassificationToRedirectRule samsaraToTrackerRules_;
};

std::vector<TaskId> getAllTaskIds(pqxx::transaction_base& txn);

FeedbackTask loadMandatoryFeedbackTask(
    pqxx::transaction_base& txn,
    const TaskId& taskId);

} // namespace maps::wiki::feedback::api::samsara_importer:tests
