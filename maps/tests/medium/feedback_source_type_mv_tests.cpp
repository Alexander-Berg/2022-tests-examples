#include <yandex/maps/wiki/unittest/arcadia.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yandex/maps/wiki/common/pg_utils.h>
#include <yandex/maps/wiki/common/robot.h>
#include <yandex/maps/wiki/social/feedback/agent.h>
#include <yandex/maps/wiki/social/feedback/enums.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <yandex/maps/wiki/social/feedback/mv_source_type.h>
#include <yandex/maps/wiki/social/feedback/task.h>
#include <yandex/maps/wiki/social/feedback/task_filter.h>
#include <yandex/maps/wiki/social/feedback/task_patch.h>

#include <maps/wikimap/mapspro/libs/social/magic_strings.h>
#include <maps/wikimap/mapspro/libs/social/feedback/util.h>
#include <maps/wikimap/mapspro/libs/social/helpers.h>

namespace maps::wiki::social::feedback::tests {

namespace {

const std::string SOURCE = "source";
const std::string SOURCE_1 = "source1";
const std::string SOURCE_2 = "source2";
const std::string SOURCE_3 = "source3";
const std::string SOURCE_4 = "source4";
const std::string SOURCE_FEEDBACK = "fbapi";
const std::string SOURCE_FEEDBACK_2 = "fbapi-samsara";
const std::string SOURCE_FEEDBACK_3 = "nmaps-complaint";

const Type TYPE = Type::Parking;
const Type TYPE_1 = Type::Maneuver;
const Type TYPE_2 = Type::Barrier;
const Type TYPE_3 = Type::RoadClosure;
const Type TYPE_4 = Type::RoadAnnotation;

const TUid SOME_USER_ID = 1001;

const bool HIDDEN = true;
const Workflow WORKFLOW = Workflow::Task;
const std::string MIN_CREATED_AT = "now()";
const std::string MAX_CREATED_AT = "now()";


struct SourceTypeMvRow
{
    std::string source;
    Type type;
    bool hidden;
    Workflow workflow;
    UIFilterStatus status;
    std::string min_created_at;
    std::string max_created_at;
};

TaskForUpdate addRevealedTask(
    Agent& agent,
    const TaskNew& newTask)
{
    auto createdTask = agent.addTask(newTask);
    auto revealedTask = agent.revealTaskByIdCascade(createdTask.id());
    UNIT_ASSERT(revealedTask);
    return *revealedTask;
}

/*
 *  It's impossible to modify materialized view directly. The only way is
 *  to modify feedback_task table and call refresh on view.
 *  Position, description, resolution are random here.
 */
void addSourceTypeMvRow(const SourceTypeMvRow& row, pqxx::transaction_base& txn)
{
    GatewayRW gatewayRw(txn);
    Agent agent(txn, SOME_USER_ID);
    TaskNew newTask({0, 0}, row.type, row.source, Description("descr"));
    newTask.hidden = row.hidden;

    auto earlierTask = addRevealedTask(agent, newTask);
    auto laterTask = addRevealedTask(agent, newTask);

    TaskPatch patch(common::ROBOT_UID);
    if (row.status == UIFilterStatus::NeedInfo) {
        patch.setBucket(Bucket::NeedInfo);
    }
    if (row.status == UIFilterStatus::Resolved) {
        patch.setResolution(Resolution::createAccepted());
    }

    std::map<TId, std::string> taskIdToCreationTime = {
        {earlierTask.id(), row.min_created_at},
        {laterTask.id(), row.max_created_at}
    };

    for (const auto& [id, time] : taskIdToCreationTime) {
        gatewayRw.updateTaskById(id, patch);
        execUpdateTasks(
            txn,
            sql::col::STATE_MODIFIED_AT + " = " + time,
            sql::col::ID + " = " + std::to_string(id)
        );
    }

    refreshSourceTypeMv(txn);
}

} // namespace anonymous


Y_UNIT_TEST_SUITE(feedback_source_mv_tests) {

Y_UNIT_TEST_F(no_filters, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& socialTxn = socialTxnHandle.get();

    SourceTypeMvRow row{
        SOURCE, TYPE, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };
    addSourceTypeMvRow(row, socialTxn);

    auto sources = mvSourcesByFilter(socialTxn, MvSourceTypeFilter());

    UNIT_ASSERT_EQUAL(sources.size(), 1);
    UNIT_ASSERT_EQUAL(*sources.begin(), SOURCE);

    auto types = mvTypesByFilter(socialTxn, MvSourceTypeFilter());

    UNIT_ASSERT_EQUAL(types.size(), 1);
    UNIT_ASSERT_EQUAL(*types.begin(), TYPE);
}

Y_UNIT_TEST_F(status_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& txn = socialTxnHandle.get();

    SourceTypeMvRow rowClosed {
        SOURCE_1, TYPE_1, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };

    SourceTypeMvRow rowOpened {
        SOURCE_2, TYPE_2, HIDDEN, WORKFLOW,
        UIFilterStatus::Opened, MIN_CREATED_AT, MAX_CREATED_AT
    };

    SourceTypeMvRow rowNeedInfo {
        SOURCE_3, TYPE_3, HIDDEN, WORKFLOW,
        UIFilterStatus::NeedInfo, MIN_CREATED_AT, MAX_CREATED_AT
    };

    addSourceTypeMvRow(rowClosed, txn);
    addSourceTypeMvRow(rowOpened, txn);
    addSourceTypeMvRow(rowNeedInfo, txn);

    // Check sources
    //
    {
        auto sourcesClosed = mvSourcesByFilter(
            txn, MvSourceTypeFilter().status(UIFilterStatus::Resolved)
        );

        UNIT_ASSERT_EQUAL(sourcesClosed.size(), 1);
        UNIT_ASSERT_EQUAL(*sourcesClosed.begin(), SOURCE_1);
    }
    {
        auto sourcesOpened = mvSourcesByFilter(
            txn, MvSourceTypeFilter().status(UIFilterStatus::Opened)
        );

        UNIT_ASSERT_EQUAL(sourcesOpened.size(), 1);
        UNIT_ASSERT_EQUAL(*sourcesOpened.begin(), SOURCE_2);
    }
    {
        auto sources = mvSourcesByFilter(
            txn, MvSourceTypeFilter().status(UIFilterStatus::NeedInfo)
        );

        UNIT_ASSERT_EQUAL(sources.size(), 1);
        UNIT_ASSERT_EQUAL(*sources.begin(), SOURCE_3);
    }

    // Check types
    //
    {
        auto typesClosed = mvTypesByFilter(
            txn, MvSourceTypeFilter().status(UIFilterStatus::Resolved)
        );

        UNIT_ASSERT_EQUAL(typesClosed.size(), 1);
        UNIT_ASSERT_EQUAL(*typesClosed.begin(), TYPE_1);
    }
    {
        auto typesOpened = mvTypesByFilter(
            txn, MvSourceTypeFilter().status(UIFilterStatus::Opened)
        );

        UNIT_ASSERT_EQUAL(typesOpened.size(), 1);
        UNIT_ASSERT_EQUAL(*typesOpened.begin(), TYPE_2);
    }
    {
        auto types = mvTypesByFilter(
            txn, MvSourceTypeFilter().status(UIFilterStatus::NeedInfo)
        );

        UNIT_ASSERT_EQUAL(types.size(), 1);
        UNIT_ASSERT_EQUAL(*types.begin(), TYPE_3);
    }
}

Y_UNIT_TEST_F(hidden_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& txn = socialTxnHandle.get();

    SourceTypeMvRow rowHidden {
        SOURCE_1, TYPE_1, true /*hidden*/, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };

    SourceTypeMvRow rowDisplayed {
        SOURCE_2, TYPE_2, false /*hidden*/, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };

    addSourceTypeMvRow(rowHidden, txn);
    addSourceTypeMvRow(rowDisplayed, txn);

    // Check sources
    //
    auto sourcesHidden = mvSourcesByFilter(
        txn, MvSourceTypeFilter().hidden(true)
    );

    UNIT_ASSERT_EQUAL(sourcesHidden.size(), 1);
    UNIT_ASSERT_EQUAL(*sourcesHidden.begin(), SOURCE_1);

    auto sourcesDisplayed = mvSourcesByFilter(
        txn, MvSourceTypeFilter().hidden(false)
    );

    UNIT_ASSERT_EQUAL(sourcesDisplayed.size(), 1);
    UNIT_ASSERT_EQUAL(*sourcesDisplayed.begin(), SOURCE_2);

    // Check types
    //
    auto typesHidden = mvTypesByFilter(
        txn, MvSourceTypeFilter().hidden(true)
    );

    UNIT_ASSERT_EQUAL(typesHidden.size(), 1);
    UNIT_ASSERT_EQUAL(*typesHidden.begin(), TYPE_1);

    auto typesDisplayed = mvTypesByFilter(
        txn, MvSourceTypeFilter().hidden(false)
    );

    UNIT_ASSERT_EQUAL(typesDisplayed.size(), 1);
    UNIT_ASSERT_EQUAL(*typesDisplayed.begin(), TYPE_2);
}

Y_UNIT_TEST_F(types_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& txn = socialTxnHandle.get();

    SourceTypeMvRow row1 {
        SOURCE_1, TYPE_1, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };
    SourceTypeMvRow row2 {
        SOURCE_2, TYPE_1, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };
    SourceTypeMvRow row3 {
        SOURCE_3, TYPE_2, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };

    addSourceTypeMvRow(row1, txn);
    addSourceTypeMvRow(row2, txn);
    addSourceTypeMvRow(row3, txn);

    // Check sources
    //
    auto sources = mvSourcesByFilter(
        txn,
        MvSourceTypeFilter().types(Types{TYPE_1})
    );

    UNIT_ASSERT_EQUAL(sources.size(), 2);
    UNIT_ASSERT_EQUAL(sources, std::set<std::string>({SOURCE_1, SOURCE_2}));
}

Y_UNIT_TEST_F(sources_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& txn = socialTxnHandle.get();

    SourceTypeMvRow row1 {
        SOURCE_1, TYPE_1, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };
    SourceTypeMvRow row2 {
        SOURCE_1, TYPE_2, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };
    SourceTypeMvRow row3 {
        SOURCE_3, TYPE_3, HIDDEN, WORKFLOW,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };

    addSourceTypeMvRow(row1, txn);
    addSourceTypeMvRow(row2, txn);
    addSourceTypeMvRow(row3, txn);

    // Check types
    //
    auto types = mvTypesByFilter(
        txn,
        MvSourceTypeFilter().sources(std::vector<std::string>{SOURCE_1})
    );

    UNIT_ASSERT_EQUAL(types.size(), 2);
    std::sort(types.begin(), types.end());
    UNIT_ASSERT_EQUAL(types, Types({TYPE_1, TYPE_2}));
}

Y_UNIT_TEST_F(age_types_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& txn = socialTxnHandle.get();

    SourceTypeMvRow row1 {
        SOURCE_FEEDBACK, TYPE_1, HIDDEN, Workflow::Feedback,
        UIFilterStatus::Resolved, "now() - interval '25 hours'", "now() - interval '23 hours'"
    };
    SourceTypeMvRow row2 {
        SOURCE_FEEDBACK_2, TYPE_2, HIDDEN, Workflow::Feedback,
        UIFilterStatus::Resolved, "now() - interval '26 hours'", "now() - interval '25 hours'"
    };
    SourceTypeMvRow row3 {
        SOURCE_FEEDBACK_3, TYPE_3, HIDDEN, Workflow::Feedback,
        UIFilterStatus::Resolved, "now()", "now()"
    };
    SourceTypeMvRow row4 {
        SOURCE_4, TYPE_4, HIDDEN, Workflow::Task,
        UIFilterStatus::Resolved, "now() - interval '2 year'", "now() - interval '2 year'"
    };

    addSourceTypeMvRow(row1, txn);
    addSourceTypeMvRow(row2, txn);
    addSourceTypeMvRow(row3, txn);
    addSourceTypeMvRow(row4, txn);

    // Check sources
    //
    auto sourcesOld = mvSourcesByFilter(
        txn,
        MvSourceTypeFilter().ageTypes(AgeTypes{AgeType::Old})
    );

    UNIT_ASSERT_EQUAL(sourcesOld.size(), 2);
    UNIT_ASSERT_EQUAL(sourcesOld, std::set<std::string>({SOURCE_FEEDBACK, SOURCE_FEEDBACK_2}));

    auto sourcesNew = mvSourcesByFilter(
        txn,
        MvSourceTypeFilter().ageTypes(AgeTypes{AgeType::New})
    );

    UNIT_ASSERT_EQUAL(sourcesNew.size(), 3);
    UNIT_ASSERT_EQUAL(sourcesNew, std::set<std::string>({SOURCE_FEEDBACK, SOURCE_FEEDBACK_3, SOURCE_4}));

    auto sourcesNone = mvSourcesByFilter(txn, MvSourceTypeFilter().ageTypes(AgeTypes{}));
    UNIT_ASSERT(sourcesNone.empty());

    // Check types
    //
    auto typesOld = mvTypesByFilter(
        txn,
        MvSourceTypeFilter().ageTypes(AgeTypes{AgeType::Old})
    );

    UNIT_ASSERT_EQUAL(typesOld.size(), 2);
    std::sort(typesOld.begin(), typesOld.end());
    UNIT_ASSERT_EQUAL(typesOld, Types({TYPE_1, TYPE_2}));

    auto typesNew = mvTypesByFilter(
        txn,
        MvSourceTypeFilter().ageTypes(AgeTypes{AgeType::New})
    );

    UNIT_ASSERT_EQUAL(typesNew.size(), 3);
    std::sort(typesNew.begin(), typesNew.end());
    UNIT_ASSERT_EQUAL(typesNew, Types({TYPE_1, TYPE_3, TYPE_4}));
}

Y_UNIT_TEST_F(workflows_filter, unittest::ArcadiaDbFixture)
{
    auto socialTxnHandle = pool().masterWriteableTransaction();
    auto& txn = socialTxnHandle.get();

    SourceTypeMvRow row1 {
        SOURCE_FEEDBACK, TYPE_1, HIDDEN, Workflow::Feedback,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };
    SourceTypeMvRow row2 {
        SOURCE_2, TYPE_2, HIDDEN, Workflow::Task,
        UIFilterStatus::Resolved, MIN_CREATED_AT, MAX_CREATED_AT
    };

    addSourceTypeMvRow(row1, txn);
    addSourceTypeMvRow(row2, txn);

    // Check sources
    //
    auto sourcesFeedback = mvSourcesByFilter(
        txn, MvSourceTypeFilter().workflows(Workflows{Workflow::Feedback})
    );

    UNIT_ASSERT_EQUAL(sourcesFeedback.size(), 1);
    UNIT_ASSERT_EQUAL(sourcesFeedback, std::set<std::string>({SOURCE_FEEDBACK}));

    auto sourcesTask = mvSourcesByFilter(
        txn, MvSourceTypeFilter().workflows(Workflows{Workflow::Task})
    );

    UNIT_ASSERT_EQUAL(sourcesTask.size(), 1);
    UNIT_ASSERT_EQUAL(sourcesTask, std::set<std::string>({SOURCE_2}));

    // Check types
    //
    auto typesFeedback = mvTypesByFilter(
        txn, MvSourceTypeFilter().workflows(Workflows{Workflow::Feedback})
    );

    UNIT_ASSERT_EQUAL(typesFeedback.size(), 1);
    UNIT_ASSERT_EQUAL(typesFeedback, Types({TYPE_1}));

    auto typesTask = mvTypesByFilter(
        txn, MvSourceTypeFilter().workflows(Workflows{Workflow::Task})
    );

    UNIT_ASSERT_EQUAL(typesTask.size(), 1);
    UNIT_ASSERT_EQUAL(typesTask, Types({TYPE_2}));
}

}

} // namespace maps::wiki::social::feedback::tests
