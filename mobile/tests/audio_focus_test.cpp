#include "../audio_focus_manager_impl.h"
#include "mocks/mock_system_focus_manager.h"

#include <yandex/maps/navikit/mocks/mock_experiments_manager.h>
#include <yandex/maps/navi/audio/audio_session_controller_factory.h>
#include <yandex/maps/navi/audio_focus/audio_focus.h>
#include <yandex/maps/navi/audio_focus/audio_focus_manager_factory.h>
#include <yandex/maps/navi/mocks/mock_settings_manager.h>
#include <yandex/maps/navi/myspin/myspin_manager_creator.h>

#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::audio_focus::tests {

namespace {
class AudioFocusTestFixture {
public:
    AudioFocusTestFixture();

    std::unique_ptr<AudioFocus> requestFocus(AudioFocusType type);

protected:
    std::shared_ptr<MockSystemFocusManager> systemFocusManager_;

private:
    void setUp();

    std::shared_ptr<myspin::ExtendedMySpinManager> mySpinManager_;
    std::shared_ptr<AudioFocusManager> manager_;
    std::unique_ptr<navikit::experiments::MockExperimentsManager> experimentsManager_;
};

AudioFocusTestFixture::AudioFocusTestFixture()
{
    runtime::async::runOnUiThread([this] {
        setUp();
    });
}

void AudioFocusTestFixture::setUp()
{
    experimentsManager_ = std::make_unique<navikit::experiments::MockExperimentsManager>();
    systemFocusManager_ = std::make_shared<MockSystemFocusManager>();
    mySpinManager_ = myspin::createMySpinManager(nullptr);
    manager_ = createAudioFocusManager(mySpinManager_.get(),
        systemFocusManager_,
        experimentsManager_.get());
}

std::unique_ptr<AudioFocus> AudioFocusTestFixture::requestFocus(AudioFocusType type)
{
    auto focus = manager_->requestFocus(type);
    BOOST_CHECK(focus->type() == type);
    return focus;
}

}  // unnamed namespace

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Test suite
//

BOOST_FIXTURE_TEST_SUITE(AudioFocusTests, AudioFocusTestFixture)

BOOST_AUTO_TEST_CASE(testFocusOpened)
{
    runtime::async::runOnUiThread([&] {
        auto focus = requestFocus(AudioFocusType::Alert);
        BOOST_CHECK(focus->state() == AudioFocusState::Opened);
    });

    runtime::async::runOnUiThread([&] {
        auto focus = requestFocus(AudioFocusType::Annotation);
        BOOST_CHECK(focus->state() == AudioFocusState::Opened);
    });

    runtime::async::runOnUiThread([&] {
        auto focus = requestFocus(AudioFocusType::Music);
        BOOST_CHECK(focus->state() == AudioFocusState::Opened);
    });
}

BOOST_AUTO_TEST_CASE(testAlertAndMusicFocus)
{
    runtime::async::runOnUiThread([&] {
        auto alertFocus = requestFocus(AudioFocusType::Alert);
        auto musicFocus = requestFocus(AudioFocusType::Music);
        BOOST_CHECK(musicFocus->state() == AudioFocusState::Opened);
    });
}

BOOST_AUTO_TEST_CASE(testAlertAndAnnotationFocus)
{
    runtime::async::runOnUiThread([&] {
        auto alertFocus = requestFocus(AudioFocusType::Alert);
        auto annotationFocus = requestFocus(AudioFocusType::Annotation);
        BOOST_CHECK(alertFocus->state() == AudioFocusState::Opened);
        BOOST_CHECK(annotationFocus->state() == AudioFocusState::Opened);
    });
}

BOOST_AUTO_TEST_CASE(testAnnotationAndMusicFocus)
{
    runtime::async::runOnUiThread([&] {
        auto annotationFocus = requestFocus(AudioFocusType::Annotation);
        auto musicFocus = requestFocus(AudioFocusType::Music);
        BOOST_CHECK(musicFocus->state() == AudioFocusState::ShouldDuck);
        annotationFocus.reset();
        BOOST_CHECK(musicFocus->state() == AudioFocusState::Opened);
    });
}

BOOST_AUTO_TEST_CASE(testSecondAcquireAnnotationFocus)
{
    runtime::async::runOnUiThread([&] {
        auto first = requestFocus(AudioFocusType::Annotation);
        auto second = requestFocus(AudioFocusType::Annotation);
        auto music = requestFocus(AudioFocusType::Music);
        BOOST_CHECK(first->state() == AudioFocusState::Opened);
        BOOST_CHECK(second->state() == AudioFocusState::Opened);
        BOOST_CHECK(music->state() == AudioFocusState::ShouldDuck);

        first.reset();
        BOOST_CHECK(second->state() == AudioFocusState::Opened);
        BOOST_CHECK(music->state() == AudioFocusState::ShouldDuck);

        second.reset();
        BOOST_CHECK(music->state() == AudioFocusState::Opened);
    });
}

BOOST_AUTO_TEST_CASE(testMusicInterrupted)
{
    runtime::async::runOnUiThread([&] {
        auto music = requestFocus(AudioFocusType::Music);
        BOOST_CHECK(music->state() == AudioFocusState::Opened);

        systemFocusManager_->setSystemState(MockSystemFocusManager::StateType::Play,
            SystemFocusManager::State::Suspended);
        BOOST_CHECK(music->state() == AudioFocusState::Suspended);

        systemFocusManager_->setSystemState(MockSystemFocusManager::StateType::Play,
            SystemFocusManager::State::Activated);
        BOOST_CHECK(music->state() == AudioFocusState::Opened);

        systemFocusManager_->setSystemState(MockSystemFocusManager::StateType::Play,
            SystemFocusManager::State::Lost);
        BOOST_CHECK(music->state() == AudioFocusState::Closed);
    });
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex
