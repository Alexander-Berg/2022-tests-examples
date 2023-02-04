#include "controller_helpers.h"

#include "tests_common.h"
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/sync/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/sync/sync_objects.h>
#include <maps/wikimap/mapspro/services/editor/src/sync/sync_params.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/observer.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>

#include <maps/wikimap/mapspro/services/editor/src/actions/save_object/saveobject.h>
#include <maps/wikimap/mapspro/services/editor/src/moderation.h>
#include "helpers.h"

#include <yandex/maps/wiki/common/robot.h>
#include <yandex/maps/wiki/social/feedback/description.h>
#include <yandex/maps/wiki/social/feedback/commits.h>
#include <yandex/maps/wiki/social/feedback/agent.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <yandex/maps/wiki/social/feedback/task_patch.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <maps/wikimap/mapspro/libs/acl/include/acl_tool.h>
#include <maps/wikimap/mapspro/libs/acl/include/permissions.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>

#include <library/cpp/testing/unittest/registar.h>

#include <boost/lexical_cast.hpp>
#include <boost/optional.hpp>

#include <fstream>
#include <random>

namespace maps {
namespace wiki {
namespace tests {

namespace {

const std::string PERMISSIONS_XML_SOURCE = "/maps/wikimap/mapspro/cfg/editor/permissions.xml";
const bool FORCE = true;
const boost::optional<TId> NO_FEEDBACK_TASK_ID;
const StringMap NO_COMMIT_ATTRIBUTES;

std::string
performSaveObjectRequestXML(
    const std::string& requestBodyFilePath,
    const ObserverCollection& observers,
    TUid uid)
{
    auto parser = SaveObjectParser();
    const auto requestBody = loadFile(requestBodyFilePath);
    parser.parse(common::FormatType::XML, requestBody);

    const auto requestResult = performRequest<SaveObject>(
        observers,
        UserContext(uid, {}),
        FORCE,
        revision::TRUNK_BRANCH_ID,
        parser.objects(),
        parser.editContexts(),
        SaveObject::IsLocalPolicy::Manual,
        NO_FEEDBACK_TASK_ID,
        NO_COMMIT_ATTRIBUTES,
        requestBody
    );
    const auto formatter = Formatter::create(common::FormatType::XML);
    return (*formatter)(*requestResult);
}

} // namespace

std::string
performSaveObjectRequestJsonStr(
    const std::string& jsonRequestBody,
    const ObserverCollection& observers,
    TUid uid)
{
    validateJsonRequest(jsonRequestBody, "SaveObject");

    auto parser = SaveObjectParser();
    parser.parse(common::FormatType::JSON, jsonRequestBody);

    const auto requestResult = performRequest<SaveObject>(
        observers,
        UserContext(uid, {}),
        FORCE,
        revision::TRUNK_BRANCH_ID,
        parser.objects(),
        parser.editContexts(),
        SaveObject::IsLocalPolicy::Auto,
        NO_FEEDBACK_TASK_ID,
        NO_COMMIT_ATTRIBUTES,
        jsonRequestBody
    );

    const auto formatter = Formatter::create(common::FormatType::JSON, make_unique<TestFormatterContext>());
    auto result = (*formatter)(*requestResult);
    validateJsonResponse(result, "SaveObject");
    return result;
}

TOid
getObjectID(const std::string& getObjectResultJSON)
{
    auto parsedResult = json::Value::fromString(getObjectResultJSON);
    return boost::lexical_cast<TOid>(parsedResult["id"].toString());
}

std::string
performSaveObjectRequest(
    const std::string& requestBodyFilePath,
    const ObserverCollection& observers,
    TUid uid)
{
    if (requestBodyFilePath.ends_with(".json")) {
        return performSaveObjectRequestJsonStr(loadFile(requestBodyFilePath), observers, uid);
    } else if (requestBodyFilePath.ends_with(".xml")) {
        return performSaveObjectRequestXML(requestBodyFilePath, observers, uid);
    }

    REQUIRE(
        false,
        "Format of the file '" << requestBodyFilePath << "' can't be guessed from its extension."
    );
}

void
catchLogicException(std::function<void()> f, const std::string& status)
{
    try {
        f();
    } catch (const LogicException& ex) {
        WIKI_TEST_REQUIRE(ex.attrs().count("status"));
        WIKI_TEST_REQUIRE(ex.attrs().at("status") == status);
        return;
    }
    UNIT_FAIL("catchLogicException failed, status: " + status);
}

void
setModerationRole(TUid uid, social::ModerationMode moderationMode, TOid aoiId)
{
    auto work = cfg()->poolCore().masterWriteableTransaction();
    acl::ACLGateway aclGateway(*work);
    auto aoi = aclGateway.aoi(aoiId);

    auto user = aclGateway.user(uid);
    UNIT_ASSERT_NO_EXCEPTION(user.checkActiveStatus());

    auto roleName = moderation::toAclRoleName(moderationMode);
    auto role = aclGateway.role(roleName);
    UNIT_ASSERT_NO_EXCEPTION(aclGateway.createPolicy(user, role, aoi));
    work->commit();
}

void
performObjectsImport(const std::string& jsonPath, const std::string& connectionString)
{
    {   // import data from json
        std::ifstream json(ArcadiaSourceRoot() + "/maps/wikimap/mapspro/services/editor/src/" + jsonPath);
        UNIT_ASSERT_C(json, "Could not open file " << jsonPath);

        revisionapi::RevisionAPI revision(cfg()->poolCore());
        revision.importData(TESTS_USER, revisionapi::IdMode::StartFromJsonId, json);
    }

    // sync view
    auto executionState = std::make_shared<ExecutionState>();

    sync::SyncParams params {
        executionState,
        revision::TRUNK_BRANCH_ID,
        sync::SetProgressState::Yes
    };

    sync::BranchLocker locker(connectionString);
    sync::SyncObjects controller(std::move(params), locker);
    controller.run({});
    REQUIRE(
        !executionState->fail,
        "execution state"
            << ", canceled: " << executionState->cancel
            << ", failed: " << executionState->fail
    );
    locker.work().commit();
}

social::feedback::Task
addFeedbackTask()
{
    auto work = cfg()->poolSocial().masterWriteableTransaction();
    social::feedback::Agent agent(*work, TESTS_USER);
    auto task = agent.addTask(
        social::feedback::TaskNew(
            geolib3::Point2(0., 0.),
            social::feedback::Type::StreetName,
            "unit-tests-source",
            social::feedback::Description("task for unit tests description")
        )
    );
    auto updatedHeadTask = agent.revealTaskByIdCascade(task.id());
    work->commit();
    REQUIRE(updatedHeadTask, "task " << task.id() << " not found");
    return *updatedHeadTask;
}

social::feedback::Task
acquireFeedbackTask(social::TId taskId, social::TUid uid)
{
    auto work = cfg()->poolSocial().masterWriteableTransaction();
    auto task = social::feedback::GatewayRW(*work).updateTaskById(
            taskId,
            social::feedback::TaskPatch(uid).setAcquired());
    REQUIRE(task, "task " << taskId << " not found");
    work->commit();
    return *task;
}

void bindCommitToFeedbackTask(TId commitId, TId feedbackTaskId)
{
    auto work = cfg()->poolSocial().masterWriteableTransaction();
    social::feedback::bindCommitsToTask(*work, feedbackTaskId, {commitId});
    work->commit();
}

void populateACLPermissionsTree()
{
    maps::xml3::Doc aclPermissionsDoc(ArcadiaSourceRoot() + PERMISSIONS_XML_SOURCE);
    auto work = cfg()->poolCore().masterWriteableTransaction();
    acl::ACLGateway aclGateway(*work);
    acl::fillPermissionsTree(aclGateway, aclPermissionsDoc);
    work->commit();
}

namespace {
std::string uidToLogin(TUid uid)
{
    return "user_" + std::to_string(uid);
}

std::string uidToRole(TUid uid)
{
    return "role_" + std::to_string(uid);
}
} // namespace

TUid createRandomUser()
{
    static std::default_random_engine generator;
    std::uniform_int_distribution<TUid> distribution(TESTS_USER2 * 2, TESTS_USER2 * 3);
    TUid uid = distribution(generator);
    auto work = cfg()->poolCore().masterWriteableTransaction();
    acl::ACLGateway aclGateway(*work);
    auto user = aclGateway.createUser(uid, uidToLogin(uid), "description", TESTS_USER);
    auto role = aclGateway.createRole(uidToRole(uid), "");
    aclGateway.createPolicy(user, role, aclGateway.aoi(0));
    work->commit();
    return uid;
}

void setUserPermissions(TUid uid, std::list<std::list<std::string>>&& paths)
{
    auto work = cfg()->poolCore().masterWriteableTransaction();
    acl::ACLGateway aclGateway(*work);
    auto allPermissions = aclGateway.allPermissions();
    auto role = aclGateway.role(uidToRole(uid));
    for (auto& path : paths) {
        role.add(allPermissions.permission(path));
    }
    work->commit();
}

} // namespace tests
} // namespace wiki
} // namespace maps
