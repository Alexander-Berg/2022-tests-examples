#include <maps/sprav/callcenter/libs/company_searcher/filters.h>

#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/testing/gtest/gtest.h>


namespace maps::sprav::callcenter::company_searcher::tests {

TEST(TestFilters, ActualizedFilter) {
    EXPECT_TRUE(ActualizedFilter().asString().starts_with("i_actualized:<"));
}

TEST(TestFilters, PublishingStatusFilter) {
    EXPECT_EQ(
        PublishingStatusFilter(NSprav::PublishingStatus::PUBLISH).asString(),
        "s_publishing_status:PUBLISH"
    );
}

TEST(TestFilters, ChainFilter) {
    EXPECT_EQ(
        ChainFilter(1).asString(),
        "s_chain_parent_companies:1"
    );
}

TEST(TestFilters, PhoneFilter) {
    EXPECT_EQ(
        PhoneFilter("+7(999)999-99-99").asString(),
        "z_phones:(+7(999)999-99-99)"
    );
}

TEST(TestFilters, GeoIdFilter) {
    EXPECT_EQ(
        GeoIdFilter(1).asString(),
        "s_geo_id:1"
    );
}

TEST(TestFilters, IsExportableFilter) {
    EXPECT_EQ(
        IsExportableFilter().asString(),
        "(s_publishing_status:PUBLISH) | (s_publishing_status:UNCHECKED) | "
        "(s_publishing_status:CLOSED) | (s_publishing_status:NOT_ANSWERED) | "
        "(s_publishing_status:TEMPORARILY_CLOSED) | (s_publishing_status:MOVED) | "
        "(s_publishing_status:CLOSED_BY_PROVIDER)"
    );
}

TEST(TestFilters, ChainsFilter) {
    EXPECT_EQ(
        ChainsFilter(test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
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
        )")).asString(),
        "(s_chain_parent_companies:1) | (s_chain_parent_companies:2)"
    );
}


TEST(TestFilters, PhonesFilter) {
    EXPECT_EQ(
        PhonesFilter(test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            phones {
                formatted: "+7(999)999-99-99"
            }
            phones {
                formatted: "+7(999)999-99-98"
            }
            phones {
            }
        )")).asString(),
        "(z_phones:(+7(999)999-99-99)) | (z_phones:(+7(999)999-99-98))"
    );
}

} // maps::sprav::callcenter::task::tests
