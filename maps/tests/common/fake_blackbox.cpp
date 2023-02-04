#include "fake_blackbox.h"

namespace infopoint {

namespace {

std::vector<std::string> split(const std::string& s,
    const char delimiter)
{
    std::vector<std::string> tokens;
    std::istringstream iss(s);
    for (std::string token; std::getline(iss, token, delimiter); ) {
        tokens.push_back(token);
    }
    return tokens;
}

} // namespace

void BlackboxUser::json(maps::json::ObjectBuilder b) const
{
    namespace json = maps::json;

    b["id"] = uid.value();

    if (behavior == BlackboxUserBehavior::AlwaysGenerateError) {
        b["exception"] << [](json::ObjectBuilder b) {
            b["value"] = "DB_EXCEPTION";
            b["id"] = 10;
        };
        b["error"] = "fetching user error";
        return;
    }

    b["uid"] << [&](json::ObjectBuilder b) {
        b["value"] = uid.value();
    };
    b["display_name"] << [&](json::ObjectBuilder b) {
        b["public_name"] = publicName;
    };
    b["public_id"] = publicUid.value();
    b["attributes"] << [&](json::ObjectBuilder b) {
        if (status != BlackboxUserStatus::Banned) {
            b["1009"] << "1";
        }
    };
}

void LocalBlackboxStorage::upsertUser(const BlackboxUser& user)
{
    removeUser(user.uid);

    uidToUser_.emplace(user.uid, user);
    if (user.token) {
        ASSERT(tokenToUid_.find(user.token.value()) == tokenToUid_.end());
        tokenToUid_[user.token.value()] = user.uid;
    }
}

void LocalBlackboxStorage::removeUser(const Uid& uid)
{
    const auto it = uidToUser_.find(uid);
    if (it == uidToUser_.end()) {
        return;
    }
    if (it->second.token) {
        tokenToUid_.erase(it->second.token.value());
    }
    uidToUser_.erase(it);
}

std::optional<BlackboxUser> LocalBlackboxStorage::userinfo(
    const Uid& uid) const
{
    const auto it = uidToUser_.find(uid);
    return it == uidToUser_.end() 
        ? std::nullopt
        : std::make_optional(it->second);
}

std::unordered_map<Uid, BlackboxUser> LocalBlackboxStorage::userinfo(
    const std::vector<Uid>& uids) const
{
    std::unordered_map<Uid, BlackboxUser> users;
    for (const auto& uid : uids) {
        if (auto user = userinfo(uid); user) {
            users[uid] = std::move(user.value());
        }
    }
    return users;
}

std::optional<BlackboxUser> LocalBlackboxStorage::authorize(
    const std::string& token) const
{
    const auto it = tokenToUid_.find(token);
    return it == tokenToUid_.end()
        ? std::nullopt
        : std::make_optional(uidToUser_.find(it->second)->second);
}

LocalBlackboxFetcher::LocalBlackboxFetcher(
    std::weak_ptr<LocalBlackboxStorage> storage)
    : storage_(std::move(storage))
{ }

maps::json::Value LocalBlackboxFetcher::operator()(
    const maps::http::URL& url,
    const maps::http::HeaderMap& /*headers*/) const
{
    namespace json = maps::json;

    const auto userip = url.optParam("userip");
    if (!userip) {
       return errorResponse("INVALID_PARAMS", 2, "missing 'userip'");
    }

    const auto method = url.optParam("method");
    if (!method) {
       return errorResponse("INVALID_PARAMS", 2, "missing 'method'");
    }

    if (*method == "userinfo") {
        const auto attributes = url.optParam("attributes");
        if (!attributes) {
            return errorResponse("INVALID_PARAM", 2,
                "expecting 'attributes' arg");
        }
        static const auto re = std::regex("(^|.*,)1009($|,.*)");
        if (!std::regex_match(*attributes, re)) {
            return errorResponse("INVALID_PARAM", 2,
                "expecting '1009' in 'attributes' arg list");
        }

        const auto uidList = url.optParam("uid");
        if (!uidList) {
            return errorResponse("INVALID_PARAMS", 2, "missing 'uid'");
        }

        std::vector<Uid> uids;
        for (auto uid : split(*uidList, ','))
            uids.push_back(Uid(uid));
        return userinfo(uids);
    }

    return errorResponse("INVALID_PARAMS", 2, "unknown method");
}

maps::json::Value LocalBlackboxFetcher::userinfo(
    const std::vector<Uid>& uids) const
{
    namespace json = maps::json;
    const auto uidToUser = storage_.lock()->userinfo(uids);
    json::Builder response;
    response << [&](json::ObjectBuilder b) {
        b["users"] << [&](json::ArrayBuilder b) {
            for (const auto& uid : uids) {
                const auto userIt = uidToUser.find(uid);
                if (userIt == uidToUser.end()) {
                    b << [&](json::ObjectBuilder b) {
                        b["id"] << uid.value();
                    };
                    continue;
                }
                b << userIt->second;
            }
        };
    };
    return json::Value::fromString(response.str());
}

maps::json::Value LocalBlackboxFetcher::errorResponse(
    const std::string& value, const int id, const std::string& message) const
{
    namespace json = maps::json;
    json::Builder response;
    response << [&](json::ObjectBuilder b) {
        b["exception"] << [&](json::ObjectBuilder b) {
            b["value"] = value;
            b["id"] = id;
        };
        b["error"] = message;
    };
    return json::Value::fromString(response.str());
}

} // namespace infopoint
