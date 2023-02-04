#include <maps/fastcgi/keyserv/lib/handlers.h>
#include <maps/fastcgi/keyserv/lib/keyserv.h>

#include <maps/libs/http/include/urlencode.h>
#include <maps/infra/yacare/include/yacare.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/xml/include/xml.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <maps/libs/stringutils/include/join.h>
#include <maps/libs/common/include/file_utils.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <fmt/format.h>

#include <string>
#include <exception>

namespace xml = maps::xml3;

namespace {

using maps::keyserv::EntryType;

const std::string MIGRATIONS_PATH =
    ArcadiaSourceRoot() + "/maps/fastcgi/keyserv/migrations/migrations";

class TestFixture : public NUnitTest::TBaseFixture {
public:
    TestFixture() : db_(blankDb().clone())
    {
        std::string configString = fmt::format(R"(<?xml version="1.0" encoding="utf-8"?>
<config>
    <database name="{name}">
        <pool writePoolSize="4" writePoolOverflow="4" readPoolSize="12" readPoolOverflow="9" pingPeriod="30" failPingPeriod="5" nearestDC="1" timeout="0"/>
        <host host="{host}" port="{port}" user="{user}" pass="{pass}"/>
    </database>
    <maxValidateKeys>7</maxValidateKeys>
</config>
            )",
            fmt::arg("name", db_.dbname()),
            fmt::arg("host", db_.host()),
            fmt::arg("port", db_.port()),
            fmt::arg("user", db_.user()),
            fmt::arg("pass", db_.password())
        );

        maps::keyserv::initializeFromString(configString);

        yacare::setErrorReporter(maps::keyserv::handleError);
    }

    ~TestFixture()
    {
        maps::keyserv::terminate();
    }

private:
    using Database = maps::local_postgres::Database;
    static Database& blankDb()
    {
        static Database instance;
        static bool isInitialized = false;
        if (!isInitialized) {
            instance.runPgMigrate(MIGRATIONS_PATH);
            isInitialized = true;
        }
        return instance;
    }

    Database db_;
};

struct LogEntry {
    LogEntry(bool blocked, const std::string& reason)
        : blocked(blocked)
        , reason(reason)
    {
    }

    bool blocked;
    std::string reason;
};

bool operator == (const LogEntry& l, const LogEntry& r)
{
    return l.blocked == r.blocked && l.reason == r.reason;
}

std::ostream& operator << (std::ostream& os, const LogEntry& e)
{
    return os << "blocked: " << e.blocked << ", reason: " << e.reason;
}

maps::http::MockResponse performRequest(const std::string& requestStr)
{
    using maps::http::MockRequest;
    using maps::http::MockResponse;
    using maps::http::URL;
    using maps::http::GET;

    MockRequest request(GET, URL("http://localhost" + requestStr));
    return yacare::performTestRequest(request);
}

xml::Doc xmlFromResponse(const std::string& requestStr)
{
    auto response = performRequest(requestStr);
    return xml::Doc::fromString(response.body);
}

// ban management

std::string stringlist(const xml::Doc& doc, const std::string& xpath)
{
    std::vector<std::string> result;
    const xml::Nodes nodes = doc.nodes(xpath, true);
    for (size_t i = 0; i < nodes.size(); ++i) {
        result.push_back(nodes[i].value<std::string>(""));
    }
    return maps::stringutils::join(result, ",");
}

std::string tag(EntryType type)
{
    return (type == EntryType::URI) ? "uri" :
           (type == EntryType::App) ? "appid" : "subnet";
}

std::string search(EntryType type, const std::string& query, const std::string& filter = "")
{
    const auto doc = xmlFromResponse(
        "/2.x/?action=search&type=" + ToString(type) + "&query=" + query);

    return stringlist(doc, "/stoplist/stop" + filter + "/" + tag(type));
}

std::string list(EntryType type)
{
    const auto doc = xmlFromResponse(
        "/2.x/?action=list&type=" + ToString(type));

    return stringlist(doc, "/stoplist/stop/" + tag(type));
}

void block(EntryType type, const std::string& id,
           long long aid = 1, const std::string& reason = "test reason")
{
    std::string encodedDescription = maps::http::urlEncode(reason);
    const auto doc = xmlFromResponse(
        "/2.x/?action=block&aid=" + std::to_string(aid) + "&type=" + ToString(type)
        + "&id=" + id + "&description=" + encodedDescription);
    doc.node("/ok");
}

void unblock(EntryType type, const std::string& id,
           long long aid = 1, const std::string& reason = "test reason")
{
    std::string encodedDescription = maps::http::urlEncode(reason);
    const auto doc = xmlFromResponse(
        "/2.x/?action=unblock&aid=" + std::to_string(aid) + "&type=" + ToString(type)
        + "&id=" + id + "&description=" + encodedDescription);
    doc.node("/ok");
}

std::vector<LogEntry> changelog(EntryType type, const std::string& id)
{
    const auto doc = xmlFromResponse(
        "/2.x/?action=changelog&type=" + ToString(type) + "&id=" + id);

    std::vector<LogEntry> result;
    const auto nodes = doc.nodes("/changelog/entry", true);
    for (size_t i = 0; i < nodes.size(); ++i) {
        const xml::Node n = nodes[i];
        result.emplace_back(n.node("blocked").value<bool>(),
                            n.node("reason").value<std::string>());
    }
    return result;
}

std::string searchBlocked(EntryType type, const std::string& query)
{
    return search(type, query, "[blocked != 0]");
}

bool isBlocked(EntryType type, const std::string& id)
{
    return !searchBlocked(type, id).empty();
}

std::string blockedApps()
{
    const auto doc = xmlFromResponse("/2.x/?action=list&type=app");
    return stringlist(doc, "//stop[blocked != 0]/appid");
}

// key management

xml::Doc generateKey(const std::string& uri, const long long uid = 1)
{
    std::string encodedUri = maps::http::urlEncode(uri);
    return xmlFromResponse("/2.x/?action=generateKey&uri="
        + encodedUri + "&uid=" + std::to_string(uid));
}

xml::Doc checkKey(
    const std::string& keys,
    const std::string& uri = "",
    const std::string& ip = "",
    const bool allowEmptyKey = false)
{
    std::string encodedUri = maps::http::urlEncode(uri);
    std::string encodedIp = maps::http::urlEncode(ip);
    return xmlFromResponse("/2.x/?action=checkKey&keys=" + keys
        + "&uri=" + encodedUri + "&ip=" + encodedIp
        + "&allowEmptyKey=" + std::to_string(allowEmptyKey));
}

bool isEmptyKeyBlocked(const std::string& uri, const std::string& ip)
{
    const auto doc = checkKey("", uri, ip, true);
    return !doc.node("/keystate/valid").value<bool>();
}

xml::Doc blockKey(
    const std::string& key,
    const long long aid = 1,
    const int reason = 2,
    const std::string& description = "test reason")
{
    std::string encodedDescription = maps::http::urlEncode(description);
    return xmlFromResponse("/2.x/?action=blockKey&aid=" + std::to_string(aid)
        + "&key=" + key + "&reason=" + std::to_string(reason)
        + "&description=" + encodedDescription);
}

xml::Doc unblockKey(
    const std::string& key,
    const long long aid = 1)
{
    return xmlFromResponse("/2.x/?action=unblockKey&aid="
        + std::to_string(aid) + "&key=" + key);
}

xml::Doc describeKey(const std::string& key)
{
    return xmlFromResponse("/2.x/?action=describeKey&key=" + key);
}

xml::Doc findHostKeys(const std::string& host)
{
    return xmlFromResponse("/2.x/?action=findHostKeys&host=" + host);
}

[[maybe_unused]] xml::Doc findHostKeysCount(const std::string& host)
{
    return xmlFromResponse("/2.x/?action=findHostKeysCount&host=" + host);
}

xml::Doc keyHistory(const std::string& key)
{
    return xmlFromResponse("/2.x/?action=keyHistory&key=" + key);
}

xml::Doc restrictions()
{
    return xmlFromResponse("/2.x/?action=restrictions");
}

xml::Doc setRestriction(
    const std::string& key,
    const std::string& name,
    const std::string& value)
{
    return xmlFromResponse("/2.x/?action=setRestriction&key=" + key
        + "&name=" + name + "&value=" + value);
}

xml::Doc listRestrictedKeys(const std::string& name)
{
    return xmlFromResponse("/2.x/?action=listRestrictedKeys&name=" + name);
}

xml::Doc setKeyComment(
    const std::string& key,
    const std::string& comment)
{
    return xmlFromResponse("/2.x/?action=setKeyComment&key=" + key
        + "&comment=" + comment);
}

xml::Doc findKeysByComment(const std::string& comment)
{
    return xmlFromResponse("/2.x/?action=findKeysByComment&comment=" + comment);
}

} // namespace

Y_UNIT_TEST_SUITE_F(keyserv_test, TestFixture) {

Y_UNIT_TEST(test_GenerateKey)
{
    xml::Doc doc = generateKey("http://www.testgeneratekey.com");

    xml::Node node = doc.node("//keystate/key", true);
    std::string key = node.value<std::string>();

    doc = checkKey(key, "testgeneratekey.com./", "255.255.255.255");

    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());
    EXPECT_TRUE(!doc.node("/keystate/broken").value<bool>());

    std::string describe;
    describeKey(key).save(describe);
    EXPECT_TRUE(describe.find("testgeneratekey.com") != std::string::npos);
    EXPECT_TRUE(describe.find(key) != std::string::npos);
}

Y_UNIT_TEST(test_BlockUnblockKey)
{
    const std::string uri = "http://www.testCheckKey.com";
    const auto key = generateKey(uri).node("//keystate/key").value<std::string>();

    auto doc = checkKey(key, uri);
    EXPECT_TRUE(doc.node("//keystate/valid").value<bool>());

    blockKey(key);
    doc = checkKey(key, uri);
    EXPECT_TRUE(!doc.node("//keystate/valid").value<bool>());

    unblockKey(key);
    doc = checkKey(key, uri);
    EXPECT_TRUE(doc.node("//keystate/valid").value<bool>());
}

Y_UNIT_TEST(test_BlockUnblockKeyWithURI)
{
    std::string uri = "http://www.testCheckKey.com";
    xml::Doc doc = generateKey(uri);
    xml::Node node = doc.node("//keystate/key");
    std::string key = node.value<std::string>();

    const std::string test_ip = "255.255.255.255";
    unblock(EntryType::IP, test_ip);

    doc = checkKey(key, uri, test_ip, false);
    node = doc.node("//keystate/valid");
    EXPECT_TRUE(node.value<bool>());

    blockKey(key);

    doc = checkKey(key, uri, test_ip, false);
    node = doc.node("//keystate/valid");
    EXPECT_TRUE(!node.value<bool>());

    unblockKey(key);
    doc = checkKey(key, uri, test_ip, false);
    node = doc.node("//keystate/valid");
    EXPECT_TRUE(node.value<bool>());

    // block/unblock URI
    block(EntryType::URI, uri);
    doc = checkKey(key, uri, test_ip, false);
    EXPECT_TRUE(!doc.node("//keystate/valid").value<bool>());

    unblock(EntryType::URI, uri);
    doc = checkKey(key, uri, test_ip, false);
    EXPECT_TRUE(doc.node("//keystate/valid").value<bool>());

    // check empty key
    doc = checkKey("", uri, test_ip, true);
    EXPECT_TRUE(doc.node("//keystate/valid").value<bool>());
}

Y_UNIT_TEST(test_blockIp)
{
    const std::string& subnet1 = "123.45.67.89/16";
    unblock(EntryType::IP, subnet1);
    const std::string& subnet2 = "123.45.67.89/24";
    block(EntryType::IP, subnet2);

    EXPECT_TRUE(isBlocked(EntryType::IP, "123.45.67.1"));
    EXPECT_TRUE(isBlocked(EntryType::IP, "123.45.67.255"));
    EXPECT_TRUE(isBlocked(EntryType::IP, "::ffff:123.45.67.1"));
    EXPECT_TRUE(isBlocked(EntryType::IP, "::ffff:7b2d:4350"));
    EXPECT_TRUE(!isBlocked(EntryType::IP, "::ffff:7bbb:abcd"));
    EXPECT_TRUE(!isBlocked(EntryType::IP,"123.45.68.255"));

    const std::string uri = "http://www.test.com";
    EXPECT_TRUE(isEmptyKeyBlocked(uri, "123.45.67.1"));
    EXPECT_TRUE(isEmptyKeyBlocked(uri, "123.45.67.255"));
    EXPECT_TRUE(!isEmptyKeyBlocked(uri, "123.45.68.255"));

    unblock(EntryType::IP, subnet2);
    EXPECT_TRUE(!isBlocked(EntryType::IP, "123.45.67.89"));
    EXPECT_TRUE(!isBlocked(EntryType::IP, "::ffff:123.45.67.1"));
    EXPECT_TRUE(!isBlocked(EntryType::IP, "::ffff:7b2d:4350"));

    EXPECT_TRUE(!isEmptyKeyBlocked(uri, "123.45.67.12"));
}

Y_UNIT_TEST(test_blockIpv6)
{
    const std::string subnet1 = "2015::/16";
    unblock(EntryType::IP, subnet1);

    const std::string subnet2 = "2015:aaaa::/32";
    block(EntryType::IP, subnet2);

    EXPECT_TRUE(isBlocked(EntryType::IP, "2015:aaaa::1"));
    EXPECT_TRUE(isBlocked(EntryType::IP, "2015:aaaa:0001::"));
    EXPECT_TRUE(!isBlocked(EntryType::IP,"2015:bbbb::"));
    EXPECT_TRUE(!isBlocked(EntryType::IP,"2014::"));

    const std::string uri = "http://www.test.com";
    EXPECT_TRUE(isEmptyKeyBlocked(uri, "2015:aaaa::2"));
    EXPECT_TRUE(!isEmptyKeyBlocked(uri,"2015:bbbb::"));
    EXPECT_TRUE(!isEmptyKeyBlocked(uri,"2042::3"));

    unblock(EntryType::IP, subnet2);
    EXPECT_TRUE(!isBlocked(EntryType::IP, "2015:aaaa::1"));

    EXPECT_TRUE(!isEmptyKeyBlocked(uri, "2015:aaaa::2"));
}

Y_UNIT_TEST(test_blockUnblockUri)
{
    const auto uri = "badsite.com/badpage";

    EXPECT_TRUE(!isBlocked(EntryType::URI, uri));

    block(EntryType::URI, uri);
    EXPECT_TRUE(isBlocked(EntryType::URI, uri));
    EXPECT_TRUE(!isBlocked(EntryType::URI, "http://www.yandex.com"));

    const auto ip = "123.45.67.89";
    EXPECT_TRUE(isEmptyKeyBlocked("http://www.badsite.com/badpage/1", ip));
    EXPECT_TRUE(isEmptyKeyBlocked("http://www.foo.badsite.com/badpage", ip));
    EXPECT_TRUE(!isEmptyKeyBlocked("http://www.yandex.com", ip));

    unblock(EntryType::URI, uri);
    EXPECT_TRUE(!isBlocked(EntryType::URI, uri));

    EXPECT_TRUE(!isEmptyKeyBlocked("http://foo.badsite.com/badpage", ip));
}

Y_UNIT_TEST(test_BlockUnblockApp)
{
    const auto appId = "ru.cheaters.www";

    EXPECT_TRUE(!isBlocked(EntryType::App, appId));

    block(EntryType::App, appId);
    EXPECT_TRUE(isBlocked(EntryType::App, appId));

    unblock(EntryType::App, appId);
    EXPECT_TRUE(!isBlocked(EntryType::App, appId));
}

Y_UNIT_TEST(test_Restrictions)
{
    std::string restrs;
    restrictions().save(restrs);
    EXPECT_TRUE(restrs.find("allowLogoDisabling") != std::string::npos);

    std::string uri = "http://www.testRestrictions.com";
    xml::Doc doc = generateKey(uri);
    xml::Node node = doc.node("//keystate/key");
    std::string key = node.value<std::string>();

    setRestriction(key, "allowLogoDisabling", std::string("TESTSTATE")).node("/ok");

    std::string listOne;
    listRestrictedKeys("allowLogoDisabling").save(listOne);
    EXPECT_TRUE(listOne.find("TESTSTATE") != std::string::npos);
}

Y_UNIT_TEST(test_comments)
{
    const std::string uri = "testhost.testsite.ru";
    xml::Doc doc = generateKey(uri);
    xml::Node node = doc.node("//keystate/key", true);
    EXPECT_TRUE(!node.isNull());
    std::string key = node.value<std::string>();

    setKeyComment(key.c_str(), "TEST KEY COMMENT").node("/ok");

    doc = describeKey(key.c_str());
    node = doc.node("//keylist/keystate/note", true);
    EXPECT_TRUE(node.value<std::string>() == "TEST KEY COMMENT");

    std::string keyData;
    findKeysByComment("KEY").save(keyData);
    EXPECT_TRUE(keyData.find(key) != std::string::npos);
}

Y_UNIT_TEST(test_status)
{
    const std::string uri = "http://www.testStatus.com";
    const xml::Doc doc = generateKey(uri);
    const auto key = doc.node("//keystate/key").value<std::string>();

    blockKey(key, 1, 1234);
    unblockKey(key);

    std::string history;
    keyHistory(key).save(history);
    EXPECT_TRUE(history.find("reason=\"0\"") != std::string::npos);
    EXPECT_TRUE(history.find("reason=\"1234\"") != std::string::npos);
}

Y_UNIT_TEST(test_hosts)
{
    std::string uri = "http://www.testhost.com";
    xml::Doc doc = generateKey(uri);
    xml::Node node = doc.node("//keystate/key");
    std::string key = node.value<std::string>();

    std::string searchResult;
    findHostKeys("testhost.com").save(searchResult);
    EXPECT_TRUE(searchResult.find(key));
}

Y_UNIT_TEST(test_whitelist)
{
    const std::string ip = "123.45.67.89";

    //BLOCK
    const std::string whiteListUri = "yandex.ru";
    block(EntryType::URI, whiteListUri);

    //OK
    auto doc = checkKey("", "http://www.yandex.ru", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.com.", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.com.tr", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.ua", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.kz.", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.by", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    //BLOCK
    block(EntryType::URI, "badsite.ru");

    //OK
    doc = checkKey("", "http://www.yandex.ru/?param=badsite.ru", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.com/badsite.ru", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.goodsite.ru/badsite.ru", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    //FAIL
    doc = checkKey("", "http://www.badsite.ru", ip, true);
    EXPECT_TRUE(!doc.node("/keystate/valid").value<bool>());


    doc = checkKey("", "http://www.badsite.ru/?param=yandex.ru", ip, true);
    EXPECT_TRUE(!doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.badsite.ru/yandex.ru", ip, true);
    EXPECT_TRUE(!doc.node("/keystate/valid").value<bool>());

    doc = checkKey("", "http://www.yandex.ru.badsite.ru", ip, true);
    EXPECT_TRUE(!doc.node("/keystate/valid").value<bool>());

    //BLOCK
    block(EntryType::URI, "goodsite.ru/badpage");

    //OK
    doc = checkKey("", "http://www.goodsite.ru/goodpage", ip, true);
    EXPECT_TRUE(doc.node("/keystate/valid").value<bool>());

    //FAIL
    doc = checkKey("", "http://www.goodsite.ru/badpage?param=foo", ip, true);
    EXPECT_TRUE(!doc.node("/keystate/valid").value<bool>());

    unblock(EntryType::URI, "badsite.ru");
    unblock(EntryType::URI, "goodsite.ru/badpage");
}

Y_UNIT_TEST(test_findBlockedUri)
{
    block(EntryType::URI, "badsite.ru/page");

    EXPECT_EQ(searchBlocked(EntryType::URI, "badsite"), "badsite.ru/page");
    EXPECT_EQ(searchBlocked(EntryType::URI, "badsite.ru"), "badsite.ru/page");
    EXPECT_EQ(searchBlocked(EntryType::URI, "badsite.ru/page"), "badsite.ru/page");

    unblock(EntryType::URI, "badsite.ru/page");
}

Y_UNIT_TEST(test_searchBannedIP)
{
    EXPECT_EQ(searchBlocked(EntryType::IP, "123.45.67.1"), "");

    block(EntryType::IP, "123.45.67.89/24");

    EXPECT_EQ(searchBlocked(EntryType::IP, "123.45.67.1"), "123.45.67.89/24");
    EXPECT_EQ(searchBlocked(EntryType::IP, "123.45.0.0"), "");

    unblock(EntryType::IP, "123.45.67.89/24");

    EXPECT_EQ(searchBlocked(EntryType::IP, "123.45.67.1"), "");
}

Y_UNIT_TEST(test_searchBannedUri)
{
    EXPECT_EQ(searchBlocked(EntryType::URI, "badsite.ru"), "");

    block(EntryType::URI, "badsite.ru");
    EXPECT_EQ(searchBlocked(EntryType::URI, "badsite.ru"), "badsite.ru");

    block(EntryType::URI, "inner.badsite.ru");
    EXPECT_EQ(searchBlocked(EntryType::URI, "badsite.ru"), "badsite.ru,inner.badsite.ru");

    unblock(EntryType::URI, "inner.badsite.ru");
    unblock(EntryType::URI, "badsite.ru");
}

Y_UNIT_TEST(test_searchBannedApp)
{
    const std::string app = "ru.cheaters.app";
    const std::string another = "ru.cheaters.another";
    EXPECT_EQ(searchBlocked(EntryType::App, "ru.cheaters"), "");

    block(EntryType::App, app);

    EXPECT_EQ(searchBlocked(EntryType::App, "ru.goodpeople"), "");
    EXPECT_EQ(searchBlocked(EntryType::App, "ru.cheaters"), app);
    EXPECT_EQ(searchBlocked(EntryType::App, another), "");

    block(EntryType::App, another);
    EXPECT_EQ(searchBlocked(EntryType::App, app), app);
    EXPECT_EQ(searchBlocked(EntryType::App, another), another);
    EXPECT_EQ(searchBlocked(EntryType::App, "ru.cheaters"), app + "," + another);

    unblock(EntryType::App, app);
    EXPECT_EQ(searchBlocked(EntryType::App, "ru.cheaters"), another);

    unblock(EntryType::App, another);
    EXPECT_EQ(searchBlocked(EntryType::App, "ru.cheaters"), "");
}

Y_UNIT_TEST(test_internal_pages_key)
{
    const bool allowEmptyKey = false;
    const std::string ip("123.45.67.89");

    std::string result;

    const std::string keyDomain = generateKey("testgeneratekey.com")
        .node("//keystate/key", true)
        .value<std::string>();

    const std::string keyInternal = generateKey("http://www.testgeneratekey.com/mypage.html")
        .node("//keystate/key", true)
        .value<std::string>();

    EXPECT_TRUE(keyDomain != keyInternal);
    //domain key is valid for the domain - obvious
    result = checkKey(keyDomain, "testgeneratekey.com", ip, allowEmptyKey).node("/keystate/valid").value<std::string>();
    EXPECT_EQ(result, "true");
    //www prefix is ignored
    result = checkKey(keyDomain, "www.testgeneratekey.com", ip, allowEmptyKey).node("/keystate/valid").value<std::string>();
    EXPECT_EQ(result, "true");
    //internal page key is valid for the internal page - obvious
    result = checkKey(keyInternal, "http://www.testgeneratekey.com/mypage.html", ip, allowEmptyKey).node("/keystate/valid").value<std::string>();
    EXPECT_EQ(result, "true");
    //internal page key isn't valid for the other pages
    result = checkKey(keyInternal, "http://www.testgeneratekey.com/yourpage.html", ip, allowEmptyKey).node("/keystate/valid").value<std::string>();
    EXPECT_EQ(result, "false");
    //internal page key isn't valid for the domain
    result = checkKey(keyInternal, "testgeneratekey.com", ip, allowEmptyKey).node("/keystate/valid").value<std::string>();
    EXPECT_EQ(result, "false");
    //domain key is valid for the internal pages
    result = checkKey(keyDomain, "www.testgeneratekey.com/mypage.html", ip, allowEmptyKey).node("/keystate/valid").value<std::string>();
    EXPECT_EQ(result, "true");
}

Y_UNIT_TEST(test_change_history)
{
    const std::string block_reason = "testchange: block reason (with special chars >&<)";
    const std::string unblock_reason = "testchange: unblock reason";

    const std::vector<std::pair<EntryType, std::string>> data = {
        {EntryType::IP,  "11.22.0.0/16"},
        {EntryType::URI, "badsite.com"},
        {EntryType::App, "tr.com.badapp"}
    };

    for (const auto& [type, id]: data) {
        block(type, id, 1, block_reason);
        EXPECT_EQ(changelog(type, id).at(0), LogEntry(true, block_reason));

        unblock(type, id, 1, unblock_reason);
        const auto entries = changelog(type, id);
        EXPECT_EQ(entries.at(0), LogEntry(false, unblock_reason));
        EXPECT_EQ(entries.at(1), LogEntry(true, block_reason));
    }
}

Y_UNIT_TEST(test_list_blocked_apps)
{
    const std::string ru = "ru.blocked.app";
    const std::string com = "com.blocked.app";
    EXPECT_EQ(blockedApps(), "");

    block(EntryType::App, ru);
    EXPECT_EQ(blockedApps(), ru);

    block(EntryType::App, com);
    EXPECT_EQ(blockedApps(), com + "," + ru);

    unblock(EntryType::App, ru);
    EXPECT_EQ(blockedApps(), com);

    unblock(EntryType::App, com);
    EXPECT_EQ(blockedApps(), "");
}

Y_UNIT_TEST(test_list)
{
    const std::string app = "ru.blocked.app";
    const std::string uri = "badsite.com";
    const std::string ip = "123.45.67.89/24";
    EXPECT_EQ(blockedApps(), "");

    block(EntryType::App, app);
    EXPECT_TRUE(list(EntryType::App).find(app) != std::string::npos);
    unblock(EntryType::App, app);
    EXPECT_TRUE(list(EntryType::App).find(app) != std::string::npos);

    block(EntryType::IP, ip);
    EXPECT_TRUE(list(EntryType::IP).find(ip) != std::string::npos);
    unblock(EntryType::IP, ip);
    EXPECT_TRUE(list(EntryType::IP).find(ip) != std::string::npos);

    block(EntryType::URI, uri);
    EXPECT_TRUE(list(EntryType::URI).find(uri) != std::string::npos);
    unblock(EntryType::URI, uri);
    EXPECT_TRUE(list(EntryType::URI).find(uri) != std::string::npos);

    EXPECT_EQ(blockedApps(), "");
}

Y_UNIT_TEST(test_banhammer_over_whitelist)
{
    const std::string BAD_IP_ADDRESS = "231.45.67.89/8";
    const std::string GOOD_IP_ADDRESS = "123.45.67.89";

    const std::string WHITELISTED_REFERER = "maps.yandex.ru";
    block(EntryType::IP, BAD_IP_ADDRESS);

    const std::string KEY = generateKey(WHITELISTED_REFERER)
        .node("//keystate/key")
        .value<std::string>();

    EXPECT_TRUE(
        checkKey(KEY, WHITELISTED_REFERER, GOOD_IP_ADDRESS)
            .node("//valid")
            .value<bool>()
    );
    EXPECT_TRUE(
        not checkKey(KEY, WHITELISTED_REFERER, BAD_IP_ADDRESS)
            .node("//valid")
            .value<bool>()
    );
}

Y_UNIT_TEST(test_incorrect_input_checkKey)
{
    // test correct input first
    auto response = performRequest("/?action=checkKey&allowEmptyKey=1&ip=1.1.1.1");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//keystate"));
    EXPECT_THROW(doc.node("//keystate/error"), xml::NodeNotFound);

    // test incorrect ip. It should be considered as if no ip was passed
    response = performRequest("/?action=checkKey&allowEmptyKey=1&ip=256.1.1.1");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_THROW(doc.node("//keystate/error"), xml::NodeNotFound);
    auto node = doc.node("//keystate/valid", true);
    EXPECT_FALSE(node.isNull());
    EXPECT_TRUE(node.value<bool>()); // empty key is allowed

    response = performRequest("/?action=checkKey&allowEmptyKey=0&ip=256.1.1.1");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_THROW(doc.node("//keystate/error"), xml::NodeNotFound);
    node = doc.node("//keystate/valid", true);
    EXPECT_FALSE(node.isNull());
    EXPECT_FALSE(node.value<bool>()); // empty key is not allowed

    // test incorrect uri
    response = performRequest("/?action=checkKey&allowEmptyKey=1&uri=http%3A%3A%2F%2F%2F%2F%2F");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//keystate/error"));

    // test incorrect key
    response = performRequest("/?action=checkKey&key=xxxxx&allowEmptyKey=0");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_THROW(doc.node("//keystate/error"), xml::NodeNotFound);
    node = doc.node("//keystate/broken", true);
    EXPECT_FALSE(node.isNull());
    EXPECT_TRUE(node.value<bool>());
}

Y_UNIT_TEST(test_incorrect_input_generateKey)
{
    // test correct input
    auto response = performRequest("/?action=generateKey&uid=1&uri=www.testgeneratekey.com");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_THROW(doc.node("//keystate/error"), xml::NodeNotFound);

    // test incorrect uri
    response = performRequest("/?action=generateKey&uid=1&uri=http%3A%3A");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//keystate/error"));
}

Y_UNIT_TEST(test_incorrect_input_search)
{
    // test correct input
    auto response = performRequest("/2.x/?action=search&type=ip&query=1.1.1.1");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_THROW(doc.node("//keystate/error"), xml::NodeNotFound);

    // test incorrect ip
    response = performRequest("/2.x/?action=search&type=ip&query=1.1.1.256");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));

    // test incorrect uri
    response = performRequest("/2.x/?action=search&type=uri&query=http%3A%3A");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));
}

Y_UNIT_TEST(test_incorrect_input_block_unblock)
{
    // test incorrect ip
    auto response = performRequest("/2.x/?action=block&type=ip&id=1.1.1.256&aid=1&description=desc");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));

    response = performRequest("/2.x/?action=unblock&type=ip&id=1.1.1.256&aid=1&description=desc");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));

    // test incorrect uri
    response = performRequest("/2.x/?action=block&type=uri&id=http%3A%3A&aid=1&description=desc");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));

    response = performRequest("/2.x/?action=unblock&type=uri&id=http%3A%3A&aid=1&description=desc");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));
}

Y_UNIT_TEST(test_incorrect_input_changelog)
{
    // test incorrect ip
    auto response = performRequest("/2.x/?action=changelog&type=ip&id=1.1.1.256");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));

    // test incorrect uri
    response = performRequest("/2.x/?action=changelog&type=uri&id=http%3A%3A");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));
}

Y_UNIT_TEST(test_incorrect_input_describeKey)
{
    auto response = performRequest("/2.x/?action=describeKey&key=xxx");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
}

Y_UNIT_TEST(test_incorrect_input_blockKey_unblockKey)
{
    auto response = performRequest("/2.x/?action=blockKey&key=xxx&aid=1&description=desc&reason=0");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));

    response = performRequest("/2.x/?action=unblockKey&key=xxx&aid=1&description=desc");
    doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));
}

Y_UNIT_TEST(test_incorrect_input_setKeyComment)
{
    auto response = performRequest("/2.x/?action=setKeyComment&key=xxx&comment=comment");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));
}

Y_UNIT_TEST(test_incorrect_input_keyHistory)
{
    auto response = performRequest("/2.x/?action=keyHistory&key=xxx");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
}

Y_UNIT_TEST(test_incorrect_input_setRestriction)
{
    auto response = performRequest("/2.x/?action=setRestriction&key=xxx&name=name&value=value");
    auto doc = xml::Doc::fromString(response.body);
    EXPECT_EQ(response.status, 200);
    EXPECT_NO_THROW(doc.node("//error"));
}

} // Y_UNIT_TEST_SUITE
