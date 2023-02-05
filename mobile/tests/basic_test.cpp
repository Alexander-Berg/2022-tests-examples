#include <yandex/maps/navikit/tts/tts_player.h>
#include <yandex/maps/navi/tts/tts_factory.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/async/promise.h>
#include <yandex/maps/runtime/logging/logging.h>
#include <yandex/maps/runtime/platform_holder.h>

#include <boost/test/unit_test.hpp>

#include <chrono>
#include <string>

namespace yandex {
namespace maps {
namespace navi {
namespace tts {

using namespace ::yandex::maps::runtime;

namespace {

#define UI(expr) async::ui()->spawn([&] { expr; }).wait()

struct TestListener : public TtsListener {
    virtual void onUtteranceFinished() override
    {
        finishedPromise.setValue(true);
    }

    async::Promise<bool> finishedPromise;
};

struct Fixture {
    Fixture()
    {
        UI(
            ttsPlayer = createTtsPlayer();
            ttsPlayer->setListener(listener.get());
            ttsPlayer->setLanguage("ru");
        );
        while (!isAvailable()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }

    bool isAvailable()
    {
        return async::ui()->async([&] {
            return ttsPlayer->isAvailable();
        }).get();
    }

    void say(const std::string& text)
    {
        UI( ttsPlayer->say(text) );
    }

    void stop()
    {
        UI( ttsPlayer->stop() );
    }

    bool isLanguageSupported(const std::string& language)
    {
        return async::ui()->async([&] {
            return ttsPlayer->isLanguageSupported(language);
        }).get();
    }

    int duration(const std::string& text)
    {
        return async::ui()->async([&] {
            return ttsPlayer->duration(text);
        }).get();
    }

    void setLanguage(const std::string& language)
    {
        UI( ttsPlayer->setLanguage(language) );
    }

    std::unique_ptr<TtsPlayer> ttsPlayer;
    std::shared_ptr<TestListener> listener = makeWeakPlatformObject<TestListener>();
};

bool waitForListener(
        const std::shared_ptr<TestListener>& listener,
        size_t msecToWait = 100)
{
    const auto status = listener->finishedPromise.future().waitFor(
        std::chrono::milliseconds(msecToWait)
    );
    return status == async::FutureStatus::Ready;
}

} // namespace

BOOST_FIXTURE_TEST_SUITE(TtsPlayerTests, Fixture)

BOOST_AUTO_TEST_CASE(compilationTest)
{
    say("Через 300 метров поворот направо");

    BOOST_CHECK(
        listener->finishedPromise.future().get()
    );
}

BOOST_AUTO_TEST_CASE(stop_whenListenerIsPresent_callsOnUtteranceFinished)
{
    say("Через 300 метров поворот направо");
    // On Android immediate stop after say just removes the phrase from the queue,
    // listener is not called.
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    stop();

    BOOST_CHECK(waitForListener(listener));
}

BOOST_AUTO_TEST_CASE(duration_givenSomePhrase_returnsSaneValue)
{
    const auto result = duration("в этой фразе больше четырёх слов");

    // even for 250 wpm reading speed, it should be greater than one second
    BOOST_CHECK(result > 1000);
    // even for 100 wpm reading speed, it should be less than five seconds
    BOOST_CHECK(result < 5000);
}

BOOST_AUTO_TEST_CASE(isLanguageSupported_givenEnglish_returnsTrue)
{
    // This test assumes that english is available in offline for every
    // TTS-engine on Android and iOS
    BOOST_CHECK(isLanguageSupported("en"));
}

BOOST_AUTO_TEST_CASE(isLanguageSupported_givenInvaildLanguage_returnsFalse)
{
    BOOST_CHECK(!isLanguageSupported("ololo"));
    BOOST_CHECK(!isLanguageSupported("42"));
}

BOOST_AUTO_TEST_CASE(isAvailable_afterSettingInvalidLanguage_returnsFalse)
{
    BOOST_CHECK(isAvailable());

    setLanguage("ololo");

    BOOST_CHECK(!isAvailable());
}

BOOST_AUTO_TEST_CASE(say_whenPlayerIsNotAvailable_isIgnored)
{
    setLanguage("ololo");
    BOOST_REQUIRE(!isAvailable());

    say("тест");

    BOOST_CHECK(!waitForListener(listener));
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tts
} // namespace navikit
} // namespace maps
} // namespace yandex
