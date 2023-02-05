#pragma once

#include <maps/infra/quotateka/libs/abc/abc.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/http/include/urlencode.h>
#include <maps/libs/json/include/value.h>

#include <util/string/join.h>
#include <util/string/split.h>

#include <unordered_set>

namespace maps::abc::tests {

class AbcFixture {
public:
    AbcFixture()
    {
        for (auto url: {Settings::ABC_PROD_URL, Settings::ABC_TEST_URL}) {
            httpMocks_.emplace_back(http::addMock(
                std::string{url} + "api/v4/services/members/",
                [this](const http::MockRequest& request) {
                    return membersRespond(request);
                }));
        }
        for (auto url: {Settings::ABC_PROD_URL, Settings::ABC_TEST_URL}) {
            httpMocks_.emplace_back(http::addMock(
                std::string{url} + "api/v4/services/",
                [this](const http::MockRequest& request) {
                    return serviceRespond(request);
                }));
        }
    }

    AbcFixture& addMember(
        const std::string& abcSlug,
        size_t abcId,
        uint64_t userId,
        const std::string& roleScope)
    {
        members_.emplace_back(std::make_tuple(abcSlug, userId, roleScope));
        servicesBySlug_.emplace(abcSlug, ServiceInfo{.id = abcId, .slug = abcSlug});
        servicesById_.emplace(abcId, ServiceInfo{.id = abcId, .slug = abcSlug});
        return *this;
    }

    AbcFixture& addService(const std::string& abcSlug, size_t abcId)
    {
        servicesBySlug_.emplace(abcSlug, ServiceInfo{.id = abcId, .slug = abcSlug});
        servicesById_.emplace(abcId, ServiceInfo{.id = abcId, .slug = abcSlug});
        return *this;
    }

private:
    http::MockResponse membersRespond(const http::MockRequest& request) const
    {
        std::vector<std::string> abcSlugs, scopeSlugs;
        std::vector<uint64_t> uids;

        if (auto value = request.url.optParam("person__uid__in"))
            StringSplitter(*value).Split(',').ParseInto(&uids);
        if (auto value = request.url.optParam<uint64_t>("person__uid"))
            uids.push_back(*value);
        if (auto value = request.url.optParam("role__scope__slug__in"))
            StringSplitter(*value).Split(',').ParseInto(&scopeSlugs);
        if (auto value = request.url.optParam("role__scope__slug"))
            scopeSlugs.push_back(*value);
        if (auto value = request.url.optParam("service__slug__in"))
            StringSplitter(*value).Split(',').ParseInto(&abcSlugs);
        if (auto value = request.url.optParam("service__slug"))
            abcSlugs.push_back(*value);

        std::vector<std::string> results;
        for (const auto& [abc, uid, scope]: members_) {
            // Lookup
            if (!abcSlugs.empty() &&
                std::find(abcSlugs.begin(), abcSlugs.end(), abc) ==
                    abcSlugs.end())
                continue;
            if (!scopeSlugs.empty() &&
                std::find(scopeSlugs.begin(), scopeSlugs.end(), scope) ==
                    scopeSlugs.end())
                continue;
            if (!uids.empty() &&
                std::find(uids.begin(), uids.end(), uid) == uids.end())
                continue;

            // We don't care about id values in result, so just set consecutive ints
            results.emplace_back(
                "{\"id\": " + std::to_string(results.size() + 1) + "}");
        }

        auto response = "{\"results\": [" + JoinSeq(",", results) +
                        "], \"next\":null,\"previous\":null}";
        return http::MockResponse(response);
    }

    http::MockResponse serviceRespond(const http::MockRequest& request)
    {
        std::vector<ServiceInfo> foundServices;
        auto requestSlug = request.url.optParam("slug");
        auto requestAbcId = request.url.optParam("id");

        if (requestSlug) {
            if (auto serviceInfoIter = servicesBySlug_.find(*requestSlug);
                serviceInfoIter != servicesBySlug_.end()) {
                foundServices.push_back(serviceInfoIter->second);
            }
        } else if (requestAbcId) {
            auto abcId = FromString<uint64_t>(*requestAbcId);
            if (auto serviceInfoIter = servicesById_.find(abcId);
                serviceInfoIter != servicesById_.end()) {
                foundServices.push_back(serviceInfoIter->second);
            }
        } else {
            for (const auto& [slug, service] : servicesBySlug_) {
                foundServices.push_back(service);
            }
        }

        json::Builder builder;
        builder << [&](maps::json::ObjectBuilder json) {
            json["results"] << [&](maps::json::ArrayBuilder results) {
                for (const auto& service : foundServices) {
                    results << service;
                }
            };
            json["next"] = json::null;
            json["previous"] = json::null;
        };
        return http::MockResponse(builder.str());
    }

    std::vector<http::MockHandle> httpMocks_;
    // collection of tuple<abcSlug, uid, scopeSlug>
    std::vector<std::tuple<std::string, uint64_t, std::string>> members_;
    std::unordered_map<std::string, ServiceInfo> servicesBySlug_;
    std::unordered_map<uint64_t, ServiceInfo> servicesById_;
};

} // namespace maps::abc::tests
