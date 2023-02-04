#include <maps/infopoint/tests/common/fixture.h>

namespace {
const std::string MESSAGES_PATH = "/usr/share/yandex/maps/i18n/messages";
const std::string MESSAGES_DOMAIN = "infopoints";

std::locale testLocaleGetter() {
    return maps::i18n::bestLocale("ru_RU");
}
}

LocaleFixture::LocaleFixture()
{
    maps::i18n::addMessagesPath(MESSAGES_PATH);
    maps::i18n::addMessagesDomain(MESSAGES_DOMAIN);
    infopoint::resetLocaleGetter(testLocaleGetter);
}

LocaleFixture::~LocaleFixture()
{
    infopoint::resetLocaleGetter();
}
