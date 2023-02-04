#include <ads/bigkv/preprocessing/utils/TextPreprocessing.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/iterator/zip.h>
#include <library/cpp/iterator/functools.h>


namespace TextPreprocessing {
    Y_UNIT_TEST_SUITE(TextPreprocessingTests) {
        Y_UNIT_TEST(TokenTest) {
            TTokenLemma tokenizer("out", false);
            auto result = tokenizer.ComputeFactors("Специализированный магазин по продаже эхолотов и GPS навигаторов. Большой выбор.");
            TVector<TString> trueOut = {"специализированный", "магазин", "по", "продаже", "эхолотов", "и", "gps", "навигаторов", "большой", "выбор", "."};

            UNIT_ASSERT_EQUAL(trueOut.size(), result["out"].AsList().size());
            for (const auto &[trueToken, actualToken]: Zip(trueOut, result["out"].AsList())) {
                UNIT_ASSERT_EQUAL(trueToken, actualToken.AsString());
            }
        }

        Y_UNIT_TEST(TokenLemmaTest) {
            TTokenLemma tokenizer("out", true);
            auto result = tokenizer.ComputeFactors("Специализированный магазин по продаже эхолотов и GPS навигаторов. Большой выбор.");
            TVector<TString> trueOut = {"специализированный", "магазин", "по", "продажа", "эхолот", "и", "gps", "навигатор", "большой", "выбор", "."};

            UNIT_ASSERT_EQUAL(trueOut.size(), result["out"].AsList().size());
            for (const auto &[trueToken, actualToken]: Zip(trueOut, result["out"].AsList())) {
                UNIT_ASSERT_EQUAL(trueToken, actualToken.AsString());
            }
        }

        Y_UNIT_TEST(NGramTest) {
            TNGrams ngram("out", 2);
            NYT::TNode::TListType input;
            for (const auto &x: {"выкуп", "брендовый", "час", " ", "дорого", "!"}) {
                input.push_back(NYT::TNode(x));
            }
            TVector<TString> trueOut = {"выкуп#брендовый", "брендовый#час", "час# ", " #дорого", "дорого#!"};

            auto result = ngram.ComputeFactors(input);

            for (const auto &[trueToken, actualToken]: Zip(trueOut, result["out"].AsList())) {
                UNIT_ASSERT_EQUAL(trueToken, actualToken.AsString());
            }
        }
    }

    Y_UNIT_TEST_SUITE(URLPreprocessingTests) {
        Y_UNIT_TEST(Simple) {
            TString url = "http://georgiatravel.ru/tur_v_gruziyu_na_7dnei?utm_source=yandex&utm_medium=cpc&utm_campaign=45801716&utm_content=7985293976&utm_term={PHRASE}.{DEVICE_TYPE}.{REGN_BS}.{PARAM126}.{PHRASE_BM}&block={PTYPE}.{POS}";
            TVector<TString> trueHost = {"georgiatravel", "ru"};
            TVector<TString> truePath = {"tur_v_gruziyu_na_7dnei"};
            TVector<TVector<TString>> trueQueries = {{"utm_source", "yandex"}, {"utm_medium", "cpc"}, {"utm_campaign", "45801716"}, {"utm_content", "7985293976"}, {"utm_term", "{PHRASE}.{DEVICE_TYPE}.{REGN_BS}.{PARAM126}.{PHRASE_BM}"}, {"block", "{PTYPE}.{POS}"}};

            TURLParse parser("out");
            auto result = parser.ComputeFactors(url);

            UNIT_ASSERT_EQUAL(trueHost.size(), result["outHost"].AsList().size());
            for (const auto &[trueToken, actualToken]: Zip(trueHost, result["outHost"].AsList())) {
                UNIT_ASSERT_EQUAL(trueToken, actualToken.AsString());
            }

            UNIT_ASSERT_EQUAL(truePath.size(), result["outPath"].AsList().size());
            for (const auto &[trueToken, actualToken]: Zip(truePath, result["outPath"].AsList())) {
                UNIT_ASSERT_EQUAL(trueToken, actualToken.AsString());
            }

            UNIT_ASSERT_EQUAL(trueQueries.size(), result["outQuery"].AsList().size());
            for (const auto &[trueQuery, actualQuery]: Zip(trueQueries, result["outQuery"].AsList())) {
                UNIT_ASSERT_EQUAL(actualQuery.AsList().size(), 2);
                for (const auto &[trueToken, actualToken]: Zip(trueQuery, actualQuery.AsList())) {
                    UNIT_ASSERT_EQUAL(trueToken, actualToken.AsString());
                }
            }
        }
    }
}

