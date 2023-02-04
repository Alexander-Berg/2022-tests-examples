#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/page_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/page_preprocessors/grab_preprocessor.h>
#include <library/cpp/iterator/zip.h>
#include <yabs/server/util/bobhash.h>


namespace NProfilePreprocessing {

class TPageGrabPreprocessorsTests : public TTestBase {
public:
    void GrabTextsTest() {
        TGrabFactorsComputer preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});
        auto schema = preproc.Schema();

        UNIT_ASSERT_VALUES_EQUAL(actualResult.size(), schema.size());
        for (const auto& [name, _]: schema) {
            UNIT_ASSERT(actualResult.contains(name));
        }

        UNIT_ASSERT(actualResult.contains("GrabTexts"));
        UNIT_ASSERT(actualResult.contains("GrabMarkups"));

        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").Size(), 6);
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").Size(), 6);

        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").AsList()[0].AsString(), "Власти Москвы разъяснили права отказавшихся от вакцинации: Яндекс.Новости");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").AsList()[1].AsString(), "Власти Москвы разъяснили права отказавшихся от вакцинации");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").AsList()[2].AsString(), "Подробнее о событии");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").AsList()[3].AsString(), "Власти Москвы назвали условия получения QR-кодов");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").AsList()[4].AsString(), "ВОЗ одобрила применение прививки против COVID-19 у подростков");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabTexts").AsList()[5].AsString(), "В Москве за сутки выявлено 8598 случаев коронавир");

        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").AsList()[0].AsString(), "tag#title");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").AsList()[1].AsString(), "tag#h1");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").AsList()[2].AsString(), "tag#h2");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").AsList()[3].AsString(), "tag#h2");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").AsList()[4].AsString(), "tag#h2");
        UNIT_ASSERT_VALUES_EQUAL(actualResult.at("GrabMarkups").AsList()[5].AsString(), "tag#h2");

    }

    void GrabTextsLimit() {
        TPageProtoBuilder BigProfileBuilder;
        TString rawString = "dGNvbS5teXdlYXRoZXJtb24ud2VhdGhlcm5vdy5NYWluQWN0aXZpdHkKM9CS0YvQsdGA0LDRgtGMINCz0L7RgNC-0LQKM9Cd0LDRgdGC0YDQvtC50LrQuAoz0JPQuNGB0LzQtdGC0LXQvgoz0K8u0J_QvtCz0L7QtNCwCjPQoNCw0LTQsNGACjPQndC40LbQvdC40Lkg0J3QvtCy0LPQvtGA0L7QtAozKzMwwrAKM9Cv0YHQvdC-CjPQoi4g0LLQvtC00Ys6IDIxIEPCsAoz0JTQsNCy0LvQtdC90LjQtTogNzU4INC80Lwg0YAu0YEuCjPQktC70LDQttC90L7RgdGC0Yw6IDM0ICUKM9CS0LXRgtC10YA6IDEg0Lwv0YEsINCuLdCXCjPQp9Cw0YHQvtCy0L7QuSDQv9GA0L7Qs9C90L7Qtwoz0JLQoSAyMTowMAozKzIzwrAKM9Cf0J0gMDA6MDAKMysyMMKwCjPQn9CdIDAzOjAwCjMrMTnCsAoz0J_QnSAwNjowMAozKzIywrAKM9Cf0YDQvtCz0L3QvtC3INC90LAg0L3QtdC00LXQu9GOCjMyMCDQuNGO0L3Rjwoz0LLQvtGB0LrRgNC10YHQtdC90YzQtQoz0J3QvtGH0Ywv0JTQtdC90YwKMysxOS8rMzDCsAoz0JLQtdGC0LXRgDogMC0yINC8LtGBLiwg0JfQsNC_0LDQtNC90YvQuQrQlNCw0LLQu9C10L3QuNC1OiA3NTctNzU5INC80Lwg0YAu0YEKM9Cv0YHQvdC-CjMyMSDQuNGO0L3Rjwoz0L_QvtC90LXQtNC10LvRjNC90LjQugoz0J3QvtGH0Ywv0JTQtdC90YwKMysxOS8rMzHCsAoz0JLQtdGC0LXRgDogMS0zINC8LtGBLiwg0JfQsNC_0LDQtNC90YvQuQrQlNCw0LLQu9C10L3QuNC1OiA3NTYtNzU4INC80Lwg0YAu0YEKM9Cv0YHQvdC-CjMyMiDQuNGO0L3Rjwoz0LLRgtC-0YDQvdC40LoKM9Cd0L7Rh9GML9CU0LXQvdGMCjMrMTkvKzMywrAKM9CS0LXRgtC10YA6IDEtMyDQvC7RgS4sINCX0LDQv9Cw0LTQvdGL0LkK0JTQsNCy0LvQtdC90LjQtTogNzU2LTc1OCDQvNC8INGALtGBCjPQr9GB0L3QvgozMjMg0LjRjtC90Y8KM9GB0YDQtdC00LAKM9Cd0L7Rh9GML9CU0LXQvdGMCjMrMjIvKzMywrAKM9CS0LXRgtC10YA6IDEtMyDQvC7RgS4sINCh0LXQstC10YDQvi3Qt9Cw0L_QsNC00L3Ri9C5CtCU0LDQstC70LXQvdC40LU6IDc1Ni03NTcg0LzQvCDRgC7RgQoz0K_RgdC90L4KMzI0INC40Y7QvdGPCjPRh9C10YLQstC10YDQswoz0J3QvtGH0Ywv0JTQtdC90YwK";
        BigProfileBuilder.AddGrab(rawString);
        TGrabFactorsComputer preproc;
        const auto sizeLimit = static_cast<size_t>(TGrabFactorsComputer::DefaultNumTexts);
        auto actualResult = preproc.Parse(*BigProfileBuilder.GetProfile(), {});

        UNIT_ASSERT(actualResult.contains("GrabTexts"));
        UNIT_ASSERT(actualResult.at("GrabTexts").Size() <= sizeLimit);
    }

    void SetUp() override {
        ProfileBuilder.AddGrab("dNCS0LvQsNGB0YLQuCDQnNC-0YHQutCy0Ysg0YDQsNC30YrRj9GB0L3QuNC70Lgg0L_RgNCw0LLQsCDQvtGC0LrQsNC30LDQstGI0LjRhdGB0Y8g0L7RgsKg0LLQsNC60YbQuNC90LDRhtC40Lg6INCv0L3QtNC10LrRgS7QndC-0LLQvtGB0YLQuAox0JLQu9Cw0YHRgtC4INCc0L7RgdC60LLRiyDRgNCw0LfRitGP0YHQvdC40LvQuCDQv9GA0LDQstCwINC-0YLQutCw0LfQsNCy0YjQuNGF0YHRjyDQvtGCINCy0LDQutGG0LjQvdCw0YbQuNC4IAoy0J_QvtC00YDQvtCx0L3QtdC1INC-INGB0L7QsdGL0YLQuNC4IAoy0JLQu9Cw0YHRgtC4INCc0L7RgdC60LLRiyDQvdCw0LfQstCw0LvQuCDRg9GB0LvQvtCy0LjRjyDQv9C-0LvRg9GH0LXQvdC40Y8gUVIt0LrQvtC00L7QsiAKMtCS0J7QlyDQvtC00L7QsdGA0LjQu9CwINC_0YDQuNC80LXQvdC10L3QuNC1INC_0YDQuNCy0LjQstC60Lgg0L_RgNC-0YLQuNCyIENPVklELTE5INGDINC_0L7QtNGA0L7RgdGC0LrQvtCyIAoy0JIg0JzQvtGB0LrQstC1INC30LAg0YHRg9GC0LrQuCDQstGL0Y_QstC70LXQvdC-IDg1OTgg0YHQu9GD0YfQsNC10LIg0LrQvtGA0L7QvdCw0LLQuNGA");
    }
    

private:
    TPageProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TPageGrabPreprocessorsTests);
    UNIT_TEST(GrabTextsTest);
    UNIT_TEST(GrabTextsLimit);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TPageGrabPreprocessorsTests);
}
