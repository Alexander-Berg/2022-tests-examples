#include <maps/sprav/callcenter/libs/company_searcher/company_searcher.h>

#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/http/misc/httpcodes.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <search/idl/meta.pb.h>


using ::testing::MatchesRegex;
using ::testing::UnorderedElementsAre;

namespace maps::sprav::callcenter::company_searcher::tests {

namespace {
std::string unifyActualized(std::string s) {
    std::string replacement = "i_actualized:<0000000000";
    s.replace(s.find("i_actualized"), replacement.size(), replacement);
    return s;
}

} // namespace


TEST(CompanySearcher, ChainCompany) {
    CompanySearcher searcher{"http://saas-test/", "43", {}};

    auto mockHandle = maps::http::addMock(
        "http://saas-test/",
        [](const maps::http::MockRequest& request) {
            EXPECT_EQ(request.method, http::GET);
            EXPECT_EQ(request.url.param("kps"), "43");
            EXPECT_EQ(request.url.param("relev"), "attr_limit=9999999");
            EXPECT_EQ(request.url.param("how"), "docid");
            EXPECT_EQ(request.url.param("ms"), "proto");
            EXPECT_EQ(request.url.param("numdoc"), "10");
            EXPECT_EQ(request.url.param("wizextra"), "usesoftness=da;usextsyntax=da");
            EXPECT_EQ(request.url.param("rwr"), "off:Text");
            EXPECT_EQ(unifyActualized(request.url.param("template")),
                "(((i_actualized:<0000000000) && "
                "((s_publishing_status:PUBLISH) | (s_publishing_status:UNCHECKED) | "
                "(s_publishing_status:CLOSED) | (s_publishing_status:NOT_ANSWERED) | "
                "(s_publishing_status:TEMPORARILY_CLOSED) | (s_publishing_status:MOVED) | "
                "(s_publishing_status:CLOSED_BY_PROVIDER)) && "
                "((s_chain_parent_companies:1) | (s_chain_parent_companies:2))) "
                "%request%) <- \"%request%\""
            );

            auto response = maps::http::MockResponse::withStatus(HTTP_OK);
            response.body = test_helpers::protoFromTextFormat<NMetaProtocol::TReport>(
                NResource::Find("data/response_chain.pb.txt")
            ).SerializeAsString();
            return response;
        }
    );

    auto companies = searcher.findHypotheses(
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            parent_companies {
                type: IsPartOf
                company_id: 1
            }
            parent_companies {
                type: IsPartOf
                company_id: 2
            }
            parent_companies {
                type: LocatedAt
                company_id: 5
            }
        )"), 10
    );
    EXPECT_THAT(companies, UnorderedElementsAre(
        1329225097, 4577235444, 150884597433, 212062873144, 54708443826,
        244088209746, 1326693480, 34723902375, 1362514690, 1351377854
    ));
}


TEST(CompanySearcher, PhoneCompany) {
    CompanySearcher searcher{"http://saas-test/", "43", {}};

    auto mockHandle = maps::http::addMock(
        "http://saas-test/",
        [](const maps::http::MockRequest& request) {
            EXPECT_EQ(request.method, http::GET);
            EXPECT_EQ(request.url.param("kps"), "43");
            EXPECT_EQ(request.url.param("relev"), "attr_limit=9999999");
            EXPECT_EQ(request.url.param("how"), "docid");
            EXPECT_EQ(request.url.param("ms"), "proto");
            EXPECT_EQ(request.url.param("numdoc"), "10");
            EXPECT_EQ(request.url.param("wizextra"), "usesoftness=da;usextsyntax=da");
            EXPECT_EQ(request.url.param("rwr"), "off:Text");
            EXPECT_EQ(unifyActualized(request.url.param("template")),
                "(((i_actualized:<0000000000) && "
                "((s_publishing_status:PUBLISH) | (s_publishing_status:UNCHECKED) | "
                "(s_publishing_status:CLOSED) | (s_publishing_status:NOT_ANSWERED) | "
                "(s_publishing_status:TEMPORARILY_CLOSED) | (s_publishing_status:MOVED) | "
                "(s_publishing_status:CLOSED_BY_PROVIDER)) && "
                "((z_phones:(+7(999)999-99-99)) | (z_phones:(+7(999)999-99-98)))) "
                "%request%) <- \"%request%\""
            );

            auto response = maps::http::MockResponse::withStatus(HTTP_OK);
            response.body = test_helpers::protoFromTextFormat<NMetaProtocol::TReport>(
                NResource::Find("data/response_empty.pb.txt")
            ).SerializeAsString();
            return response;
        }
    );

    auto companies = searcher.findHypotheses(
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            phones {
                formatted: "+7(999)999-99-99"
            }
            phones {
                formatted: "+7(999)999-99-98"
            }
            phones {
            }
        )"), 10
    );
    EXPECT_TRUE(companies.empty());
}


TEST(CompanySearcher, PhoneCompanyWithGeoId) {
    CompanySearcher searcher{"http://saas-test/", "43", {}};

    auto mockHandle = maps::http::addMock(
        "http://saas-test/",
        [](const maps::http::MockRequest& request) {
            EXPECT_EQ(request.method, http::GET);
            EXPECT_EQ(request.url.param("kps"), "43");
            EXPECT_EQ(request.url.param("relev"), "attr_limit=9999999");
            EXPECT_EQ(request.url.param("how"), "docid");
            EXPECT_EQ(request.url.param("ms"), "proto");
            EXPECT_EQ(request.url.param("numdoc"), "10");
            EXPECT_EQ(request.url.param("wizextra"), "usesoftness=da;usextsyntax=da");
            EXPECT_EQ(request.url.param("rwr"), "off:Text");
            EXPECT_EQ(unifyActualized(request.url.param("template")),
                "(((i_actualized:<0000000000) && "
                "((s_publishing_status:PUBLISH) | (s_publishing_status:UNCHECKED) | "
                "(s_publishing_status:CLOSED) | (s_publishing_status:NOT_ANSWERED) | "
                "(s_publishing_status:TEMPORARILY_CLOSED) | (s_publishing_status:MOVED) | "
                "(s_publishing_status:CLOSED_BY_PROVIDER)) && "
                "((z_phones:(+7(999)999-99-99)) | (z_phones:(+7(999)999-99-98))) && (s_geo_id:123)) "
                "%request%) <- \"%request%\""
            );

            auto response = maps::http::MockResponse::withStatus(HTTP_OK);
            response.body = test_helpers::protoFromTextFormat<NMetaProtocol::TReport>(
                NResource::Find("data/response_empty.pb.txt")
            ).SerializeAsString();
            return response;
        }
    );

    auto companies = searcher.findHypotheses(
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            address {
                geo_id: 123
            }
            phones {
                formatted: "+7(999)999-99-99"
            }
            phones {
                formatted: "+7(999)999-99-98"
            }
            phones {
            }
        )"), 10
    );
    EXPECT_TRUE(companies.empty());
}
} // maps::sprav::callcenter::task::tests
