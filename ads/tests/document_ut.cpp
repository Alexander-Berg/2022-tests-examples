#include <library/cpp/testing/unittest/registar.h>
#include <yabs/server/util/bobhash.h>

#include <ads/bigkv/search/entities/document/document.h>


TString TEST_RALIB_URL = "https://otvet.mail.ru/question/212586631";
TString TEST_OMNI_TITLE = "ответы mail ru здравствуйте посмотрите пожалуйста что ждать в ближайшее время как обстоят дела с учебой";
TString TEST_RANDOM_LOG_UNPACKED_COMPRESSED_L3_FEATURES = "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";


namespace {
    void assertEqualVectors(TVector<ui64> a, TVector<ui64> b) {
        UNIT_ASSERT_EQUAL_C(a.size(), b.size(), TStringBuilder{} << a.size() << " != " << b.size());
        for (ui32 i = 0; i < a.size(); ++i) {
            UNIT_ASSERT_EQUAL_C(a[i], b[i], TStringBuilder{} << "a[" << i << "] = " << a[i] << " != " << b[i] << " = b[" << i << "]");
        }
    }

    // void assertEqualVectors(TVector<float> a, TVector<float> b) {
    //     UNIT_ASSERT_EQUAL_C(a.size(), b.size(), TStringBuilder{} << a.size() << " != " << b.size());
    //     for (ui32 i = 0; i < a.size(); ++i) {
    //         UNIT_ASSERT_DOUBLES_EQUAL_C(a[i], b[i], 0.0001, TStringBuilder{} << "a[" << i << "] = " << a[i] << " != " << b[i] << " = b[" << i << "]");
    //     }
    // }
}

class TDocumentEntityFeaturesTest : public TTestBase {
    public:
        void TestDocumentID() {
            assertEqualVectors(
                CategoricalFeatures[NSearchTsar::TDocument::DocumentID],
                {yabs_bobhash(TEST_RALIB_URL)}
            );
        }

        void TestDomainID() {
            assertEqualVectors(
                CategoricalFeatures[NSearchTsar::TDocument::DomainID],
                {yabs_bobhash("otvet.mail.ru")}
            );
        }

        void TestRalibUrlV1BertNormed() {
            assertEqualVectors(
                CategoricalFeatures[NSearchTsar::TDocument::RalibUrlV1BertNormed],
                {
                    10, 633, 115, 11, 597, 7561, 65206, 46896
                }
            );
        }

        void TestOmniTitleBertNormed() {
            assertEqualVectors(
                CategoricalFeatures[NSearchTsar::TDocument::OmniTitleBertNormed],
                {
                    195, 115, 11, 3393, 7787, 912, 32, 7258, 6, 10254, 151, 22, 70028, 1078, 2108, 12, 38489, 88
                }
            );
        }

        void SetUp() override {
            NSearchTsar::TTextualDocumentRepresentation docData = {
                {NSearchTsar::TDocument::RalibUrlInputColumn, TEST_RALIB_URL},
                {NSearchTsar::TDocument::OmniTitleInputColumn, TEST_OMNI_TITLE},
                {NSearchTsar::TDocument::WebFeaturesInputColumn, TEST_RANDOM_LOG_UNPACKED_COMPRESSED_L3_FEATURES}
            };

            auto tokenizer = NProfilePreprocessing::TFpmBpeTokenizer::CreateWithDefaultVocab();
            const auto& doc = NSearchTsar::TDocument(tokenizer, docData);
            CategoricalFeatures = doc.GetNamedFeatures();
            RealValueFeatures = doc.GetRealvalueFeatures();

        }

    private:
        THashMap<TString, TVector<ui64>> CategoricalFeatures;
        THashMap<TString, TVector<float>> RealValueFeatures;

        UNIT_TEST_SUITE(TDocumentEntityFeaturesTest);

        UNIT_TEST(TestDocumentID);
        UNIT_TEST(TestDomainID);
        UNIT_TEST(TestRalibUrlV1BertNormed);
        UNIT_TEST(TestOmniTitleBertNormed);
        UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TDocumentEntityFeaturesTest);
