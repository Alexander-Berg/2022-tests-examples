#include <library/cpp/testing/unittest/registar.h>
#include <ads/pytorch/deploy/math_lib/vector_math.h>

#include <library/cpp/iterator/zip.h>
#include <util/generic/hash.h>
#include <util/folder/path.h>
#include <util/system/tempfile.h>

using namespace NPytorchTransport::NMath;


Y_UNIT_TEST_SUITE(TVectorMathTest) {

    /// Memcpy tests. They are most important since code of other tests
    /// relies on correct memcpy work

    Y_UNIT_TEST(TestMemcpyFromFloatToHalfNoAccuracyLoss) {
        TVector<float> src({1.f, 2.f, 3.f});
        TVector<uint16_t> dst(src.size(), 0);
        MemcpyFloatsWithHalfCast(dst.data(), src.data(), src.size());

        TVector<float> srcAfterCopy(src.size(), 0.);
        MemcpyFloatsWithHalfCast(srcAfterCopy.data(), dst.data(), src.size());

        for (const auto &[trueValue, actualValue]: Zip(src, srcAfterCopy)) {
            UNIT_ASSERT_EQUAL(trueValue, actualValue);
        }
    }

    Y_UNIT_TEST(TestMemcpyFloatToFloat) {
        TVector<float> src({1.f, 2.f, 3.f});
        TVector<float> dst(src.size(), 0.);
        MemcpyFloatsWithHalfCast(dst.data(), src.data(), src.size());

        for (const auto &[trueValue, actualValue]: Zip(src, dst)) {
            UNIT_ASSERT_EQUAL(trueValue, actualValue);
        }
    }

    Y_UNIT_TEST(TestMemcpyHalfTohalf) {
        TVector<uint16_t> src({5, 10, 15});
        TVector<uint16_t> dst(src.size(), 0);
        MemcpyFloatsWithHalfCast(dst.data(), src.data(), src.size());

        for (const auto &[trueValue, actualValue]: Zip(src, dst)) {
            UNIT_ASSERT_EQUAL(trueValue, actualValue);
        }
    }

    /// math tests

    Y_UNIT_TEST(TestVectorMulC) {
        TVector<float> src({1.f, 2.f, 3.f});
        VectorMulC(src.data(), src.data(), 0.1f, src.size());
        TVector<float> reference({0.1f, 0.2f, 0.3f});

        for (const auto &[trueValue, actualValue]: Zip(src, reference)) {
            UNIT_ASSERT_EQUAL(trueValue, actualValue);
        }
    }

    namespace {
        template <typename TDst, typename TV1, typename TV2>
        void AddVectorHelper(const TVector<float> &v1, const TVector<float> &v2, const TVector<float> &reference) {
            TVector<TDst> dstCasted(v1.size());
            TVector<TV1> v1Casted(v1.size());
            TVector<TV2> v2Casted(v2.size());

            MemcpyFloatsWithHalfCast(v1Casted.data(), v1.data(), v1.size());
            MemcpyFloatsWithHalfCast(v2Casted.data(), v2.data(), v2.size());

            AddVector(dstCasted.data(), v1Casted.data(), v2Casted.data(), v1.size());

            TVector<float> result(v1.size());
            MemcpyFloatsWithHalfCast(result.data(), dstCasted.data(), dstCasted.size());

            for (const auto &[trueValue, actualValue]: Zip(result, reference)) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue);
            }
        }

        template <typename TDst, typename TV1, typename TV2>
        void FMAddVectorHelper(const TVector<float> &v1, const TVector<float> &v2, const float &multiplier, const TVector<float> &reference) {
            TVector<TDst> dstCasted(v1.size());
            TVector<TV1> v1Casted(v1.size());
            TVector<TV2> v2Casted(v2.size());

            MemcpyFloatsWithHalfCast(v1Casted.data(), v1.data(), v1.size());
            MemcpyFloatsWithHalfCast(v2Casted.data(), v2.data(), v2.size());

            FMAddVector(dstCasted.data(), v1Casted.data(), v2Casted.data(), multiplier, v1.size());

            TVector<float> result(v1.size());
            MemcpyFloatsWithHalfCast(result.data(), dstCasted.data(), dstCasted.size());

            for (const auto &[trueValue, actualValue]: Zip(result, reference)) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue);
            }
        }
    }

    Y_UNIT_TEST(TestAddVector) {
        TVector<float> v1({1.f, 2.f, 3.f});
        TVector<float> v2({3.f, 4.f, 5.f});
        TVector<float> reference({4.f, 6.f, 8.f});

        AddVectorHelper<float, float, float>(v1, v2, reference);
        AddVectorHelper<float, float, uint16_t>(v1, v2, reference);
        AddVectorHelper<float, uint16_t, float>(v1, v2, reference);
        AddVectorHelper<float, uint16_t, uint16_t>(v1, v2, reference);
        AddVectorHelper<uint16_t, float, float>(v1, v2, reference);
        AddVectorHelper<uint16_t, float, uint16_t>(v1, v2, reference);
        AddVectorHelper<uint16_t, uint16_t, float>(v1, v2, reference);
        AddVectorHelper<uint16_t, uint16_t, uint16_t>(v1, v2, reference);
    }

    Y_UNIT_TEST(TestFMAddVector) {
        TVector<float> v1({1.f, 2.f, 3.f});
        TVector<float> v2({3.f, 4.f, 5.f});
        TVector<float> reference({-2.f, -2.f, -2.f});
        float multiplier = -1.f;

        FMAddVectorHelper<float, float, float>(v1, v2, multiplier, reference);
        FMAddVectorHelper<float, float, uint16_t>(v1, v2, multiplier, reference);
        FMAddVectorHelper<float, uint16_t, float>(v1, v2, multiplier, reference);
        FMAddVectorHelper<float, uint16_t, uint16_t>(v1, v2, multiplier, reference);
        FMAddVectorHelper<uint16_t, float, float>(v1, v2, multiplier, reference);
        FMAddVectorHelper<uint16_t, float, uint16_t>(v1, v2, multiplier, reference);
        FMAddVectorHelper<uint16_t, uint16_t, float>(v1, v2, multiplier, reference);
        FMAddVectorHelper<uint16_t, uint16_t, uint16_t>(v1, v2, multiplier, reference);
    }
}
