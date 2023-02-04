#include <library/cpp/testing/unittest/registar.h>
#include <yabs/proto/tsar.pb.h>
#include <ads/tensor_transport/lib_2/banner.h>
#include <ads/tensor_transport/proto/banner.pb.h>
#include <ads/tensor_transport/lib_2/factorization_tensor.h>
#include <library/cpp/dot_product/dot_product.h>


using namespace NTsarTransport;


class TTestTensortFirstValue : public TTestBase {

public:
    void TestTensorComputation() {
        ui64 vectorSize = 50;
        auto tsarTensor = TFactorizationTensor().MakePageBannerTensor(vectorSize);

        UNIT_ASSERT_EQUAL(tsarTensor.GetFactors(0), 1);
        float sum = 0;
        for (ui64 i = 0; i< vectorSize; ++i) {
            sum += tsarTensor.GetFactors(i);
        }

        UNIT_ASSERT_EQUAL(sum, 1.0);

        UNIT_ASSERT_EQUAL((ui64)tsarTensor.GetFactors().size(), vectorSize*vectorSize*vectorSize);
        UNIT_ASSERT_EQUAL(tsarTensor.GetFactors(vectorSize*vectorSize*vectorSize - 1), 0);

    }

    UNIT_TEST_SUITE(TTestTensortFirstValue);
    UNIT_TEST(TestTensorComputation);
    UNIT_TEST_SUITE_END();

};

UNIT_TEST_SUITE_REGISTRATION(TTestTensortFirstValue);

Y_UNIT_TEST_SUITE(BannerUserDotTensor) {
    Y_UNIT_TEST(Simple) {
        auto tensor = TBannerUserDotTensor::MakeBannerUserDotTensor(10);
        UNIT_ASSERT_EQUAL(tensor.GetFactors().size(), 100);

        TVector<float> bannerVec{0.231, 0.885, 1.423, 99.99, 0.5, 0.632, 0.753, 0.2347, 0.853, 0.659};
        TVector<float> userVec{0.133, 0.325, 0.09678, 0.561, 0.5891, 0.019, 0.98134, 105.434, 9.64, 0.65};
        TVector<float> pageVec{1.0};
        UNIT_ASSERT_EQUAL(bannerVec.size(), userVec.size());
        UNIT_ASSERT_EQUAL(bannerVec.size(), 10);
        UNIT_ASSERT_EQUAL(pageVec.size(), 1);

        float naiveDotProduct = DotProduct(bannerVec.cbegin(), userVec.cbegin(), bannerVec.size());

        auto d1 = bannerVec.size();
        auto d2 = userVec.size();
        auto d3 = pageVec.size();

        TVector<float> hitVector(bannerVec.size(), 0.0);
        for (size_t i = 0; i < d1; ++i) {
            for (size_t j = 0; j < d2; ++j) {
                hitVector[i] += DotProduct(tensor.GetFactors().cbegin() + (i * d2 * d3 + j * d3), pageVec.cbegin(), d3) * (*(userVec.cbegin() + j));
            }
        }
        float tsarResult = DotProduct(bannerVec.cbegin(), hitVector.cbegin(), bannerVec.size());

        UNIT_ASSERT_DOUBLES_EQUAL(tsarResult, naiveDotProduct, 1e-7);
    }

};
