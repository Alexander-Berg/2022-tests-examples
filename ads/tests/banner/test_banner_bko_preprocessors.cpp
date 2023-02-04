#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/banner_proto_helpers.h>
#include <ads/bigkv/preprocessors/banner_preprocessors/banner_bko_preprocessors.h>

#include <yabs/server/util/bobhash.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/iterator/zip.h>

#include <util/stream/output.h>


namespace {
    static const double EPS = 0.0001;

    bool FloatVectorsEqual(TVector<float> v1, TVector<float> v2) {
        if (v1.size() != v2.size()) {
            return false;
        }

        for (const auto& [x1, x2] : Zip(v1, v2)) {
            if (std::abs(x1 - x2) > EPS) {
                return false;
            }
        }

        return true;
    }

    void AssertHasAllKeys(const TSet<TString> requiredKeys, const NYT::TNode::TMapType& result) {
        for (auto& key : requiredKeys) {
            UNIT_ASSERT_C(result.contains(key), "required key " << key << " is missing.");
        }

        for (auto& [key, value] : result) {
            UNIT_ASSERT_C(requiredKeys.contains(key), "unknown key " << key << ".");
        }
    }
}


namespace NProfilePreprocessing {

Y_UNIT_TEST_SUITE(TryParseHexRgbToUi8Else256) {
    Y_UNIT_TEST(ParseTest) {
        UNIT_ASSERT_VALUES_EQUAL(TryParseHexRgbToUi8Else256(""), 256);
        UNIT_ASSERT_VALUES_EQUAL(TryParseHexRgbToUi8Else256("bad string"), 256);
        // #ff00dd == (255, 0, 221) -> 7, 0, 3 -> 227
        // (255 * 8 / 256, 0 * 8 / 256, 221 * 4 / 256) == (7, 0, 3)
        // 3 + (3 << 2) + (7 << 5) == 227
        UNIT_ASSERT_VALUES_EQUAL(TryParseHexRgbToUi8Else256("#ff00dd"), 227);
    }
}

class TProductPricePreprocessorTests : public TTestBase {
public:
    void ParseTest() {
        NCSR::TBannerProfileProto banner;
        SetBannerPrice(banner, "1.35", "USD", "1,5", 10);

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        UNIT_ASSERT_DOUBLES_EQUAL(actual["ProductPrice"].AsList()[0].AsDouble(), 1.35, EPS);
        UNIT_ASSERT_VALUES_EQUAL(actual["ProductPriceLogBin"].AsUint64(), 1);
        UNIT_ASSERT_VALUES_EQUAL(actual["ProductCurrency"].AsUint64(), yabs_bobhash("USD"));
        UNIT_ASSERT_DOUBLES_EQUAL(actual["ProductOldPrice"].AsList()[0].AsDouble(), 1.5, EPS);
        UNIT_ASSERT_VALUES_EQUAL(actual["ProductOldPriceLogBin"].AsUint64(), 1);
        UNIT_ASSERT_DOUBLES_EQUAL(actual["ProductDiscount"].AsList()[0].AsDouble(), 10, EPS);
    }

    void ParseEmptyTest() {
        NCSR::TBannerProfileProto banner;

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        UNIT_ASSERT_DOUBLES_EQUAL(actual["ProductPrice"].AsList()[0].AsDouble(), 0.0, EPS);
        UNIT_ASSERT_VALUES_EQUAL(actual["ProductPriceLogBin"].AsUint64(), 0);
        UNIT_ASSERT_VALUES_EQUAL(actual["ProductCurrency"].AsUint64(), 0);
        UNIT_ASSERT_DOUBLES_EQUAL(actual["ProductOldPrice"].AsList()[0].AsDouble(), 0.0, EPS);
        UNIT_ASSERT_VALUES_EQUAL(actual["ProductOldPriceLogBin"].AsUint64(), 0);
        UNIT_ASSERT_DOUBLES_EQUAL(actual["ProductDiscount"].AsList()[0].AsDouble(), 0.0, EPS);
    }

    void SchemaTest() {
        const auto schema = Preprocessor.Schema();

        AssertHasAllKeys(schema);
    }

    void AssertHasAllKeys(const NYT::TNode::TMapType& result) {
        ::AssertHasAllKeys({
            "ProductPrice",
            "ProductPriceLogBin",
            "ProductCurrency",
            "ProductOldPrice",
            "ProductOldPriceLogBin",
            "ProductDiscount"
        }, result);
    }

private:
    TProductPricePreprocessor Preprocessor;

    void SetBannerPrice(
        NCSR::TBannerProfileProto& banner,
        TString price, TString currency, TString oldPrice, int32_t discount
    ) {
        NGrut::NExperimental::NBanner::TBannerPrice bannerPrice;
        bannerPrice.SetPrice(price);
        bannerPrice.SetCurrency(currency);
        bannerPrice.SetOldPrice(oldPrice);
        bannerPrice.SetDiscount(discount);

        banner.MutableResources()
            ->MutableDirectBannersLogFields()
            ->MutableBannerPrice()
            ->CopyFrom(bannerPrice);
    }

    UNIT_TEST_SUITE(TProductPricePreprocessorTests);
    UNIT_TEST(ParseTest);
    UNIT_TEST(ParseEmptyTest);
    UNIT_TEST(SchemaTest);

    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TProductPricePreprocessorTests);


class TProductProbabilityPreprocessorTests : public TTestBase {
public:
    void ParseTest() {
        NCSR::TBannerProfileProto banner;

        auto* ecomFeatures = banner.MutableResources()->MutableUrl()->MutableEcomFeatures()->Add();
        ecomFeatures->SetNoProductsProbability(0.25);
        ecomFeatures->SetOneProductProbability(0.55);
        ecomFeatures->SetManyProductsProbability(0.20);

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        auto& probas = actual["ProductProbabilities"].AsList();
        UNIT_ASSERT_VALUES_EQUAL(probas.size(), 3);
        UNIT_ASSERT_DOUBLES_EQUAL(probas[0].AsDouble(), 0.25, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(probas[1].AsDouble(), 0.55, EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(probas[2].AsDouble(), 0.20, EPS);
    }

    void SchemaTest() {
        AssertHasAllKeys(Preprocessor.Schema());
    }

    void AssertHasAllKeys(const NYT::TNode::TMapType& result) {
        ::AssertHasAllKeys({"ProductProbabilities"}, result);
    }

private:
    TProductProbabilityPreprocessor Preprocessor;

    UNIT_TEST_SUITE(TProductProbabilityPreprocessorTests);
    UNIT_TEST(ParseTest);
    UNIT_TEST(SchemaTest);

    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TProductProbabilityPreprocessorTests);

class TImageAvatarsMetaDataPreprocessorTests : public TTestBase {
public:
    void ParseTest() {
        NCSR::TBannerProfileProto banner;

        // NCSR::TClassifier classifier;
        auto* classifier = banner.MutableAvatarsMetaData()
            ->MutableImageClassifiers()
            ->Add()
            ->AddClassifiers();
        classifier->SetClassifierID(NCSR::EAvatarsClassifier::AC_CLOTHES);
        classifier->SetValue(42);


        auto* i2tEmbedding = banner.MutableAvatarsMetaData()
            ->MutableImageEmbeddings()
            ->Add()
            ->AddEmbeddings();
        TVector<float> i2tVector(200, 1.0);
        i2tEmbedding->SetVectorID(NCSR::EAvatarsEmbedding::AE_I2T_V12);
        for (auto x: i2tVector) {
            i2tEmbedding->AddVector(x);
        }

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        auto& classifiers = actual["ImageAvatarsClassifiers"].AsList();
        const size_t classifiersNum = 10;
        const size_t clothesIndex = 3;
        TVector<ui64> expectedClassifiers(classifiersNum, 0u);
        expectedClassifiers[clothesIndex] = 42;
        UNIT_ASSERT_VALUES_EQUAL(classifiers.size(), expectedClassifiers.size());
        for (const auto& [actualNode, expected] : Zip(classifiers, expectedClassifiers)) {
            UNIT_ASSERT_VALUES_EQUAL(actualNode.AsUint64(), expected);
        }

        TVector<float> actualI2tVector;
        for (const auto& node : actual["ImageAvatarsEmbeddingI2T"].AsList()) {
            actualI2tVector.push_back(node.AsDouble());
        }
        UNIT_ASSERT(FloatVectorsEqual(i2tVector, actualI2tVector));
    }

    void ParseEmptyTest() {
        NCSR::TBannerProfileProto banner;

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);
    }

    void SchemaTest() {
        AssertHasAllKeys(Preprocessor.Schema());
    }

    void AssertHasAllKeys(const NYT::TNode::TMapType& result) {
        ::AssertHasAllKeys({
            "ImageAvatarsClassifiers",
            "ImageAvatarsEmbeddingI2T",
            "ImageAvatarsEmbeddingClothesColor",
        }, result);
    }

private:
    TImageAvatarsMetaDataPreprocessor Preprocessor;

    UNIT_TEST_SUITE(TImageAvatarsMetaDataPreprocessorTests);
    UNIT_TEST(ParseTest);
    UNIT_TEST(ParseEmptyTest);
    UNIT_TEST(SchemaTest);

    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TImageAvatarsMetaDataPreprocessorTests);

class TDirectImageMdsMetaPreprocessorTests : public TTestBase {
public:
    void ParseTest() {
        NCSR::TBannerProfileProto banner;

        auto* mdsMeta = banner.MutableResources()
            ->MutableDirectBannersLogFields()
            ->MutableResources()
            ->MutableImagesInfo()
            ->Add()
            ->MutableParsedMdsMeta();
        mdsMeta->SetAverageColor("#ff00dd");  // 255, 0, 221 -> 7, 0, 3 -> 227
        mdsMeta->SetColorWizBack("bad color");

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        UNIT_ASSERT_VALUES_EQUAL(actual["DirectImageAverageColor"].AsUint64(), 227);
        UNIT_ASSERT_VALUES_EQUAL(actual["DirectImageColorWizBack"].AsUint64(), 256);
        UNIT_ASSERT_VALUES_EQUAL(actual["DirectImageColorWizButton"].AsUint64(), 256);
        UNIT_ASSERT_VALUES_EQUAL(actual["DirectImageColorWizButtonText"].AsUint64(), 256);
    }

    void SchemaTest() {
        AssertHasAllKeys(Preprocessor.Schema());
    }

    void AssertHasAllKeys(const NYT::TNode::TMapType& result) {
        ::AssertHasAllKeys({
            "DirectImageAverageColor",
            "DirectImageColorWizBack",
            "DirectImageColorWizButton",
            "DirectImageColorWizButtonText",
        }, result);
    }

private:
    TDirectImageMdsMetaPreprocessor Preprocessor;

    UNIT_TEST_SUITE(TDirectImageMdsMetaPreprocessorTests);
    UNIT_TEST(ParseTest);
    UNIT_TEST(SchemaTest);

    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TDirectImageMdsMetaPreprocessorTests);

class TContentAttrsOfferPreprocessorTests : public TTestBase {
public:
    void ParseTest() {
        NCSR::TBannerProfileProto banner;

        auto* attrs = banner.MutableUrlData()->MutableContentAttrsOffer();
        attrs->SetShopVendor("Some vendoR");
        attrs->SetColor("светло-серый");
        attrs->SetMaterial("Калёная сталь");

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        UNIT_ASSERT_VALUES_EQUAL(actual["ContentAttrsOfferShopVendor"].AsString(), "Some vendoR");
        UNIT_ASSERT_VALUES_EQUAL(actual["ContentAttrsOfferColor"].AsString(), "светло-серый");
        UNIT_ASSERT_VALUES_EQUAL(actual["ContentAttrsOfferMaterial"].AsString(), "Калёная сталь");
    }

    void SchemaTest() {
        AssertHasAllKeys(Preprocessor.Schema());
    }

    void AssertHasAllKeys(const NYT::TNode::TMapType& result) {
        ::AssertHasAllKeys({
            "ContentAttrsOfferShopVendor",
            "ContentAttrsOfferColor",
            "ContentAttrsOfferMaterial",
        }, result);
    }

private:
    TContentAttrsOfferPreprocessor Preprocessor;

    UNIT_TEST_SUITE(TContentAttrsOfferPreprocessorTests);
    UNIT_TEST(ParseTest);
    UNIT_TEST(SchemaTest);

    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TContentAttrsOfferPreprocessorTests);

class TBannerLandProductInfoPreprocessorTests : public TTestBase {
public:
    void ParseTest() {
        NCSR::TBannerProfileProto banner;

        auto* offer = banner.MutableResources()->MutableBannerLandSpecificFields()->MutableOfferProperties();
        offer->SetTypePrefix("Some Prefix");
        for (const auto& name : {"Parent", "Child"}) {
            auto* node = offer->MutableCategoryPath()->MutableNodes()->Add();
            node->SetName(name);
        }
        offer->MutableParameters()->SetSex("Женский");
        offer->MutableParameters()->SetColor("светло-серый");
        offer->MutableParameters()->SetMaterial("Калёная сталь");

        NYT::TNode::TMapType actual = Preprocessor.ParseProfile(banner, TArgs(), TExtractedBannerFeatures());

        AssertHasAllKeys(actual);

        UNIT_ASSERT_VALUES_EQUAL(actual["BannerLandProductTypePrefix"].AsString(), "Some Prefix");
        UNIT_ASSERT_VALUES_EQUAL(actual["BannerLandProductCategoryPath"].AsString(), " -> Parent -> Child");
        UNIT_ASSERT_VALUES_EQUAL(actual["BannerLandProductSex"].AsUint64(), yabs_bobhash("женский"));
        UNIT_ASSERT_VALUES_EQUAL(actual["BannerLandProductColor"].AsString(), "светло-серый");
        UNIT_ASSERT_VALUES_EQUAL(actual["BannerLandProductMaterial"].AsString(), "Калёная сталь");
    }

    void SchemaTest() {
        AssertHasAllKeys(Preprocessor.Schema());
    }

    void AssertHasAllKeys(const NYT::TNode::TMapType& result) {
        ::AssertHasAllKeys({
            "BannerLandProductTypePrefix",
            "BannerLandProductCategoryPath",
            "BannerLandProductSex",
            "BannerLandProductColor",
            "BannerLandProductMaterial",
            "BannerLandProductSize",
            "BannerLandProductSizeUnit",
            "BannerLandProductModel",
            "BannerLandProductVendor",
        }, result);
    }

private:
    TBannerLandProductInfoPreprocessor Preprocessor;

    UNIT_TEST_SUITE(TBannerLandProductInfoPreprocessorTests);
    UNIT_TEST(ParseTest);
    UNIT_TEST(SchemaTest);

    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerLandProductInfoPreprocessorTests);

}  // namespace NProfilePreprocessing
