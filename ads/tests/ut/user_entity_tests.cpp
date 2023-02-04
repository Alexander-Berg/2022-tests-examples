#include <library/cpp/testing/unittest/registar.h>

#include <random>
#include <ads/bigkv/ltp/entities/user.h>

#define ASSERT_VECTORS_EQUAL(x, y, eps) \
    UNIT_ASSERT_EQUAL(x.size(), y.size()); \
    for (size_t __i = 0; __i < x.size(); ++__i) { \
        UNIT_ASSERT_DOUBLES_EQUAL(x[__i], y[__i], eps); \
    }

TVector<float> GenerateRandomVector(size_t size) {
    TVector<float> vector(size);
    static std::mt19937 gen(8892);
    std::uniform_real_distribution<float> distrib(-1.0, 1.0);
    for (float& x : vector) {
        x = distrib(gen);
    }
    return vector;
}

Y_UNIT_TEST_SUITE(TUserWithLtpEntityTests) {
    Y_UNIT_TEST(LtpVectorExtractor) {
        NewUserFactors::TProfile profile;
        auto vector = GenerateRandomVector(173);
        {
            auto newVector = profile.add_tsar_vectors();
            newVector->set_vector_id(132);
            static_assert(sizeof(float) == 4, "wrong size of float");
            newVector->set_value(reinterpret_cast<const char*>(vector.data()), vector.size() * sizeof(float));
            newVector->set_element_size(sizeof(float));
            newVector->set_vector_size(vector.size());
        }
        NLtpTransport::TLtpVectorExtractor extractor(profile, 132 /* ltp id */, 173 /* ltp size */);
        auto extracted = extractor.GetVector();
        ASSERT_VECTORS_EQUAL(extracted, vector, 1e-9);
    }

    Y_UNIT_TEST(EmptyRtPartTest) {
        NewUserFactors::TProfile profile;
        auto vector = GenerateRandomVector(173);
        {
            auto newVector = profile.add_tsar_vectors();
            newVector->set_vector_id(132);
            static_assert(sizeof(float) == 4, "wrong size of float");
            newVector->set_value(reinterpret_cast<const char*>(vector.data()), vector.size() * sizeof(float));
            newVector->set_element_size(sizeof(float));
            newVector->set_vector_size(vector.size());

            newVector = profile.add_tsar_vectors();
            newVector->set_vector_id(101);
            static_assert(sizeof(float) == 4, "wrong size of float");
            auto anotherVector = GenerateRandomVector(23);
            newVector->set_value(reinterpret_cast<const char*>(anotherVector.data()), anotherVector.size() * sizeof(float));
            newVector->set_element_size(sizeof(float));
            newVector->set_vector_size(anotherVector.size());
        }
        NLtpTransport::TUserWithLtp user(profile, 19998 /* time */, 132 /* ltp tsar vector id */, 173 /* ltp size */);
        auto features = user.GetRealvalueFeatures();
        ASSERT_VECTORS_EQUAL(features["_LTPVector"], vector, 1e-9);
    }

    Y_UNIT_TEST(EmptyBothPartsTest) {
        NewUserFactors::TProfile profile;
        NLtpTransport::TUserWithLtp user(profile, 19998 /* time */, 132 /* ltp tsar vector id */, 10 /* ltp size */);
        auto features = user.GetRealvalueFeatures();
        ASSERT_VECTORS_EQUAL(features["_LTPVector"], TVector<float>(10, 0), 1e-9);
    }
}
