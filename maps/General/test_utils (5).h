#pragma once

#include <maps/infra/quotateka/libs/abcd/abcd.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/http/include/urlencode.h>
#include <maps/libs/json/include/value.h>
#include <contrib/libs/re2/re2/re2.h>

#include <util/string/join.h>
#include <util/string/split.h>

#include <maps/libs/log8/include/log8.h>

#include <unordered_set>

namespace maps::abcd::tests {

struct FolderInfo {
    std::string id;
    int64_t version{0};
    std::string folderType{DEFAULT_FOLDER_TYPE};
    std::string displayName{"default"};
    std::string description{};
    bool deleted = false;

    void json(maps::json::VariantBuilder b) const
    {
        b << [this](maps::json::ObjectBuilder b) {
            b["id"] << id;
            b["version"] << version;
            b["folderType"] << folderType;
            b["displayName"] << displayName;
            b["description"] << description;
            b["deleted"] << deleted;
            b["tags"] << [](maps::json::ArrayBuilder) {};
        };
    }
};

class AbcdFixture {
public:
    AbcdFixture() = default;

    AbcdFixture& addServiceFolder(uint64_t abcId, FolderInfo folderInfo)
    {
        foldersByAbcId_.emplace(abcId, std::move(folderInfo));
        for (auto url: {Settings::ABCD_PROD_URL, Settings::ABCD_TEST_URL}) {
            httpMocks_.emplace_back(http::addMock(
                fmt::format(
                    "{}api/v1/services/{}/folders",
                    url,
                    abcId),
                [this](const http::MockRequest& request) {
                    return servicesRespond(request);
                }));
        }
        return *this;
    }

private:
    http::MockResponse servicesRespond(const http::MockRequest& request) const
    {
        static const RE2 pattern("/api/v1/services/(\\w+)/folders");
        std::string abcIdString;
        re2::StringPiece path(request.url.path());
        if (!RE2::FindAndConsume(&path, pattern, &abcIdString)) {
            throw RuntimeError(
                "request.url=" + request.url.path() +
                " doesn't match pattern=" + path.as_string());
        }

        auto abcId = FromString<uint64_t>(abcIdString);
        FolderInfo folder;
        try {
            folder = foldersByAbcId_.at(abcId);
        } catch (const std::out_of_range&) {
            return http::MockResponse("{\"items\": []}");
        }

        auto response = "{\"items\": [" + (json::Builder() << folder).str() + "]}";
        return http::MockResponse(response);
    }

    std::vector<http::MockHandle> httpMocks_;
    // TODO: Use std::vector<FolderInfo> once support for multiple folders
    // is implemented
    std::unordered_map<uint64_t, FolderInfo> foldersByAbcId_;
};

} // namespace maps::abcd::tests
