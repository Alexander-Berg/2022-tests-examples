#include "labeler/text_chooser.h"
#include "core/feature.h"
#include <maps/renderer/libs/base/include/string_convert.h>
#include <boost/test/unit_test.hpp>

using namespace maps::renderer::base;

namespace maps {
namespace renderer5 {
namespace labeler {

namespace {
std::pair<std::string, std::string> chooseTexts(
    const std::string& locale,
    const std::vector<std::string>& texts)
{
    renderer::feature::Feature f(core::FeatureType::Text);
    f.add(core::CapabilityFeatureText);
    for (const auto& text : texts)
        f.localizedTexts().insert(std::make_pair(s2ws(text), s2ws(text)));

    auto result = TextChooser(locale).texts(f);
    return std::make_pair(ws2s(result.first), ws2s(result.second));
}

std::string toStr(const std::pair<std::string, std::string>& texts)
{
    return texts.first + "|" + texts.second;
}
} // namespace

BOOST_AUTO_TEST_SUITE(labeler)

BOOST_AUTO_TEST_CASE(text_chooser)
{
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", {})), "|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("", { "ru" })), "|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", { "ru" })), "ru|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", { "ru" })), "ru|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", { "ru", "en" })), "en|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", { "ru_LOCAL", "en" })), "en|ru_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", { "ru_LOCAL", "en" })), "ru_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", { "ru_LOCAL", "ru-Latn_LOCAL" })), "ru_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", { "ru_LOCAL", "ru-Latn_LOCAL" })), "ru-Latn_LOCAL|ru_LOCAL");

    // https://wiki.yandex-team.ru/maps/dev/core/wikimap/mapspro/krm/locales/

    // Chuvash
    std::vector<std::string> Cheboksary = { "cv_LOCAL", "ru_LOCAL", "en", "tr" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", Cheboksary)), "ru_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", Cheboksary)), "en|ru_LOCAL");
    BOOST_CHECK_EQUAL(chooseTexts("uk_UA", Cheboksary).first, "ru_LOCAL");
    BOOST_CHECK_EQUAL(chooseTexts("ru_UA", Cheboksary).first, "ru_LOCAL");
    BOOST_CHECK_EQUAL(chooseTexts("en_UA", Cheboksary).first, "en");
    BOOST_CHECK_EQUAL(chooseTexts("tr_TR", Cheboksary).first, "tr");
    BOOST_CHECK_EQUAL(chooseTexts("en_TR", Cheboksary).first, "en");
    BOOST_CHECK_EQUAL(chooseTexts("en_US", Cheboksary).first, "en");
    std::vector<std::string> CheboksaryRU = { "cv_RU_LOCAL", "ru_RU_LOCAL", "en_RU", "tr_RU" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", CheboksaryRU)), "ru_RU_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", CheboksaryRU)), "en_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("uk_UA", CheboksaryRU)), "ru_RU_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_UA", CheboksaryRU)), "ru_RU_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_UA", CheboksaryRU)), "en_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("tr_TR", CheboksaryRU)), "tr_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_TR", CheboksaryRU)), "en_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", CheboksaryRU)), "en_RU|ru_RU_LOCAL");

    // Crimea
    std::vector<std::string> Zheleznodorozhnoe = { "ru_LOCAL", "uk_LOCAL", "en", "tr" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", Zheleznodorozhnoe)), "ru_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", Zheleznodorozhnoe)), "en|ru_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("uk_UA", Zheleznodorozhnoe)), "uk_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_UA", Zheleznodorozhnoe)), "ru_LOCAL|uk_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_UA", Zheleznodorozhnoe)), "en|uk_LOCAL");
    BOOST_CHECK_EQUAL(chooseTexts("tr_TR", Zheleznodorozhnoe).first, "tr");
    BOOST_CHECK_EQUAL(chooseTexts("en_TR", Zheleznodorozhnoe).first, "en");
    BOOST_CHECK_EQUAL(chooseTexts("en_US", Zheleznodorozhnoe).first, "en");
    std::vector<std::string> ZheleznodorozhnoeRU = { "ru_RU_LOCAL", "uk_RU_LOCAL", "en_RU", "tr_RU" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", ZheleznodorozhnoeRU)), "ru_RU_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", ZheleznodorozhnoeRU)), "en_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("uk_UA", ZheleznodorozhnoeRU)), "uk_RU_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_UA", ZheleznodorozhnoeRU)), "ru_RU_LOCAL|uk_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_UA", ZheleznodorozhnoeRU)), "en_RU|uk_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("tr_TR", ZheleznodorozhnoeRU)), "tr_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_TR", ZheleznodorozhnoeRU)), "en_RU|ru_RU_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", ZheleznodorozhnoeRU)), "en_RU|ru_RU_LOCAL");
    std::vector<std::string> ZheleznodorozhnoeUA = { "ru_UA_LOCAL", "uk_UA_LOCAL", "en_UA", "tr_UA" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", ZheleznodorozhnoeUA)), "ru_UA_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", ZheleznodorozhnoeUA)), "en_UA|ru_UA_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("uk_UA", ZheleznodorozhnoeUA)), "uk_UA_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_UA", ZheleznodorozhnoeUA)), "ru_UA_LOCAL|uk_UA_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_UA", ZheleznodorozhnoeUA)), "en_UA|uk_UA_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("tr_TR", ZheleznodorozhnoeUA)), "tr_UA|uk_UA_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_TR", ZheleznodorozhnoeUA)), "en_UA|uk_UA_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", ZheleznodorozhnoeUA)), "en_UA|uk_UA_LOCAL");

    // Belarus
    std::vector<std::string> Bobruisk = { "be_LOCAL", "ru_LOCAL", "uk", "en", "tr" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", Bobruisk)), "ru_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", Bobruisk)), "en|ru_LOCAL");
    BOOST_CHECK_EQUAL(chooseTexts("uk_UA", Bobruisk).first, "uk");
    BOOST_CHECK_EQUAL(chooseTexts("ru_UA", Bobruisk).first, "ru_LOCAL");
    BOOST_CHECK_EQUAL(chooseTexts("en_UA", Bobruisk).first, "en");
    BOOST_CHECK_EQUAL(chooseTexts("tr_TR", Bobruisk).first, "tr");
    BOOST_CHECK_EQUAL(chooseTexts("en_TR", Bobruisk).first, "en");
    BOOST_CHECK_EQUAL(chooseTexts("en_US", Bobruisk).first, "en");
    std::vector<std::string> BobruiskBY = { "be_BY_LOCAL", "ru_BY_LOCAL", "uk_BY", "en_BY", "tr_BY" };
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_RU", BobruiskBY)), "ru_BY_LOCAL|");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_RU", BobruiskBY)), "en_BY|ru_BY_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("uk_UA", BobruiskBY)), "uk_BY|be_BY_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("ru_UA", BobruiskBY)), "ru_BY_LOCAL|be_BY_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_UA", BobruiskBY)), "en_BY|be_BY_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("tr_TR", BobruiskBY)), "tr_BY|be_BY_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_TR", BobruiskBY)), "en_BY|be_BY_LOCAL");
    BOOST_CHECK_EQUAL(toStr(chooseTexts("en_US", BobruiskBY)), "en_BY|be_BY_LOCAL");
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace labeler
} // namespace renderer5
} // namespace maps
