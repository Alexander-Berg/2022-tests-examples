#include <maps/libs/cmdline/include/cmdline.h>

#include <maps/wikimap/mapspro/libs/common/include/yandex/maps/wiki/common/secrets.h>

#include <maps/wikimap/mapspro/libs/bell_client/include/client.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/log8/include/log8.h>

#include <util/string/strip.h>
#include <util/system/env.h>

namespace bc = maps::wiki::bell_client;
namespace secrets = maps::wiki::common::secrets;
namespace json = maps::json;

const TString SUP_TOKEN_ENV = "SUP_TOKEN";
const std::string SUP_PRIEMKA_URL = "http://sup-priemka.n.yandex-team.ru";

int main(int argc, char* argv[]) {
    maps::cmdline::Parser parser;
    auto uid = parser.option<uint64_t>('u').required().help(
        "Test pasport UID for Bell. How to get: https://nda.ya.ru/t/qgmVLAzE3VtZhv"
    );
    auto commitsCount = parser.option<uint64_t>('c').required().help(
        "Number of commits to put inside template "
        "(https://tanker.yandex-team.ru/?project=disk_notifiier&branch=master&keyset=map_editor&key=nmaps_edits_release)"
    );
    auto groupKey = parser.string('g').help(
        "Bell's group key. Pushes with the same key will be squashed, taking last provided meta"
    );
    parser.parse(argc, argv);

    maps::log8::setLevel(maps::log8::Level::DEBUG);

    std::string supToken = Strip(GetEnv(SUP_TOKEN_ENV));
    if (supToken.empty()) {
        supToken = secrets::tokenByKey(secrets::Key::RobotWikimapSupToken);
    }

    bc::Configuration config{
        SUP_PRIEMKA_URL,
        supToken,
        "nmaps",
        "map_editor"
    };

    bc::Client client(config);

    json::Builder metaBuilder;
    metaBuilder << [&](json::ObjectBuilder builder) {
        builder["count"] << [&](json::ObjectBuilder builder) {
            builder["type"] << "text";
            builder["text"] << std::to_string(commitsCount);
        };
        builder["action"] << [&](json::ObjectBuilder builder) {
            builder["type"] << "link";
            builder["link"] << "https://n.maps.yandex.ru/#!/users/" + std::to_string(uid) + "/feeds/edits";
        };
    };
    auto meta = json::Value::fromString(metaBuilder.str());

    /* 'project', 'service' (both in client config) and 'type' have to match server configuration
     * 'actor' is required field, but may be filled with anything (?) */
    bc::PushInfo pushInfo(uid, "ya_map_editor", bc::NotificationType::NmapsEditsRelease, meta);

    if (groupKey.defined()) {
        pushInfo.groupKey = groupKey;
    }

    client.push(pushInfo);
}
