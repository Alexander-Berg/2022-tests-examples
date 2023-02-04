#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/banner_transport/tsar_banner_embedder.h>
#include <ads/pytorch/deploy/banner_transport/tsar_banner_embedder_v2.h>
#include <ads/pytorch/deploy/lib/tsar_computer.h>
#include <ads/pytorch/deploy/model_builder_lib/partitioned_model.h>

#include <util/generic/array_ref.h>
#include <util/stream/file.h>
#include <util/system/tempfile.h>
#include <util/ysaveload.h>

using namespace NPytorchTransport;

namespace {
    class EmptyModel: public IModel {
    public:
        EmptyModel() = default;

        EmptyModel(const ui64 modelSize)
            : ModelSize(modelSize)
        {
        }

        TVector<float> CalculateModel(const THashMap<TString, TVector<ui64>>& features, const THashMap<TString, TVector<float>>& realvalueFeatures) const override {
            Y_UNUSED(features);
            Y_UNUSED(realvalueFeatures);
            return TVector<float>(ModelSize);
        }

        TVector<TVector<float>> CalculateModel(const TVector<THashMap<TString, TVector<ui64>>>& features, const TVector<THashMap<TString, TVector<float>>>& realvalueFeatures) const override {
            Y_UNUSED(realvalueFeatures);
            TVector<TVector<float>> result(features.size(), TVector<float>(ModelSize));
            return result;
        }

        ui64 GetVectorSize() const override {
            return ModelSize;
        }

        TVector<TString> GetRealValueFeatureNames() const override {
            return {};
        }

        TVector<TString> GetCategoricalFeatureNames() const override {
            return {};
        }

        void WarmUp(const ui64 warmUpCount) const override {
            Y_UNUSED(warmUpCount);
        }

        void Save(IOutputStream& stream) const override {
            ::Save(&stream, ModelSize);
        }

        void Load(IInputStream& stream) override {
            ::Load(&stream, ModelSize);
        }

    private:
        ui64 ModelSize;
    };
}

Y_UNIT_TEST_SUITE(BannerEmbedderTest) {
    Y_UNIT_TEST(TTsarBannerEmbedderSaveLoadTest) {
        THashMap<TString, THashSet<TString>> lemmerFixList;
        lemmerFixList.emplace("a", THashSet<TString>{"b"});

        TTsarBannerEmbedder<TTsarComputer, EmptyModel> embedderOne(new TTsarComputer(new EmptyModel(10)), lemmerFixList);
        auto& lemmerFixListOne = embedderOne.GetLemmerFixList();
        UNIT_ASSERT(lemmerFixListOne.at("a").count("b"));
        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        embedderOne.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TTsarBannerEmbedder<TTsarComputer, EmptyModel> embedderTwo;
        embedderTwo.Load(inputStream);
        auto& lemmerFixListTwo = embedderTwo.GetLemmerFixList();
        UNIT_ASSERT(lemmerFixListTwo.at("a").count("b"));
    }

    Y_UNIT_TEST(TTsarBannerEmbedderV1Test) {
        THashMap<TString, THashSet<TString>> lemmerFixList;
        lemmerFixList.emplace("a", THashSet<TString>{"b"});
        auto computer = MakeHolder<TTsarComputer>(new TPartitionedModel("BannerNamespaces", true));
        TTsarBannerEmbedder<TTsarComputer, TPartitionedModel> embedderOne(std::move(computer), lemmerFixList);
        TensorTransport::TBannerRecord fakeBannerRow;
        fakeBannerRow.SetBannerID(1);
        fakeBannerRow.SetOrderID(2);
        fakeBannerRow.SetTargetDomainID(3);
        fakeBannerRow.SetBannerText("какой-то рекламный текст");
        fakeBannerRow.SetBannerTitle("КАКОЙ-ТО РЕКЛАМНЫЙ ЗАГОЛОВОК");
        fakeBannerRow.SetLandingPageTitle("заходите к нам на страницу");
        fakeBannerRow.SetTrueLandingURL("https://yandex.ru");
        fakeBannerRow.SetLandingURL("https://yandex.ru");
        fakeBannerRow.SetCategories("2000000012 2000034000");
        auto outOne = embedderOne.EmbedBanner(fakeBannerRow);
    }

    Y_UNIT_TEST(TTsarBannerEmbedderV2Test) {
        auto computer = MakeHolder<TTsarComputer>(new TPartitionedModel("BannerNamespacesFakeYamlModel", true));
        TTsarBannerEmbedderV2<TTsarComputer, TPartitionedModel> embedderOne(std::move(computer));
        TensorTransport::TBannerRecord fakeBannerRow;
        fakeBannerRow.SetBannerID(1);
        fakeBannerRow.SetOrderID(2);
        fakeBannerRow.SetTargetDomainID(3);
        fakeBannerRow.SetBannerText("какой-то рекламный текст");
        fakeBannerRow.SetBannerTitle("КАКОЙ-ТО РЕКЛАМНЫЙ ЗАГОЛОВОК");
        fakeBannerRow.SetLandingPageTitle("заходите к нам на страницу");
        fakeBannerRow.SetTrueLandingURL("https://yandex.ru");
        fakeBannerRow.SetLandingURL("https://yandex.ru");
        fakeBannerRow.SetCategories("2000000012 2000034000");
        auto outOne = embedderOne.EmbedBanner(fakeBannerRow);

        TTempFile tmpFile(MakeTempName());
        TFileOutput outputStream(tmpFile.Name());
        embedderOne.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(tmpFile.Name());
        TTsarBannerEmbedderV2<TTsarComputer, TPartitionedModel> embedderTwo;
        embedderTwo.Load(inputStream);
        auto outTwo  = embedderTwo.EmbedBanner(fakeBannerRow);
        UNIT_ASSERT_VALUES_EQUAL(outOne.size(), outTwo.size());
        for (ui64 i = 0; i < outOne.size(); ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(outOne[i], outTwo[i], 1e-7);
        }
    }
};
