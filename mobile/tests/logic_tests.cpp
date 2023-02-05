#include "../interaction_feedback_manager_impl.h"

#include <yandex/maps/navikit/mocks/mock_experiments_manager.h>
#include <yandex/maps/navikit/mocks/mock_guidance.h>
#include <yandex/maps/navikit/mocks/mock_route_manager.h>
#include <yandex/maps/navi/mocks/mock_interaction_feedback_manager.h>
#include <yandex/maps/navi/mocks/mock_settings_manager.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/platform.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::interaction {

namespace {

constexpr auto ONE_MONTH = std::chrono::hours(30 * 24);

}

settings::InteractionFeedbackDataExtended interactionFeedbackDataExtended;

class InteractionFeedbackFixture :
    public settings::MockSettingsManager,
    public navikit::experiments::MockExperimentsManager,
    public navikit::routing::MockRouteManager,
    public navikit::guidance::MockGuidance
{
public:
    InteractionFeedbackFixture() :
        interactionData_(std::make_shared<settings::InteractionFeedbackDataExtended>())
    {
        runtime::async::ui()->spawn(
            [this]
            {
                feedbackManager_ = std::make_shared<InteractionFeedbackManagerImpl>(
                    this,
                    this,
                    this,
                    this,
                    [this]() -> runtime::AbsoluteTimestamp { return now_; }
                );
                feedbackManager_->addListener(listener_);
            }
        ).wait();
    }

    InteractionFeedbackManagerImpl* manager()
    {
        return feedbackManager_.get();
    }

    MockInteractionFeedbackListener& listener()
    {
        return *listener_;
    }

    void setNow(const runtime::AbsoluteTimestamp& timepoint)
    {
        now_ = timepoint;
    }

    void rateMultipleRoadEvents(int count)
    {
        for (int i = 0; i < count; ++i) {
            manager()->onRoadEventRated();
        }
    }

    void checkInitialRating()
    {
        BOOST_CHECK(manager()->status() == PendingAction::Noop);
        EXPECT_CALL(listener(), onStatusChanged()).Times(1);
        rateMultipleRoadEvents(5);
        BOOST_CHECK(manager()->status() == PendingAction::RateInStore);
    }

    void checkAfterUpdate()
    {
        EXPECT_CALL(listener(), onStatusChanged()).Times(0);
        manager()->onNewVersionInstalled();
        BOOST_CHECK(manager()->status() == PendingAction::Noop);

        EXPECT_CALL(listener(), onStatusChanged()).Times(1);
        rateMultipleRoadEvents(5);
        BOOST_CHECK(manager()->status() == PendingAction::RateInStore);
    }

    /**
     * settings::InteractionFeedbackStorage
     */
    virtual const std::shared_ptr<settings::InteractionFeedbackDataExtended>& interactionData()
        const override
    {
        return interactionData_;
    }

    virtual const std::shared_ptr<runtime::bindings::Vector<runtime::AbsoluteTimestamp>>&
        lastCommercialUsages() const override
    {
        return lastCommercialUsages_;
    }

    virtual void saveInteractionData() override {}

    virtual const std::shared_ptr<settings::TutorialDataExtended>& tutorialData() const override
    {
        return tutorialData_;
    }

    virtual void saveTutorialData() override {}

    virtual int routeCompleteCount() const override
    {
        return routeCompleteCount_;
    }

    virtual void setRouteCompleteCount(int value) override
    {
        routeCompleteCount_ = value;
    }

private:
    runtime::AbsoluteTimestamp now_ = runtime::now<runtime::AbsoluteTimestamp>();
    std::shared_ptr<MockInteractionFeedbackListener> listener_ =
        std::make_shared<MockInteractionFeedbackListener>();
    std::shared_ptr<settings::InteractionFeedbackDataExtended> interactionData_;
    std::shared_ptr<InteractionFeedbackManagerImpl> feedbackManager_;
    std::shared_ptr<runtime::bindings::Vector<runtime::AbsoluteTimestamp>> lastCommercialUsages_;
    std::shared_ptr<settings::TutorialDataExtended> tutorialData_;
    int routeCompleteCount_ = 0;
};

BOOST_FIXTURE_TEST_SUITE(InteractionFeedbackTests, InteractionFeedbackFixture)

BOOST_AUTO_TEST_CASE(defaultStatus)
{
    runtime::async::ui()->spawn(
        [&]
        {
            BOOST_CHECK(manager()->status() == PendingAction::Noop);
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(successfullRoutes)
{
    runtime::async::ui()->spawn(
        [&]
        {
            EXPECT_CALL(listener(), onStatusChanged()).Times(0);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);
            manager()->onRouteSuccessfullyCompleted();
            manager()->onRouteSuccessfullyCompleted();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);
            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onRouteSuccessfullyCompleted();
            BOOST_CHECK(manager()->status() == PendingAction::RateInStore);
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(statisticsOpenings)
{
    runtime::async::ui()->spawn(
        [&]
        {
            EXPECT_CALL(listener(), onStatusChanged()).Times(0);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);
            manager()->onRouteStatisticsOpened();
            manager()->onRouteStatisticsOpened();
            manager()->onRouteStatisticsOpened();
            manager()->onRouteStatisticsOpened();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);
            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onRouteStatisticsOpened();
            BOOST_CHECK(manager()->status() == PendingAction::RateInStore);
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(roadEventRatings)
{
    runtime::async::ui()->spawn(
        [&]
        {
            EXPECT_CALL(listener(), onStatusChanged()).Times(0);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            rateMultipleRoadEvents(4);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onRoadEventRated();
            BOOST_CHECK(manager()->status() == PendingAction::RateInStore);
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(likeDialogClose)
{
    runtime::async::ui()->spawn(
        [&]
        {
            checkInitialRating();

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onLikeDialogCloseClicked();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::RateInStore);

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onLikeDialogCloseClicked();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            checkAfterUpdate();
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(rateDialogYes)
{
    runtime::async::ui()->spawn(
        [&]
        {
            checkInitialRating();

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onRateDialogYesClicked();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            manager()->onNewVersionInstalled();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            rateMultipleRoadEvents(5);

#if defined(BUILDING_FOR_ANDROID)
            BOOST_CHECK(manager()->status() == PendingAction::RateLocal);
            setNow(runtime::now<runtime::AbsoluteTimestamp>() + 5 * ONE_MONTH);
            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            rateMultipleRoadEvents(1);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            setNow(runtime::now<runtime::AbsoluteTimestamp>() + 7 * ONE_MONTH);
            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            rateMultipleRoadEvents(5);
#endif
            BOOST_CHECK(manager()->status() == PendingAction::RateInStore);

        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(rateDialogNo)
{
    runtime::async::ui()->spawn(
        [&]
        {
            checkInitialRating();

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onRateDialogNoClicked();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            checkAfterUpdate();
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(negativeDialogYes)
{
    runtime::async::ui()->spawn(
        [&]
        {
            checkInitialRating();

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onNegativeExperienceDialogYesClicked();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            checkAfterUpdate();
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(negativeDialogNo)
{
    runtime::async::ui()->spawn(
        [&]
        {
            checkInitialRating();

            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            manager()->onNegativeExperienceDialogNoClicked();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            manager()->onNewVersionInstalled();
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            setNow(runtime::now<runtime::AbsoluteTimestamp>() + 3 * ONE_MONTH);
            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::Noop);

            setNow(runtime::now<runtime::AbsoluteTimestamp>() + 5 * ONE_MONTH);
            EXPECT_CALL(listener(), onStatusChanged()).Times(1);
            rateMultipleRoadEvents(5);
            BOOST_CHECK(manager()->status() == PendingAction::RateInStore);
        }
    ).wait();
}

BOOST_AUTO_TEST_CASE(gasStations)
{
    runtime::async::ui()->spawn(
        [&]
        {
            const std::string id = "automotive-gas-station";

            BOOST_CHECK(manager()->isStationInteresting(id));
            manager()->onStationInteresting(id, /* interesting = */ false);
            BOOST_CHECK(manager()->isStationInteresting(id));
            manager()->onStationInteresting(id, /* interesting = */ false);
            BOOST_CHECK(manager()->isStationInteresting(id) == false);

            manager()->onStationInteresting(id, /* interesting = */ true);
            BOOST_CHECK(manager()->isStationInteresting(id));
        }
    ).wait();
}

BOOST_AUTO_TEST_SUITE_END()

}
