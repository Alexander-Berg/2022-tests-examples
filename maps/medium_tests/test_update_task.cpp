#include <maps/wikimap/feedback/api/src/libs/dbqueries/dbqueries.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/update_task.h>

#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

const std::string TVM_ALIAS = "maps-core-feedback-api";

const FeedbackTask SAMPLE_TASK_WITH_NMAPS_INTEGRATION{
    TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
    Service::Nmaps,
    ServiceObjectId("serviceObjectId"),
    "http://serviceObjectUrl",
    TaskStatus::InProgress,
    api::tests::EMPTY_ORIGINAL_TASK,
    Integration{{
        {Service::Nmaps, ServiceDesc{
            .serviceObjectId = ServiceObjectId{"serviceObjectId"},
            .serviceObjectUrl = "http://serviceObjectUrl",
        }},
    }},
    maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
    maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
};

struct StatusUpdateParamsTemplate {
    TaskStatus status;
    std::optional<NmapsResolution> resolution = std::nullopt;
    std::optional<fbapi::Service> initiatorService = Service::Nmaps;
    std::optional<std::string> message = "test message";
};

const std::vector<StatusUpdateParamsTemplate> PARAM_TEMPLATES{
    {TaskStatus::Accepted},
    {TaskStatus::Published},
    {TaskStatus::NeedInfo},
    {TaskStatus::Rejected, NmapsResolution::IncorrectData},
    {TaskStatus::Rejected, NmapsResolution::NoData},
    {TaskStatus::Rejected, NmapsResolution::NoInfo},
    {TaskStatus::Rejected, NmapsResolution::ProhibitedByRules},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentAuto},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentBigTask},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentMasstransit},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentOther},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentPedestrian},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentRoadEvents},
    {TaskStatus::Rejected, NmapsResolution::RedirectToContentTruck},
    {TaskStatus::Rejected, NmapsResolution::RedirectToPlatformAuto},
    {TaskStatus::Rejected, NmapsResolution::RedirectToPlatformBicycle},
    {TaskStatus::Rejected, NmapsResolution::RedirectToPlatformMasstransit},
    {TaskStatus::Rejected, NmapsResolution::RedirectToPlatformToponym},
    {TaskStatus::Rejected, NmapsResolution::RedirectToPlatformTruck},
    {TaskStatus::Rejected, NmapsResolution::RedirectToSprav},
    {TaskStatus::Rejected, NmapsResolution::RedirectToSupport},
    {TaskStatus::Rejected, NmapsResolution::Spam},
};

UpdateTaskParams updateParamsFromTemplate(
    StatusUpdateParamsTemplate paramTemplate)
{
    std::optional<RequestTemplateId> requestTemplateId;
    if (paramTemplate.status == TaskStatus::Rejected) {
        ASSERT(paramTemplate.resolution.has_value());
    } else if (paramTemplate.status == TaskStatus::NeedInfo) {
        requestTemplateId = RequestTemplateId::NeedMoreInfo;
    }

    std::optional<std::string> resolutionStr;
    if (paramTemplate.resolution.has_value()) {
        resolutionStr = std::string{toString(*paramTemplate.resolution)};
    }

    UpdateTaskParams updateTaskParams;
    updateTaskParams.newStatus = paramTemplate.status;
    updateTaskParams.resolution = std::move(resolutionStr);
    updateTaskParams.initiatorService = paramTemplate.initiatorService;
    updateTaskParams.message = std::move(paramTemplate.message);
    updateTaskParams.requestTemplateId = std::move(requestTemplateId);
    return updateTaskParams;
}

} // namespace

Y_UNIT_TEST_SUITE(test_update_task)
{

Y_UNIT_TEST(task_status_and_resolution_update_in_db)
{
    std::unique_ptr<Globals> globals = Globals::create(
        SRC_("data/feedback_api.conf"), true /* dryRun */);
    ASSERT(globals != nullptr);

    auto txn = dbPool().masterWriteableTransaction();
    insertTask(*txn, SAMPLE_TASK_WITH_NMAPS_INTEGRATION);

    for (auto&& paramTemplate : PARAM_TEMPLATES) {
        UNIT_ASSERT_NO_EXCEPTION(
            updateTask(
                *globals,
                updateParamsFromTemplate(paramTemplate),
                SAMPLE_TASK_WITH_NMAPS_INTEGRATION)
            .exec(*txn));

        auto updatedTask = dbqueries::loadFeedbackTask(
            *txn,
            SAMPLE_TASK_WITH_NMAPS_INTEGRATION.id);
        ASSERT(updatedTask.has_value());
        UNIT_ASSERT_VALUES_EQUAL(updatedTask->status, paramTemplate.status);
        if (paramTemplate.resolution.has_value()) {
            auto nmapsIntegration =
                updatedTask->integration.get(Service::Nmaps);
            UNIT_ASSERT(nmapsIntegration.has_value());
            UNIT_ASSERT_VALUES_EQUAL(
                nmapsIntegration->resolution.value_or("empty"),
                toString(*paramTemplate.resolution));
        }
    }
}

} // test_update_task suite

} // namespace maps::wiki::feedback::api::tests
