#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/user_preprocessors/search_history_preprocessors.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TSearchHistoryPreprocessorsTests : public TTestBase {
public:

    void SearchQueryTest() {
        TSearchQueryComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), Timestamp);

        UNIT_ASSERT_EQUAL(actualResult["SearchQueryText"].AsString(), "Hello world search query");
        UNIT_ASSERT_EQUAL(actualResult["SearchQueryRegion"].AsList().size(), 1);
        UNIT_ASSERT_EQUAL(actualResult["SearchQueryRegion"].AsList()[0].AsUint64(), 123);
        UNIT_ASSERT_EQUAL(actualResult["SearchQueryPositionalEmbedding"].AsList()[0].AsUint64(), 1);
    }

    void SearchQueryHistoryTest() {
        TSearchQueryHistoryComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), Timestamp);

        TVector<TString> texts = {"asd df", "qwer ksmfg"};
        TVector<i64> regions = {12, 34};
        TVector<double> ts = {56, 78};

        UNIT_ASSERT_VALUES_EQUAL(actualResult["QueryHistoryTexts"].AsList().size(), texts.size());
        for (const auto &[trueValue, actualValue]: Zip(texts, actualResult["QueryHistoryTexts"].AsList())) {
            UNIT_ASSERT_VALUES_EQUAL(trueValue, actualValue.AsString());
        }

        UNIT_ASSERT_VALUES_EQUAL(actualResult["QueryHistoryRegions"].AsList().size(), regions.size());
        for (const auto &[trueValue, actualValue]: Zip(regions, actualResult["QueryHistoryRegions"].AsList())) {
            UNIT_ASSERT_VALUES_EQUAL(trueValue, actualValue.AsUint64());
        }

        UNIT_ASSERT_VALUES_EQUAL(actualResult["QueryHistoryFactors"].AsList().size(), ts.size());
        for (const auto &[trueValue, actualValue]: Zip(ts, actualResult["QueryHistoryFactors"].AsList())) {
            UNIT_ASSERT_VALUES_EQUAL(trueValue, actualValue.AsDouble());
        }
    }

    void SearchDocumentHistoryTest() {
        TSearchDocumentHistoryComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), Timestamp);

        TVector<TString> titles = {"asd df", "qwer ksmfg"}, urls = {"http://asdf.df", "www.qwer.ru/ksmfg"};
        TVector<TVector<double>> factors = {
            {56, 66, 12},
            {78, 88, 34}
        };

        UNIT_ASSERT_VALUES_EQUAL(actualResult["DocumentHistoryTitles"].AsList().size(), titles.size());
        for (const auto &[trueValue, actualValue]: Zip(titles, actualResult["DocumentHistoryTitles"].AsList())) {
            UNIT_ASSERT_VALUES_EQUAL(trueValue, actualValue.AsString());
        }

        UNIT_ASSERT_VALUES_EQUAL(actualResult["DocumentHistoryUrls"].AsList().size(), urls.size());
        for (const auto &[trueValue, actualValue]: Zip(urls, actualResult["DocumentHistoryUrls"].AsList())) {
            UNIT_ASSERT_VALUES_EQUAL(trueValue, actualValue.AsString());
        }

        UNIT_ASSERT_VALUES_EQUAL(actualResult["DocumentHistoryFactors"].AsList().size(), factors.size());
        for (const auto &[trueList, actualList]: Zip(factors, actualResult["DocumentHistoryFactors"].AsList())) {
            UNIT_ASSERT_VALUES_EQUAL(actualList.AsList().size(), trueList.size());
            for (const auto &[trueValue, actualValue]: Zip(trueList, actualList.AsList())) {
                UNIT_ASSERT_VALUES_EQUAL(trueValue, actualValue.AsDouble());
            }
        }
    }

    void SetUp() override {
        Timestamp = 1601988640;

        ProfileBuilder.AddSearchQuery("Hello world search query", 123);
        ProfileBuilder.AddSearchQueryHistory({"asd df", "qwer ksmfg"}, {12, 34}, {Timestamp - 56, Timestamp - 78});
        ProfileBuilder.AddSearchDocumentHistory(
            {"asd df", "qwer ksmfg"}, {"http://asdf.df", "www.qwer.ru/ksmfg"},
            {12, 34},
            {Timestamp - 56, Timestamp - 78}, {Timestamp - 66, Timestamp - 88}
        );
    }

private:
    ui64 Timestamp;
    TUserProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TSearchHistoryPreprocessorsTests);
    UNIT_TEST(SearchQueryTest);
    UNIT_TEST(SearchQueryHistoryTest);
    UNIT_TEST(SearchDocumentHistoryTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TSearchHistoryPreprocessorsTests);
}
