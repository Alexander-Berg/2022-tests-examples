#include <saas/rtyserver/factors/rty_features_factory.h>
#include <saas/rtyserver/search/cgi/rty_external_cgi.h>


using namespace NRTYFeatures;

namespace {
    constexpr TStringBuf TestFactorName = "TestDpFactor";
}

//
// TMockRtyFeatureCalcer : a mock implementation of factor, for tests
//
class TMockRtyFeatureCalcer: public IFeatureCalcer {
private:
    const TDynMapping Factors;

public:
    TMockRtyFeatureCalcer(const TDynMapping& factorsInRequest, const NRTYFactors::TConfig*)
        : Factors(factorsInRequest)
    {
    }

    float CalcValue(const TRTYDynamicFeatureContext& ctx, ui32 docId) const {
        Y_UNUSED(docId);
        const TString& req = ctx.RP->UserRequest;
        if (req.find(' ') != TString::npos) {
            return 0.1f;
        } else {
            TBlob blob = ctx.AD->GetDocText(docId)->UncompressBlob();
            TStringBuf docText(blob.AsCharPtr(), blob.Length());
            if (docText.find(req)) {
                return 0.6f;
            } else {
                return 0.5f;
            }
        }
    }

    void Calc(TFactorStorage& storage, const TRTYDynamicFeatureContext& ctx, ui32 docId) override {
        for (const auto& item : Factors) {
            if ((item.BaseIndex < -1) || item.SourceFactor != TestFactorName) {
                continue;
            }
            storage[item.BaseIndex] = CalcValue(ctx, docId);
        }
    }
};

//
// TMockRtyFeature : a mock implementation of feature, for tests
//
class TMockRtyFeature: public IFeature {
public:
    TIntrusivePtr<IFeatureCalcer> CreateCalcer(const TDynMapping& factorsInRequest, const NRTYFactors::TConfig* relevCfg, bool fastFeaturesOnly) override {
        Y_UNUSED(fastFeaturesOnly);

        const bool isFeatureEnabled = std::any_of(factorsInRequest.begin(), factorsInRequest.end(), [](const auto& item)->bool{
            return item.SourceFactor == TestFactorName;
        });
        if (!isFeatureEnabled) {
            return nullptr;
        }

        return MakeIntrusive<TMockRtyFeatureCalcer>(factorsInRequest, relevCfg);
    }

    void InitModels(const TDynMapping&, const NRTYFactors::TConfig*, const TString&) override { }

    void InitStaticInfo(TStringBuf /*modelsPath*/, std::function<void(TDynFactorInfo)> addFactor) override {
        addFactor({"TestDpFactor", Max<size_t>(), 32, TBitsReader::ftInt});
    }

private:
    static TRTYFeaturesFactory::TRegistrator<TMockRtyFeature> Registrator;
};

TRTYFeaturesFactory::TRegistrator<TMockRtyFeature> TMockRtyFeature::Registrator("mock_rty_feature");
