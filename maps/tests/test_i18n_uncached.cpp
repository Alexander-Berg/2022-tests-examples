#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/i18n.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/mocks.h>

namespace maps::automotive::store_internal {

namespace {

struct CtxMock: public AppContext {
    CtxMock()
        : AppContext(getConfig())
        , tankerMock("")
    {
        {
            auto mock = http::addMock(
                "https://tanker-beta.yandex-team.ru/api/legacy/keysets/json/",
                [](const http::MockRequest&) {
                    return std::string("{}");
                });
            initTanker();
        }
        tankerMock = http::addMock(
            "https://tanker-beta.yandex-team.ru/api/legacy/keysets/json/",
            [](const http::MockRequest& req) {
                if (req.url.params().find("packages_titles") != std::string::npos) {
                    return std::string(
                        R"({"ru": {"superapp": "nazvanie"}})");
                }
                if (req.url.params().find("firmware_titles") != std::string::npos) {
                    return std::string(
                        R"({"ru": {"fw/1": "proshivka"}})");
                }
                return std::string("{}");
            });
    }

public:
    http::MockHandle tankerMock;
};

struct TranslationsUncached: public ::testing::Test {
    TranslationsUncached() {
        g_ctx.reset(new CtxMock());
    }

    ~TranslationsUncached() {
        g_ctx.reset();
    }
};

} // namespace

TEST_F(TranslationsUncached, appendTranslationsPackage)
{
    Package pkg;
    appendTranslations(pkg);
    ASSERT_EQ(0, pkg.metadata().release_notes_size());
    ASSERT_EQ(0, pkg.metadata().title_size());

    pkg.mutable_metadata()->set_app_name("superapp");
    appendTranslations(pkg);
    ASSERT_EQ(0, pkg.metadata().release_notes_size());
    ASSERT_EQ(1, pkg.metadata().title_size());
    
    EXPECT_EQ("nazvanie", pkg.metadata().title().at("ru"));

    (*pkg.mutable_metadata()->mutable_title())["ru"] = "rr";
    (*pkg.mutable_metadata()->mutable_title())["en"] = "ee";
    auto before = printToString(pkg);
    appendTranslations(pkg);
    EXPECT_EQ(before, printToString(pkg));
}

TEST_F(TranslationsUncached, appendTranslationsFirmware)
{
    Firmware fw;
    appendTranslations(fw);
    ASSERT_EQ(0, fw.metadata().release_notes_size());
    ASSERT_EQ(0, fw.metadata().title_size());

    fw.mutable_id()->set_name("fw");
    fw.mutable_id()->set_version("1");
    appendTranslations(fw);
    ASSERT_EQ(0, fw.metadata().release_notes_size());
    ASSERT_EQ(1, fw.metadata().title_size());
    EXPECT_EQ("proshivka", fw.metadata().title().at("ru"));

    (*fw.mutable_metadata()->mutable_title())["ru"] = "rr";
    (*fw.mutable_metadata()->mutable_title())["en"] = "ee";
    auto before = printToString(fw);
    appendTranslations(fw);
    EXPECT_EQ(before, printToString(fw));
}

}
