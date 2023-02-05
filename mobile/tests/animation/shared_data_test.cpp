#include <boost/test/unit_test.hpp>

#include <yandex/metrokit/animation/internal/shared_data.h>

namespace yandex {
namespace metrokit {
namespace animation {

using SharedData = internal::SharedData;

BOOST_AUTO_TEST_CASE(after_setInterval__interval_should_return_the_same) {
    SharedData data;
    
    data.setInterval(TimeInterval { 0.012 });
    
    BOOST_CHECK_EQUAL(data.interval().count(), TimeInterval { 0.012 }.count());
}

BOOST_AUTO_TEST_CASE(after_setInterval__onUpdate_callback_should_be_called) {
    SharedData data;
    
    TimeInterval callbackResult { 0.0 };
    
    data.setOnUpdate([&](const auto& interval) {
        callbackResult = interval;
    });
    
    data.setInterval(TimeInterval { 0.012 });
    
    BOOST_CHECK_EQUAL(callbackResult.count(), TimeInterval { 0.012 }.count());
}

BOOST_AUTO_TEST_CASE(after_move__onUpdate_callback_should_be_called) {
    SharedData dataA;
    
    TimeInterval callbackResult { 0.0 };
    
    dataA.setOnUpdate([&](const auto& interval) {
        callbackResult = interval;
    });
    
    auto dataB = std::move(dataA);
    
    dataB.setInterval(TimeInterval { 0.012 });
    
    BOOST_CHECK_EQUAL(callbackResult.count(), TimeInterval { 0.012 }.count());
}

} } }
