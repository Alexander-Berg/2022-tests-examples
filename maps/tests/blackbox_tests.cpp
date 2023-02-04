#include <maps/infopoint/tests/common/fake_blackbox.h>

#include <maps/infopoint/lib/auth/blackbox.h>
#include <maps/infopoint/lib/misc/exceptions.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/json/include/exception.h>
#include <maps/libs/json/include/std.h>
#include <yandex/maps/i18n/i18n.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace infopoint;

const maps::http::URL STUB_BLACKBOX_URL("http://localhost");
const std::string STUB_REMOTE_IP = "127.0.0.1";

const auto UNRECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY =
    maps::json::Value::fromString(R"({
  "exception": {
    "value":"INVALID_PARAMS",
    "id":2
   },
   "error":"BlackBox error: Missing userip argument"
}
)");

const auto RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY =
    maps::json::Value::fromString(R"({
  "exception": {
    "value":"DB_EXCEPTION",
    "id":10
   },
   "error":"BlackBox error: Recoverable db error"
}
)");

// http client replacer that returns subsequent responses on every request.
class BlackboxResponseSequence {
public:
    explicit BlackboxResponseSequence(
        std::vector<maps::json::Value> responses)
        : responses_(std::move(responses))
    {
        ASSERT(!responses_.empty());
    }

    maps::json::Value operator()(
        const maps::http::URL&,
        const maps::http::HeaderMap&)
    {
        const auto response = responses_[idx_++];
        if (idx_ >= responses_.size()) {
            idx_ = responses_.size() - 1;
        }
        return response;
    }

private:
    size_t idx_ = 0;
    const std::vector<maps::json::Value> responses_;
};

void fillBlackboxStorage(
    const std::shared_ptr<LocalBlackboxStorage> storage,
    const std::unordered_set<Uid>& uids,
    const std::unordered_set<Uid>& bannedUids,
    const std::unordered_set<Uid>& errorUids,
    const std::unordered_set<Uid>& missingUids)
{
    for (const auto& uid : uids) {
        if (missingUids.count(uid)) {
            continue;
        }
        storage->upsertUser({
            uid,
            PublicUid("puid" + uid.value()),
            "token" + uid.value(),
            "public_name" + uid.value(),
            bannedUids.count(uid)
                ? BlackboxUserStatus::Banned
                : BlackboxUserStatus::Regular,
            errorUids.count(uid)
                ? BlackboxUserBehavior::AlwaysGenerateError
                : BlackboxUserBehavior::Normal});
    }
}

maps::json::Value userinfoBlackboxOkResponseBody(
    const std::vector<Uid>& uids)
{
    const auto storage = std::make_shared<LocalBlackboxStorage>();
    fillBlackboxStorage(storage, {uids.begin(), uids.end()}, {}, {}, {});
    const LocalBlackboxFetcher fetcher(storage);
    return fetcher.userinfo(uids);
};

// 'banned' - user started deletion process or was banned.
// 'error' - always return blackbox error for such users.
// 'missing' - no such user or user was deleted.
// 'testIndividual' - whether must check authorization and fetching single
//  user name for each uid.
void testFetchUserNames(
    const std::unordered_set<Uid>& uids,
    const std::unordered_set<Uid>& bannedUids,
    const std::unordered_set<Uid>& errorUids,
    const std::unordered_set<Uid>& missingUids,
    const bool testIndividually)
{
    maps::i18n::addMessagesDomain(MESSAGES_DOMAIN,
        { maps::Locale(maps::LANG_RU) });

    const auto storage = std::make_shared<LocalBlackboxStorage>();
    fillBlackboxStorage(storage, uids, bannedUids, errorUids, missingUids);
    LocalBlackboxFetcher fetcher(storage);

    Blackbox blackbox(
        STUB_BLACKBOX_URL,
        BlackboxAuthorizationMethod::NoAuthorization,
        fetcher);

    const auto uidToInfo = blackbox.fetchUserInfos(uids, STUB_REMOTE_IP);
    for (const auto& uid : uids) {
        if (bannedUids.count(uid) || errorUids.count(uid)
            || missingUids.count(uid) || !isValid(uid))
        {
            EXPECT_TRUE(uidToInfo.count(uid) == 0);
            continue;
        }

        const auto infoIt = uidToInfo.find(uid);
        ASSERT_NE(infoIt, uidToInfo.end());
        EXPECT_EQ(infoIt->second.name, "public_name" + uid.value());
        EXPECT_EQ(infoIt->second.puid.value(), "puid" + uid.value());
    }

    for (const auto& uidAndName : uidToInfo) {
        EXPECT_TRUE(uids.contains(uidAndName.first));
    }

    if (!testIndividually) {
        return;
    }

    for (const auto& uid : uids) {
        const auto info = blackbox.fetchUserInfo(uid, STUB_REMOTE_IP);
        if (bannedUids.count(uid) || errorUids.count(uid)
            || missingUids.count(uid) || !isValid(uid))
        {
            EXPECT_FALSE(info.has_value());
            continue;
        }
        EXPECT_EQ(info->name, "public_name" + uid.value());
        EXPECT_EQ(info->puid.value(), "puid" + uid.value());
    }
}


TEST(Blackbox, Uid)
{
    EXPECT_TRUE(isValid(Uid("1")));
    EXPECT_TRUE(isValid(Uid("123")));
    EXPECT_FALSE(isValid(Uid("")));
    EXPECT_FALSE(isValid(Uid(" 123")));
    EXPECT_FALSE(isValid(Uid("12,3")));
    EXPECT_EQ(
        makeUserUri(Uid("123")), UserURI("urn:yandex-uid:123"));
    EXPECT_EQ(
        makeUserPublicUri(PublicUid("123")),
        PublicUserURI("urn:yandex-puid:123"));
    EXPECT_EQ(
        *tryExtractUid(UserURI("urn:yandex-uid:123")),
        Uid("123"));
    EXPECT_FALSE(tryExtractUid(UserURI("urn:yandex-uid:12,3")).has_value());
    EXPECT_FALSE(tryExtractUid(UserURI("")).has_value());
    EXPECT_FALSE(tryExtractUid(UserURI("not_uid:123")).has_value());
}

TEST(Blackbox, MisformedBlackboxResponse)
{
    EXPECT_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            [](const auto&, const auto&) -> maps::json::Value {
                throw maps::json::ParserError();
            }),
        maps::json::ParserError);
}

TEST(Blackbox, BlackboxErrorRecovery)
{
    EXPECT_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            BlackboxResponseSequence({
                UNRECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY
            })
        ),
        infopoint::BlackboxError);

    EXPECT_NO_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            BlackboxResponseSequence({
                userinfoBlackboxOkResponseBody({Uid("123"), Uid("234")})
            })
        )
    );

    EXPECT_NO_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            BlackboxResponseSequence({
                RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY,
                userinfoBlackboxOkResponseBody({Uid("123")})
            })
        )
    );

    // Must not recover after exceeding retries count.
    EXPECT_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            BlackboxResponseSequence({
                RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY
            })
        ),
        BlackboxError);

    EXPECT_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            BlackboxResponseSequence({
                RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY,
                RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY,
                userinfoBlackboxOkResponseBody({Uid("123")})
            })
        ),
        BlackboxError);

    EXPECT_THROW(
        fetchFromBlackbox(
            STUB_BLACKBOX_URL,
            {},
            2,
            BlackboxResponseSequence({
                UNRECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY,
                userinfoBlackboxOkResponseBody({Uid("123")})
            })
        ),
        BlackboxError);
}

TEST(Blackbox, FetchUserNamesErrorHandling)
{
    const auto fetchUserInfos = [](const std::unordered_set<Uid>& uids,
        std::vector<maps::json::Value> responses) -> auto
    {
        Blackbox bb(
            STUB_BLACKBOX_URL,
            BlackboxAuthorizationMethod::NoAuthorization,
            BlackboxResponseSequence(std::move(responses)));
        return bb.fetchUserInfos(uids, STUB_REMOTE_IP);
    };

    EXPECT_THROW(
        fetchUserInfos(
            {Uid("123")},
            { UNRECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY }
        ),
        BlackboxError);

    EXPECT_THROW(
        fetchUserInfos(
            {Uid("123")},
            { RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY }
        ),
        BlackboxError);

    EXPECT_NO_THROW(
        fetchUserInfos(
            {Uid("123")},
            {
                RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY,
                userinfoBlackboxOkResponseBody({Uid("123")})
            }
        )
    );

    const auto tryFetchUserInfos = [](const std::unordered_set<Uid>& uids,
        std::vector<maps::json::Value> responses) -> auto
    {
        Blackbox bb(
            STUB_BLACKBOX_URL,
            BlackboxAuthorizationMethod::NoAuthorization,
            BlackboxResponseSequence(std::move(responses)));
        return bb.tryFetchUserInfos(uids, STUB_REMOTE_IP);
    };

    {
        const auto infos = tryFetchUserInfos(
            {Uid("123")},
            { UNRECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY });
        EXPECT_TRUE(infos.empty());
    }

    {
        const auto infos = tryFetchUserInfos(
            {Uid("123")},
            { RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY });
        EXPECT_TRUE(infos.empty());
    }

    {
        const auto infos = tryFetchUserInfos(
            {Uid("123")},
            {
                RECOVERABLE_BLACKBOX_ERROR_RESPONSE_BODY,
                userinfoBlackboxOkResponseBody({Uid("123")})});
        EXPECT_TRUE(infos.find(Uid("123")) != infos.end());
    }
}

TEST(Blackbox, AuthorizeAndFetchUserNamesIndividually)
{
    testFetchUserNames({}, {}, {}, {}, true);
    testFetchUserNames({Uid("123")}, {}, {}, {}, true);
    testFetchUserNames({Uid("12,3")}, {}, {}, {}, true);
    testFetchUserNames({Uid("123"), Uid("234")}, {}, {}, {}, true);
    testFetchUserNames({Uid("123"), Uid("234")}, {Uid("123")}, {}, {}, true);
    testFetchUserNames({Uid("123"), Uid("234")}, {}, {Uid("234")}, {}, true);
    testFetchUserNames(
        {Uid("123"), Uid("234")},
        {Uid("123")},
        {Uid("123"), Uid("234")},
        {},
        true);
    testFetchUserNames(
        {Uid("123"), Uid("234"), Uid("345")},
        {},
        {Uid("234")},
        {Uid("123")},
        true);
}

TEST(Blackbox, FetchUserNamesBatchedForDivisibleByBatchSizeUidsCount)
{
    std::unordered_set<Uid> uids, bannedUids, errorUids, missingUids;
    for (size_t i = 0;
        i < 10 * MAX_UIDS_COUNT_PER_BLACKBOX_REQUEST; ++i)
    {
        const auto uid = Uid(std::to_string((i + 96289) * 67073));
        uids.insert(uid);
        if (i % 13) {
            bannedUids.insert(uid);
        }
        if (i % 113) {
            errorUids.insert(uid);
        }
        if (i % 7) {
            missingUids.insert(uid);
        }
    }
    testFetchUserNames(uids, bannedUids, errorUids, missingUids, false);
}

TEST(Blackbox, FetchUserNamesBatchedForNonDivisibleByBatchSizeUidsCount)
{
    std::unordered_set<Uid> uids, bannedUids, errorUids, missingUids;
    for (size_t i = 0;
        i < 3 * MAX_UIDS_COUNT_PER_BLACKBOX_REQUEST - 1; ++i)
    {
        const auto uid = Uid(std::to_string((i + 96289) * 67073));
        uids.insert(uid);
        if (i % 13) {
            bannedUids.insert(uid);
        }
        if (i % 113) {
            errorUids.insert(uid);
        }
        if (i % 7) {
            missingUids.insert(uid);
        }
    }
    testFetchUserNames(uids, bannedUids, errorUids, missingUids, false);
}
