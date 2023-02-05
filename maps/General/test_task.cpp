#include <maps/sprav/callcenter/libs/dao/request.h>
#include <maps/sprav/callcenter/libs/dao/task.h>
#include <maps/sprav/callcenter/libs/test_helpers/types.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>

#include <maps/sprav/callcenter/proto/request.pb.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>


namespace maps::sprav::callcenter::tests {

MATCHER(ProtoEq, "") {
    return ::testing::ExplainMatchResult(
        NGTest::EqualsProto(std::get<0>(arg)),
        std::get<1>(arg),
        result_listener
    );
}

class TaskDaoTest : public testing::Test {
public:
    auto createTransaction() {
        return db->pool().masterWriteableTransaction();
    }

    proto::Request baseRequest() {
        proto::Request request;
        request.set_receive_time(
            std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now().time_since_epoch()
            ).count()
        );
        request.set_status(proto::Request::Status::NEW);
        request.set_workflow_status(proto::Request::WorkflowStatus::QUEUED);
        return request;
    }

    proto::Request createRequest(uint64_t permalink, pqxx::transaction_base& tx) {
        auto request = baseRequest();
        request.set_permalink(permalink);
        request.set_id(dao::Request::create(request, tx));
        return request;
    }

    proto::Request createRequestWithPhones(std::vector<std::string> phones, pqxx::transaction_base& tx) {
        auto request = baseRequest();
        for (const auto& phone : phones) {
            request.mutable_related_phones()->Add(TString(phone));
        }
        request.set_id(dao::Request::create(request, tx));
        return request;
    }

    proto::Request createRequestWithUserId(uint64_t userId, pqxx::transaction_base& tx) {
        auto request = baseRequest();
        request.set_user_id(userId);
        request.set_id(dao::Request::create(request, tx));
        return request;
    }

    proto::Request createRequestWithChain(uint64_t chainId, pqxx::transaction_base& tx) {
        auto request = baseRequest();
        request.set_chain_id(chainId);
        request.set_id(dao::Request::create(request, tx));
        return request;
    }

    proto::TaskCompanyUpdate taskCompanyUpdate(int64_t taskId, uint64_t requestId, uint64_t permalink) {
        proto::TaskCompanyUpdate update;
        update.set_task_id(taskId);
        update.set_company_id(permalink);
        update.set_request_id(requestId);
        update.set_user_id(1);
        update.set_create_time(0);
        update.set_updated(0);
        return update;
    }

    std::vector<proto::Request> concat(const std::vector<std::vector<proto::Request>>& requests) {
        std::vector<proto::Request> result;
        for (const auto& r : requests) {
            result.insert(result.end(), r.begin(), r.end());
        }
        return result;
    }

    std::vector<int64_t> taskIds(const std::vector<task::TaskDataPtr>& tasks) {
        std::vector<int64_t> result;
        result.reserve(tasks.size());
        std::transform(tasks.begin(), tasks.end(), std::back_inserter(result), [](const auto& task){ return task->id(); });
        return result;
    }

    static void SetUpTestSuite() {
        db.reset(new DbFixture());
    }

    static void TearDownTestSuite() {
        db.reset();
    }

public:
    static std::unique_ptr<DbFixture> db;
};

std::unique_ptr<DbFixture> TaskDaoTest::db = nullptr;

TEST_F(TaskDaoTest, CreateAndGetTask) {
    auto tx = createTransaction();

    auto createdTaskData = dao::TaskActive::createFromRequests({}, "actualization", *tx);

    auto readActiveTaskData = dao::TaskActive::get(createdTaskData->id(), *tx);
    auto readAnyTaskData = dao::TaskAny::get(createdTaskData->id(), *tx);
    auto readArchiveTaskData = dao::TaskArchive::get(createdTaskData->id(), *tx);

    EXPECT_TRUE(readActiveTaskData.has_value());
    EXPECT_TRUE(readAnyTaskData.has_value());
    EXPECT_FALSE(readArchiveTaskData.has_value());

    EXPECT_THAT(readActiveTaskData.value()->task(), NGTest::EqualsProto(createdTaskData->task()));
    EXPECT_THAT(readAnyTaskData.value()->task(), NGTest::EqualsProto(createdTaskData->task()));
}

TEST_F(TaskDaoTest, FillRequestsSingleTask) {
    auto tx = createTransaction();

    std::vector<proto::Request> requests = {createRequest(1, *tx), createRequest(2, *tx), createRequest(1, *tx)};

    auto createdTaskData = dao::TaskActive::createFromRequests(requests, "actualization", *tx);
    EXPECT_EQ(createdTaskData->companies().size(), 2u);
    EXPECT_EQ(createdTaskData->requests().size(), 3u);

    auto readTaskData = dao::TaskAny::get(createdTaskData->id(), *tx).value();
    dao::TaskActive::fillRequests({readTaskData}, *tx);
    EXPECT_EQ(readTaskData->requests().size(), 3u);

    EXPECT_THAT(readTaskData->requests(), ::testing::UnorderedPointwise(ProtoEq(), createdTaskData->requests()));
}

TEST_F(TaskDaoTest, FillRequestsMultipleTasks) {
    auto tx = createTransaction();
    std::vector<proto::Request> requests = {
        createRequest(1, *tx), createRequest(2, *tx), createRequest(1, *tx), createRequest(4, *tx)
    };

    std::vector<task::TaskDataPtr> createdTaskData;
    createdTaskData.push_back(dao::TaskActive::createFromRequests({requests[0], requests[1], requests[2]}, "actualization", *tx));
    createdTaskData.push_back(dao::TaskActive::createFromRequests({requests[3]}, "actualization", *tx));
    createdTaskData.push_back(dao::TaskActive::createFromRequests({}, "actualization", *tx));

    std::vector<task::TaskDataPtr> readTaskData;
    for (const auto& taskData : createdTaskData) {
        readTaskData.push_back(dao::TaskActive::get(taskData->id(), *tx).value());
    }
    dao::TaskActive::fillRequests(readTaskData, *tx);

    for (const auto i : xrange(createdTaskData.size())) {
        EXPECT_THAT(readTaskData[i]->requests(), ::testing::UnorderedPointwise(ProtoEq(), createdTaskData[i]->requests()));
    }
}

TEST_F(TaskDaoTest, FillCommitIdsSingleTask) {
    auto tx = createTransaction();

    std::vector<proto::Request> requests = {createRequest(1, *tx), createRequest(2, *tx), createRequest(1, *tx)};

    auto taskData = dao::TaskActive::createFromRequests(requests, "actualization", *tx);

    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES (" + std::to_string(taskData->id()) + ", 1, 123456789)");
    dao::TaskAny::fillPermalinkCommits({taskData}, *tx);

    for (const auto& [_, company] : taskData->permalinkCompanies()) {
        if (company.permalink() == 1) {
            EXPECT_EQ(company.commitId().value(), 123456789);
        } else {
            EXPECT_FALSE(company.commitId().has_value());
        }
    }

    auto taskData2 = dao::TaskActive::createFromRequests({createRequestWithPhones({"123"}, *tx)}, "actualization", *tx);
    dao::TaskAny::fillPermalinkCommits({taskData2}, *tx);

    for (const auto& [_, company] : taskData2->permalinkCompanies()) {
        EXPECT_FALSE(company.commitId().has_value());
    }
}

TEST_F(TaskDaoTest, FillCommitIdsMultipleTasks) {
    auto tx = createTransaction();
    std::vector<proto::Request> requests = {
        createRequest(1, *tx), createRequest(2, *tx), createRequest(1, *tx), createRequest(4, *tx)
    };

    std::vector<task::TaskDataPtr> taskData;
    taskData.push_back(dao::TaskActive::createFromRequests({requests[0], requests[1], requests[2]}, "actualization", *tx));
    taskData.push_back(dao::TaskActive::createFromRequests({requests[3]}, "actualization", *tx));
    taskData.push_back(dao::TaskActive::createFromRequests({}, "actualization", *tx));

    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES (" + std::to_string(taskData[0]->id()) + ", 1, 123456789)");
    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES ( 99999999 , 1, 1111111)");
    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES (" + std::to_string(taskData[1]->id()) + ", 4, 987654321)");

    dao::TaskActive::fillPermalinkCommits(taskData, *tx);

    for (const auto& [_, company] : taskData[0]->permalinkCompanies()) {
        if (company.permalink() == 1) {
            EXPECT_EQ(company.commitId().value(), 123456789);
        } else {
            EXPECT_FALSE(company.commitId().has_value());
        }
    }

    for (const auto& [_, company] : taskData[1]->permalinkCompanies()) {
        if (company.permalink() == 4) {
            EXPECT_EQ(company.commitId().value(), 987654321);
        } else {
            EXPECT_FALSE(company.commitId().has_value());
        }
    }
}

TEST_F(TaskDaoTest, FillCompanyUpdatesSingleTask) {
    auto tx = createTransaction();

    std::vector<proto::Request> requests = {createRequest(1, *tx), createRequest(2, *tx), createRequestWithPhones({"12345"}, *tx)};

    auto taskData = dao::TaskActive::createFromRequests(requests, "actualization", *tx);

    std::vector<proto::TaskCompanyUpdate> updates = {
        taskCompanyUpdate(taskData->id(), 0, 1),
        taskCompanyUpdate(taskData->id(), requests[2].id(), 0)
    };

    for (const auto& update : updates) {
        maps::wiki::query_builder::InsertQuery query("tasker.task_company_update");
        formatModifyQuery(update, query);
        query.exec(*tx);
    }

    dao::TaskAny::fillCompanyUpdates({taskData}, *tx);

    for (const auto& company : taskData->companies()) {
        if (company->requests()[0].permalink() == 1) {
            EXPECT_THAT(company->companyUpdate(), NGTest::EqualsProto(updates[0]));
        } else if (!company->requests()[0].has_permalink()) {
            EXPECT_THAT(company->companyUpdate(), NGTest::EqualsProto(updates[1]));
        }
    }
}

TEST_F(TaskDaoTest, FillCompanyUpdatesMultipleTasks) {
    auto tx = createTransaction();

    std::vector<proto::Request> requests = {createRequest(1, *tx), createRequest(2, *tx), createRequestWithPhones({"12345"}, *tx)};

    std::vector<task::TaskDataPtr> taskData;
    taskData.push_back(dao::TaskActive::createFromRequests({requests[0], requests[1]}, "actualization", *tx));
    taskData.push_back(dao::TaskActive::createFromRequests({requests[2]}, "actualization", *tx));
    taskData.push_back(dao::TaskActive::createFromRequests({}, "actualization", *tx));

    std::vector<proto::TaskCompanyUpdate> updates = {
        taskCompanyUpdate(taskData[0]->id(), 0, 1),
        taskCompanyUpdate(taskData[1]->id(), requests[2].id(), 0)
    };

    for (const auto& update : updates) {
        maps::wiki::query_builder::InsertQuery query("tasker.task_company_update");
        formatModifyQuery(update, query);
        query.exec(*tx);
    }

    dao::TaskAny::fillCompanyUpdates(taskData, *tx);

    for (const auto& company : taskData[0]->companies()) {
        if (company->requests()[0].permalink() == 1) {
            EXPECT_THAT(company->companyUpdate(), NGTest::EqualsProto(updates[0]));
        }
    }

    for (const auto& company : taskData[1]->companies()) {
        if (!company->requests()[0].has_permalink()) {
            EXPECT_THAT(company->companyUpdate(), NGTest::EqualsProto(updates[1]));
        }
    }
}

TEST_F(TaskDaoTest, FillStatistics) {
    auto tx = createTransaction();

    std::vector<proto::Request> requests1 = {createRequest(1, *tx), createRequest(2, *tx), createRequest(1, *tx)};

    long expectedSize1 = 0;
    std::string currentComment = "*";
    for (auto& request : requests1) {
        currentComment += currentComment;
        request.mutable_actualization()->set_comment(TString(currentComment));
        expectedSize1 += request.actualization().ByteSizeLong();
        dao::Request::update(request, *tx);
    }

    proto::Request request2 = createRequest(10, *tx);

    long expectedSize2 = 0;
    request2.mutable_actualization()->set_comment("qweasdasdsdqweasdqwe");
    request2.mutable_actualization()->set_permalink(10000);
    expectedSize2 = request2.actualization().ByteSizeLong();
    dao::Request::update(request2, *tx);

    std::vector<task::TaskDataPtr> taskData;
    taskData.push_back(dao::TaskActive::createFromRequests(requests1, "actualization", *tx));
    taskData.push_back(dao::TaskActive::createFromRequests({request2}, "actualization", *tx));

    dao::TaskActive::fillStatistics(taskData, *tx);

    EXPECT_EQ(taskData[0]->statistics().content_size(), expectedSize1);
    EXPECT_EQ(taskData[0]->statistics().number_of_companies(), 2);

    EXPECT_EQ(taskData[1]->statistics().content_size(), expectedSize2);
    EXPECT_EQ(taskData[1]->statistics().number_of_companies(), 1);
}

TEST_F(TaskDaoTest, AddRequests) {
    auto tx = createTransaction();

    std::vector<proto::Request> baseRequests = {createRequest(1, *tx), createRequest(2, *tx)};
    std::vector<proto::Request> requestsToAdd = {createRequest(1, *tx), createRequest(2, *tx)};

    task::TaskDataPtr createdTask = dao::TaskActive::createFromRequests(baseRequests, "actualization", *tx);

    dao::TaskActive::addRequests(createdTask, requestsToAdd, *tx);
    EXPECT_THAT(createdTask->requests(), ::testing::UnorderedPointwise(ProtoEq(), concat({baseRequests, requestsToAdd})));

    task::TaskDataPtr readTask = dao::TaskActive::get(createdTask->id(), *tx).value();
    dao::TaskActive::fillRequests({readTask}, *tx);
    EXPECT_THAT(readTask->requests(), ::testing::UnorderedPointwise(ProtoEq(), createdTask->requests()));
}

TEST_F(TaskDaoTest, DetachRequests) {
    auto tx = createTransaction();

    std::vector<proto::Request> requests = {
        createRequest(1, *tx),
        createRequest(1, *tx),
        createRequest(2, *tx),
        createRequest(3, *tx),
    };

    task::TaskDataPtr task = dao::TaskActive::createFromRequests(requests, "actualization", *tx);

    dao::TaskActive::detachRequests(task->id(), {requests[0].id(), requests[2].id()}, *tx);
    task = dao::TaskAny::get(task->id(), *tx).value();
    dao::TaskAny::fillRequests({task}, *tx);

    EXPECT_THAT(
        task->requests(),
        ::testing::UnorderedPointwise(
            ProtoEq(), {requests[1], requests[3]}
        )
    );
}


TEST_F(TaskDaoTest, MergeTasks) {
    auto tx = createTransaction();
    std::vector<proto::Request> baseTaskRequests = {createRequest(1, *tx), createRequest(2, *tx)};
    std::vector<proto::Request> toJoinTask1Requests = {createRequest(1, *tx), createRequest(2, *tx)};
    std::vector<proto::Request> toJoinTask2Requests = {createRequest(1, *tx), createRequest(2, *tx)};

    task::TaskDataPtr baseTask = dao::TaskActive::createFromRequests(baseTaskRequests, "actualization", *tx);
    std::vector<task::TaskDataPtr> toJoinTasks;
    toJoinTasks.push_back(dao::TaskActive::createFromRequests(toJoinTask1Requests, "actualization", *tx));
    toJoinTasks.push_back(dao::TaskActive::createFromRequests(toJoinTask2Requests, "actualization", *tx));


    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES (" + std::to_string(baseTask->id()) + ", 1, 111)");
    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES (" + std::to_string(toJoinTasks[0]->id()) + ", 1, 222)");
    tx->exec("INSERT INTO tasker.task_permalink_commit VALUES (" + std::to_string(toJoinTasks[1]->id()) + ", 2, 333)");

    std::vector<proto::TaskCompanyUpdate> updates = {
        taskCompanyUpdate(toJoinTasks[0]->id(), 0, 1),
        taskCompanyUpdate(toJoinTasks[1]->id(), 0, 2),
    };

    for (const auto& update : updates) {
        maps::wiki::query_builder::InsertQuery query("tasker.task_company_update");
        formatModifyQuery(update, query);
        query.exec(*tx);
    }

    dao::TaskActive::mergeTasks(toJoinTasks, baseTask, *tx);

    task::TaskDataPtr readTask = dao::TaskActive::get(baseTask->id(), *tx).value();
    dao::TaskActive::fillRequests({readTask}, *tx);
    dao::TaskActive::fillPermalinkCommits({readTask}, *tx);
    dao::TaskActive::fillCompanyUpdates({readTask}, *tx);
    EXPECT_THAT(
        readTask->requests(),
        ::testing::UnorderedPointwise(
            ProtoEq(),
            concat({baseTaskRequests, toJoinTask1Requests, toJoinTask2Requests})
        )
    );

    std::vector<proto::TaskPermalinkCommit> expectedPermalinkCommits;
    {
        auto& permalinkCommit = expectedPermalinkCommits.emplace_back();
        permalinkCommit.set_task_id(baseTask->id());
        permalinkCommit.set_permalink(1);
        permalinkCommit.set_commit_id(111);
    }
    {
        auto& permalinkCommit = expectedPermalinkCommits.emplace_back();
        permalinkCommit.set_task_id(baseTask->id());
        permalinkCommit.set_permalink(2);
        permalinkCommit.set_commit_id(333);
    }

    EXPECT_THAT(
        readTask->permalinkCommits(),
        ::testing::UnorderedPointwise(ProtoEq(), expectedPermalinkCommits)
    );

    EXPECT_THAT(
        readTask->permalinkCompanies().at(1).companyUpdate(),
        ::NGTest::EqualsProto(taskCompanyUpdate(readTask->id(), 0, 1))
    );

    EXPECT_THAT(
        readTask->permalinkCompanies().at(2).companyUpdate(),
        ::NGTest::EqualsProto(taskCompanyUpdate(readTask->id(), 0, 2))
    );

    for (auto task : toJoinTasks) {
        EXPECT_EQ(
            tx->exec("SELECT * FROM tasker.task_permalink_commit WHERE task_id = " + std::to_string(task->id())).size(),
            0u
        );

        EXPECT_EQ(
            tx->exec("SELECT * FROM tasker.task_company_update WHERE task_id = " + std::to_string(task->id())).size(),
            0u
        );
    }
}

TEST_F(TaskDaoTest, tasksWithPermalink) {
    auto tx = createTransaction();
    std::vector<proto::Request> task1Requests = {createRequest(1, *tx), createRequest(2, *tx)};
    std::vector<proto::Request> task2Requests = {createRequest(1, *tx), createRequest(4, *tx)};
    std::vector<proto::Request> task3Requests = {createRequest(4, *tx), createRequest(2, *tx)};
    std::vector<proto::Request> task4Requests = {createRequest(4, *tx), createRequest(2, *tx)};

    task::TaskDataPtr task1 = dao::TaskActive::createFromRequests(task1Requests, "actualization", *tx);
    task::TaskDataPtr task2 = dao::TaskActive::createFromRequests(task2Requests, "actualization", *tx);
    task::TaskDataPtr task3 = dao::TaskActive::createFromRequests(task3Requests, "actualization", *tx);
    task::TaskDataPtr task4 = dao::TaskActive::createFromRequests(task3Requests, "feedback", *tx);

    proto::Request baseRequest;
    baseRequest.set_permalink(1);
    auto foundTasks = dao::TaskActive::tasksWithPermalink(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task1->id(), task2->id()}));

    foundTasks = dao::TaskActive::tasksWithPermalink(baseRequest, "actualization", 1, *tx);
    EXPECT_EQ(foundTasks.size(), 1u);
    EXPECT_THAT(taskIds(foundTasks), ::testing::IsSubsetOf({task1->id(), task2->id()}));

    baseRequest.set_permalink(2);
    foundTasks = dao::TaskActive::tasksWithPermalink(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task1->id(), task3->id()}));

    baseRequest.set_permalink(4);
    foundTasks = dao::TaskActive::tasksWithPermalink(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task2->id(), task3->id()}));

    baseRequest.set_permalink(999);
    foundTasks = dao::TaskActive::tasksWithPermalink(baseRequest, "actualization", 100, *tx);
    EXPECT_TRUE(foundTasks.empty());
}

TEST_F(TaskDaoTest, tasksWithPhones) {
    auto tx = createTransaction();
    std::vector<proto::Request> task1Requests = {
        createRequestWithPhones({"1"}, *tx),
        createRequestWithPhones({"2", "3"}, *tx)
    };
    std::vector<proto::Request> task2Requests = {
        createRequestWithPhones({"2"}, *tx),
        createRequestWithPhones({"4"}, *tx)
    };
    std::vector<proto::Request> task3Requests = {
        createRequestWithPhones({"1", "2", "3"}, *tx)
    };
    std::vector<proto::Request> task4Requests = {
        createRequestWithPhones({"1", "2", "3"}, *tx)
    };
    std::vector<proto::Request> task5Requests = {
        createRequestWithPhones({"1", "2", "3"}, *tx)
    };
    std::vector<proto::Request> task6Requests = {
        createRequestWithPhones({"1", "2", "3"}, *tx)
    };
    std::vector<proto::Request> task7Requests = {
        createRequestWithPhones({"1", "2", "3"}, *tx)
    };

    task::TaskDataPtr task1 = dao::TaskActive::createFromRequests(task1Requests, "actualization", *tx);
    task::TaskDataPtr task2 = dao::TaskActive::createFromRequests(task2Requests, "actualization", *tx);
    task::TaskDataPtr task3 = dao::TaskActive::createFromRequests(task3Requests, "actualization", *tx);
    // bad type
    task::TaskDataPtr task4 = dao::TaskActive::createFromRequests(task4Requests, "feedback", *tx);

    // bad offset
    task5Requests[0].set_tz_offset(1);
    dao::Request::update(task5Requests[0], *tx);
    task::TaskDataPtr task5 = dao::TaskActive::createFromRequests(task5Requests, "actualization", *tx);

    // is chain
    task6Requests[0].set_belongs_to_chain(true);
    dao::Request::update(task6Requests[0], *tx);
    task::TaskDataPtr task6 = dao::TaskActive::createFromRequests(task6Requests, "actualization", *tx);

    // bad offset is chain
    task7Requests[0].set_belongs_to_chain(true);
    task7Requests[0].set_tz_offset(1);
    dao::Request::update(task7Requests[0], *tx);
    task::TaskDataPtr task7 = dao::TaskActive::createFromRequests(task7Requests, "actualization", *tx);

    proto::Request baseRequest;
    baseRequest.mutable_related_phones()->Add("1");
    auto foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(
        taskIds(foundTasks),
        ::testing::UnorderedElementsAreArray({task1->id(), task3->id(), task6->id(), task7->id()})
    );

    foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 1, *tx);
    EXPECT_EQ(foundTasks.size(), 1u);
    EXPECT_THAT(taskIds(foundTasks), ::testing::IsSubsetOf({task1->id(), task3->id(), task6->id()}));

    baseRequest.mutable_related_phones()->Add("2");
    foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(
        taskIds(foundTasks),
        ::testing::UnorderedElementsAreArray({task1->id(), task2->id(), task3->id(), task6->id(), task7->id()})
    );

    baseRequest.mutable_related_phones()->Clear();
    baseRequest.mutable_related_phones()->Add("4");
    baseRequest.mutable_related_phones()->Add("9");
    baseRequest.mutable_related_phones()->Add("99");
    baseRequest.mutable_related_phones()->Add("999");
    foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task2->id()}));

    baseRequest.mutable_related_phones()->Clear();
    foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 100, *tx);
    EXPECT_TRUE(foundTasks.empty());

    baseRequest.mutable_related_phones()->Add("9");
    baseRequest.mutable_related_phones()->Add("99");
    baseRequest.mutable_related_phones()->Add("999");
    foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 100, *tx);
    EXPECT_TRUE(foundTasks.empty());

    baseRequest.mutable_related_phones()->Clear();
    baseRequest.mutable_related_phones()->Add("1");
    baseRequest.set_belongs_to_chain(true);
    foundTasks = dao::TaskActive::tasksWithPhones(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(
        taskIds(foundTasks),
        ::testing::UnorderedElementsAreArray({task1->id(), task3->id(), task5->id(), task6->id(), task7->id()})
    );
}

TEST_F(TaskDaoTest, tasksWithUserId) {
    auto tx = createTransaction();
    std::vector<proto::Request> task1Requests = {createRequestWithUserId(1, *tx), createRequestWithUserId(2, *tx)};
    std::vector<proto::Request> task2Requests = {createRequestWithUserId(1, *tx), createRequestWithUserId(4, *tx)};
    std::vector<proto::Request> task3Requests = {createRequestWithUserId(4, *tx), createRequestWithUserId(2, *tx)};
    std::vector<proto::Request> task4Requests = {createRequestWithUserId(4, *tx), createRequestWithUserId(2, *tx)};

    task::TaskDataPtr task1 = dao::TaskActive::createFromRequests(task1Requests, "actualization", *tx);
    task::TaskDataPtr task2 = dao::TaskActive::createFromRequests(task2Requests, "actualization", *tx);
    task::TaskDataPtr task3 = dao::TaskActive::createFromRequests(task3Requests, "actualization", *tx);
    task::TaskDataPtr task4 = dao::TaskActive::createFromRequests(task3Requests, "feedback", *tx);

    proto::Request baseRequest;
    baseRequest.set_user_id(1);
    auto foundTasks = dao::TaskActive::tasksWithUserId(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task1->id(), task2->id()}));

    foundTasks = dao::TaskActive::tasksWithUserId(baseRequest, "actualization", 1, *tx);
    EXPECT_EQ(foundTasks.size(), 1u);
    EXPECT_THAT(taskIds(foundTasks), ::testing::IsSubsetOf({task1->id(), task2->id()}));

    baseRequest.set_user_id(2);
    foundTasks = dao::TaskActive::tasksWithUserId(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task1->id(), task3->id()}));

    baseRequest.set_user_id(4);
    foundTasks = dao::TaskActive::tasksWithUserId(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task2->id(), task3->id()}));

    baseRequest.set_user_id(999);
    foundTasks = dao::TaskActive::tasksWithUserId(baseRequest, "actualization", 100, *tx);
    EXPECT_TRUE(foundTasks.empty());
}

TEST_F(TaskDaoTest, tasksWithChain) {
    auto tx = createTransaction();
    std::vector<proto::Request> task1Requests = {createRequestWithChain(1, *tx), createRequestWithChain(2, *tx)};
    std::vector<proto::Request> task2Requests = {createRequestWithChain(1, *tx), createRequestWithChain(4, *tx)};
    std::vector<proto::Request> task3Requests = {createRequestWithChain(4, *tx), createRequestWithChain(2, *tx)};
    std::vector<proto::Request> task4Requests = {createRequestWithChain(4, *tx), createRequestWithChain(2, *tx)};

    task::TaskDataPtr task1 = dao::TaskActive::createFromRequests(task1Requests, "actualization", *tx);
    task::TaskDataPtr task2 = dao::TaskActive::createFromRequests(task2Requests, "actualization", *tx);
    task::TaskDataPtr task3 = dao::TaskActive::createFromRequests(task3Requests, "actualization", *tx);
    task::TaskDataPtr task4 = dao::TaskActive::createFromRequests(task3Requests, "feedback", *tx);

    proto::Request baseRequest;
    baseRequest.set_chain_id(1);
    auto foundTasks = dao::TaskActive::tasksWithChain(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task1->id(), task2->id()}));

    foundTasks = dao::TaskActive::tasksWithChain(baseRequest, "actualization", 1, *tx);
    EXPECT_EQ(foundTasks.size(), 1u);
    EXPECT_THAT(taskIds(foundTasks), ::testing::IsSubsetOf({task1->id(), task2->id()}));

    baseRequest.set_chain_id(2);
    foundTasks = dao::TaskActive::tasksWithChain(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task1->id(), task3->id()}));

    baseRequest.set_chain_id(4);
    foundTasks = dao::TaskActive::tasksWithChain(baseRequest, "actualization", 100, *tx);
    EXPECT_THAT(taskIds(foundTasks), ::testing::UnorderedElementsAreArray({task2->id(), task3->id()}));

    baseRequest.set_chain_id(999);
    foundTasks = dao::TaskActive::tasksWithChain(baseRequest, "actualization", 100, *tx);
    EXPECT_TRUE(foundTasks.empty());
}

} // maps::sprav::callcenter::tests
