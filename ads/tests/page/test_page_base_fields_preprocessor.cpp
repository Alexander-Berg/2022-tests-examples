#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/page_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/page_preprocessors/veniamin_features_preprocessors.h>
#include <library/cpp/iterator/zip.h>
#include <yabs/server/util/bobhash.h>


namespace NProfilePreprocessing {

class TPageBaseFieldsPreprocessorsTests : public TTestBase {
public:
    void BaseFieldsTest() {
        TVeniaminBasePageFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});
        auto schema = preproc.Schema();

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), schema.size());
        for (const auto& [name, _]: schema) {
            UNIT_ASSERT(actualResult.contains(name));
        }

        UNIT_ASSERT(actualResult.contains("PageID"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["PageID"].AsUint64(), 1);
        UNIT_ASSERT(actualResult.contains("ImpID"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["ImpID"].AsUint64(), 2);
        UNIT_ASSERT(actualResult.contains("PageID,ImpID"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["PageID,ImpID"].AsUint64(), 27942141 * 1 + 2);
        UNIT_ASSERT(actualResult.contains("SSPID"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["SSPID"].AsUint64(), 3);
        UNIT_ASSERT(actualResult.contains("PageImp"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["PageImp"].AsUint64(), 100002);
        UNIT_ASSERT(actualResult.contains("PageDomainMD5"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["PageDomainMD5"].AsUint64(), 4);
        UNIT_ASSERT(actualResult.contains("PageTokenTagIDMD5"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["PageTokenTagIDMD5"].AsUint64(), 5);
        UNIT_ASSERT(actualResult.contains("Referer"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["Referer"].AsString(), "page_url");
        UNIT_ASSERT(actualResult.contains("ReReferer"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["ReReferer"].AsString(), "page_rereferer");
    }


    void UserAgentTest() {
        TVeniaminUserAgentPageFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});
        auto schema = preproc.Schema();

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), schema.size());
        for (const auto& [name, _]: schema) {
            UNIT_ASSERT(actualResult.contains(name));
        }

        UNIT_ASSERT(actualResult.contains("agent_bow"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].Size(), 10);

        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[0].AsUint64(), yabs_bobhash("0_3"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[1].AsUint64(), yabs_bobhash("1_2"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[2].AsUint64(), yabs_bobhash("2_1"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[3].AsUint64(), yabs_bobhash("3_6"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[4].AsUint64(), yabs_bobhash("4_5"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[5].AsUint64(), yabs_bobhash("5_4"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[6].AsUint64(), yabs_bobhash("6_10"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[7].AsUint64(), yabs_bobhash("7_9"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[8].AsUint64(), yabs_bobhash("8_8"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["agent_bow"].AsList()[9].AsUint64(), yabs_bobhash("9_7"));
    }

    void LayoutTest() {
        TVeniaminLayoutPageFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});
        auto schema = preproc.Schema();

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), schema.size());
        for (const auto& [name, _]: schema) {
            UNIT_ASSERT(actualResult.contains(name));
        }

        UNIT_ASSERT(actualResult.contains("AdNo"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AdNo"].AsUint64(), 18 + 1);
        UNIT_ASSERT(actualResult.contains("AdsLimit"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["AdsLimit"].AsUint64(), 9 + 1);
        UNIT_ASSERT(actualResult.contains("VisibleAd"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["VisibleAd"].AsUint64(), 20 + 1);
        UNIT_ASSERT(actualResult.contains("BlockOffsetTop"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BlockOffsetTop"].AsUint64(), 10 + 1);
        UNIT_ASSERT(actualResult.contains("BlockOffsetLeft"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BlockOffsetLeft"].AsUint64(), 10 + 1);
        UNIT_ASSERT(actualResult.contains("BlockTypeID"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BlockTypeID"].AsUint64(), 11);
        UNIT_ASSERT(actualResult.contains("DistanceToBlock"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["DistanceToBlock"].AsUint64(), 1);
        UNIT_ASSERT(actualResult.contains("BlockWidth"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BlockWidth"].AsUint64(), 1 + 1 + 1);
        UNIT_ASSERT(actualResult.contains("BlockHeight"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BlockHeight"].AsUint64(), 1 + 1 + 1);
        UNIT_ASSERT(actualResult.contains("BlockArea"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["BlockArea"].AsUint64(), 14);
        UNIT_ASSERT(actualResult.contains("WinHeight"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["WinHeight"].AsUint64(), 1 + 1);
        UNIT_ASSERT(actualResult.contains("WinWidth"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["WinWidth"].AsUint64(), 1 + 1);
        UNIT_ASSERT(actualResult.contains("WinArea"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["WinArea"].AsUint64(), 14);
        UNIT_ASSERT(actualResult.contains("TitleSize"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["TitleSize"].AsUint64(), 9);
        UNIT_ASSERT(actualResult.contains("TitleBold"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["TitleBold"].AsUint64(), 23);
    }

    void DateTimeTest() {
        TVeniaminDateTimeFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), 1622715867);
        auto schema = preproc.Schema();

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), schema.size());
        for (const auto& [name, _]: schema) {
            UNIT_ASSERT(actualResult.contains(name));
        }

        UNIT_ASSERT(actualResult.contains("WeekDay"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["WeekDay"].AsUint64(), 4);
        UNIT_ASSERT(actualResult.contains("HourDay"));
        UNIT_ASSERT_VALUES_EQUAL(actualResult["HourDay"].AsUint64(), 13);
    }

    void SetUp() override {
        ProfileBuilder.AddBaseFields(
            1, 2, 3, 4, 5, "page_url", "page_rereferer"
        );
        ProfileBuilder.AddUAFields(
            3, 2, 1, 6, 5, 4, 10, 9, 8, 7
        );
        ProfileBuilder.AddLayout(
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22
        );
    }

private:
    TPageProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TPageBaseFieldsPreprocessorsTests);
    UNIT_TEST(BaseFieldsTest);
    UNIT_TEST(UserAgentTest);
    UNIT_TEST(LayoutTest);
    UNIT_TEST(DateTimeTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TPageBaseFieldsPreprocessorsTests);
}
