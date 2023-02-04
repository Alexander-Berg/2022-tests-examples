#include <maps/infopoint/takeout/lib/tracker.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/folder/tempdir.h>

namespace {

struct InfopointTakeoutTrackerTestFixture : public NUnitTest::TBaseFixture
{
    InfopointTakeoutTrackerTestFixture()
        : tracker(tempDir.Name())
    {}

    TTempDir tempDir;
    maps::infopoint::takeout::JobTracker tracker;
};

} // namespace

Y_UNIT_TEST_SUITE_F(InfopointTakeoutTracker, InfopointTakeoutTrackerTestFixture) {

    Y_UNIT_TEST(TestFileCreation) {
        maps::infopoint::takeout::Job job {"ac", "dc"};
        tracker.jobScheduled(job);
        TVector<TFsPath> children;
        auto trackerDirPath = tempDir.Path();
        trackerDirPath.List(children);

        UNIT_ASSERT_EQUAL(children.size(), 1);
        UNIT_ASSERT_EQUAL(children.front().Basename(), "YWM,_dc");

        children.clear();
        tracker.jobFinished(job);
        trackerDirPath.List(children);

        UNIT_ASSERT_EQUAL(children.size(), 0);
    }

    Y_UNIT_TEST(TestState) {
        UNIT_ASSERT_EQUAL(tracker.getSavedState().size(), 0);

        maps::infopoint::takeout::Job job {"ac", "dc"};
        tracker.jobScheduled(job);

        auto stateWithJob = tracker.getSavedState();
        UNIT_ASSERT_EQUAL(stateWithJob.size(), 1);
        UNIT_ASSERT_EQUAL(stateWithJob.count(job), 1);
        UNIT_ASSERT_EQUAL(*stateWithJob.begin(), job);

        tracker.jobFinished(job);
        auto emptyState = tracker.getSavedState();
        UNIT_ASSERT_EQUAL(emptyState.size(), 0);
    }
}
