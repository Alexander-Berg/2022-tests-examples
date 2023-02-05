#include <yandex/metrokit/animation/animation.h>
#include <yandex/metrokit/animation/arithmetic+animatable.h>
#include <yandex/metrokit/animation/info.h>

#include <boost/test/unit_test.hpp>

namespace yandex {
namespace metrokit {
namespace animation {

namespace {

const auto DEFAULT_TIME_INTERVAL = TimeInterval { 0.3 };
const auto DEFAULT_LINEAR_INFO = Info { DEFAULT_TIME_INTERVAL, timing_function::linear };

auto getRelativeTimestamp(const TimeInterval& interval) -> TimePoint {
    return TimePoint() + interval;
}

const auto BEGIN_TIMESTAMP = getRelativeTimestamp(TimeInterval { 0.0 });
const auto MID_TIMESTAMP = getRelativeTimestamp(DEFAULT_TIME_INTERVAL / 2.0);
const auto END_TIMESTAMP = getRelativeTimestamp(DEFAULT_TIME_INTERVAL);

} // namespace

BOOST_AUTO_TEST_CASE(after_setBeginTimestamp__animation_state_should_not_change) {
    auto ret = Animation<float>::make(7.0f, 12.0f, DEFAULT_LINEAR_INFO);
    auto animation = std::move(ret.first);
    auto updater = std::move(ret.second);
    
    updater.setBeginTimestamp(BEGIN_TIMESTAMP);
    
    BOOST_CHECK_EQUAL(animation.currentState(), 7.0f);
    BOOST_CHECK_EQUAL(animation.progress(), 0.0f);
    BOOST_CHECK_EQUAL(animation.isDone(), false);
}

BOOST_AUTO_TEST_CASE(after_setting_mid_timestamp__animation_state_should_be_middle) {
    auto ret = Animation<float>::make(7.0f, 12.0f, DEFAULT_LINEAR_INFO);
    auto animation = std::move(ret.first);
    auto updater = std::move(ret.second);
    
    updater.setBeginTimestamp(BEGIN_TIMESTAMP);
    updater.setCurrentTimestamp(MID_TIMESTAMP);
    
    BOOST_CHECK_EQUAL(animation.currentState(), 9.5f);
    BOOST_CHECK_EQUAL(animation.progress(), 0.5f);
    BOOST_CHECK_EQUAL(animation.isDone(), false);
}

BOOST_AUTO_TEST_CASE(after_setting_end_timestamp__animation_state_should_be_final) {
    auto ret = Animation<float>::make(7.0f, 12.0f, DEFAULT_LINEAR_INFO);
    auto animation = std::move(ret.first);
    auto updater = std::move(ret.second);
    
    updater.setBeginTimestamp(BEGIN_TIMESTAMP);
    updater.setCurrentTimestamp(END_TIMESTAMP);
    
    BOOST_CHECK_EQUAL(animation.currentState(), 12.0f);
    BOOST_CHECK_EQUAL(animation.progress(), 1.0f);
    BOOST_CHECK_EQUAL(animation.isDone(), true);
}

BOOST_AUTO_TEST_CASE(after_setting_mid_timestamp__animation_callback_state_should_be_middle) {
    auto ret = Animation<float>::make(7.0f, 12.0f, DEFAULT_LINEAR_INFO);
    auto animation = std::move(ret.first);
    auto updater = std::move(ret.second);
    
    float callbackState;
    float callbackProgress;
    bool callbackIsDone;
    
    animation.setOnUpdate([&](const auto& state, auto progress, auto isDone) {
        callbackState = state;
        callbackProgress = progress;
        callbackIsDone = isDone;
    });
    
    updater.setBeginTimestamp(BEGIN_TIMESTAMP);
    updater.setCurrentTimestamp(MID_TIMESTAMP);
    
    BOOST_CHECK_EQUAL(callbackState, 9.5f);
    BOOST_CHECK_EQUAL(callbackProgress, 0.5f);
    BOOST_CHECK_EQUAL(callbackIsDone, false);
}

BOOST_AUTO_TEST_CASE(after_movement__animation_callback_should_be_called_once) {
    auto ret = Animation<float>::make(7.0f, 12.0f, DEFAULT_LINEAR_INFO);
    auto animation = std::move(ret.first);
    auto updater = std::move(ret.second);
    
    float callbackState = 0.0f;
    float callbackProgress = 0.0f;
    bool callbackIsDone = true;
    int onUpdateCount = 0;
    
    animation.setOnUpdate([&](const auto& state, auto progress, auto isDone) {
        callbackState = state;
        callbackProgress = progress;
        callbackIsDone = isDone;
        onUpdateCount++;
    });
    
    updater.setBeginTimestamp(BEGIN_TIMESTAMP);
    
    auto animation2 = std::move(animation);
    
    updater.setCurrentTimestamp(MID_TIMESTAMP);
    
    BOOST_CHECK_EQUAL(callbackState, 9.5f);
    BOOST_CHECK_EQUAL(callbackProgress, 0.5f);
    BOOST_CHECK_EQUAL(callbackIsDone, false);
    BOOST_CHECK_EQUAL(onUpdateCount, 1);
}

BOOST_AUTO_TEST_CASE(animation_done_callback_should_be_called_once) {
    auto ret = Animation<float>::make(7.0f, 12.0f, DEFAULT_LINEAR_INFO);
    auto animation = std::move(ret.first);
    auto updater = std::move(ret.second);
    
    int onUpdateCount = 0;
    
    animation.setOnUpdate([&](const auto&, auto, auto) {
        onUpdateCount++;
    });
    
    updater.setBeginTimestamp(BEGIN_TIMESTAMP);
    updater.setCurrentTimestamp(END_TIMESTAMP);
    updater.setCurrentTimestamp(END_TIMESTAMP);
    updater.setCurrentTimestamp(END_TIMESTAMP);
    
    BOOST_CHECK_EQUAL(onUpdateCount, 1);
}

} } }

