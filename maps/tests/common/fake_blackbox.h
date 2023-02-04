#pragma once

#include <maps/infopoint/lib/auth/blackbox.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/http/include/urlencode.h>

#include <memory>
#include <regex>
#include <sstream>

namespace infopoint {

enum class BlackboxUserStatus {
    Regular,
    Banned
};

enum class BlackboxUserBehavior {
    Normal,
    AlwaysGenerateError
};

struct BlackboxUser {
    Uid uid;
    PublicUid publicUid;
    std::optional<std::string> token;
    std::string publicName;
    BlackboxUserStatus status = BlackboxUserStatus::Regular;
    BlackboxUserBehavior behavior = BlackboxUserBehavior::Normal;

    void json(maps::json::ObjectBuilder b) const;
};

// Fake local blackbox backend. Temporary in-memory users database.
class LocalBlackboxStorage {
public:
    void upsertUser(const BlackboxUser& user);
    void removeUser(const Uid& uid);
    std::optional<BlackboxUser> userinfo(const Uid& uid) const;
    std::unordered_map<Uid, BlackboxUser> userinfo(
        const std::vector<Uid>& uids) const;
    std::optional<BlackboxUser> authorize(const std::string& token) const;

private:
    std::unordered_map<Uid, BlackboxUser> uidToUser_;
    std::unordered_map<std::string, Uid> tokenToUid_;
};

// http client replacer that uses local blackbox storage to form responses
// to blackbox requests.
class LocalBlackboxFetcher {
public:
    LocalBlackboxFetcher(std::weak_ptr<LocalBlackboxStorage> storage);

    maps::json::Value operator()(
        const maps::http::URL& url,
        const maps::http::HeaderMap& headers) const;
    // Fakes blackbox's 'userinfo' method.
    maps::json::Value userinfo(const std::vector<Uid>& uids) const;

private:
    maps::json::Value errorResponse(const std::string& value,
        const int id, const std::string& message) const;

private:
    const std::weak_ptr<LocalBlackboxStorage> storage_;
};

} // namespace infopoint
