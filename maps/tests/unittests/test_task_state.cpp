#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/async_executor/include/task.h>
#include <maps/libs/common/include/exception.h>

#include <iostream>

using namespace maps::async_executor;

template<>
void Out<TaskStatus>(IOutputStream& out, TaskStatus taskStatus) {
    out << toString(taskStatus);
}

namespace maps::async_executor::tests {

Y_UNIT_TEST_SUITE(TaskState) {

Y_UNIT_TEST(testTaskState) {

    const auto dc1 = Dc{"dc1"};
    const auto dc2 = Dc{"dc2"};
    const auto dc3 = Dc{"dc3"};
    const auto fake_url = Url{"fake_url"};

    TaskState taskState(/*replicationFactor*/2);

    EXPECT_EQ(taskState.replicationFactor(), 2);
    EXPECT_GT(taskState.creationTime(), std::chrono::system_clock::time_point{});
    EXPECT_EQ(taskState.revision(), 0);

    EXPECT_EQ(taskState.status(), InProgress);
    EXPECT_TRUE(taskState.wantWorker());
    taskState.validate();

    // some prohibited methods now:
    EXPECT_THROW(taskState.setWorkerCompleted(dc1, fake_url), RuntimeError);
    EXPECT_THROW(taskState.setWorkerFailed(dc1, "some message"), RuntimeError);

    taskState.setWorkerInProgress(dc1);
    EXPECT_TRUE(taskState.wantWorker());

    EXPECT_THROW(taskState.setWorkerInProgress(dc1), RuntimeError);

    taskState.setWorkerInProgress(dc2);
    EXPECT_FALSE(taskState.wantWorker());

    EXPECT_THROW(taskState.setWorkerInProgress(dc3), RuntimeError);

    taskState.setWorkerCompleted(dc1, fake_url);
    EXPECT_THROW(taskState.setWorkerCompleted(dc1, fake_url), RuntimeError);
    EXPECT_FALSE(taskState.wantWorker());

    taskState.setWorkerFailed(dc2, "some message");
    EXPECT_THROW(taskState.setWorkerFailed(dc2, "some message"), RuntimeError);
    EXPECT_FALSE(taskState.wantWorker());

    EXPECT_EQ(taskState.status(), Succeeded);
    EXPECT_EQ(taskState.url().value(), fake_url);

    taskState.validate();
    std::cerr << "TaskState: " << toString(taskState) << std::endl;
}

} // Y_UNIT_TEST_SUITE(TaskState)

} // namespace maps::async_executor::tests
