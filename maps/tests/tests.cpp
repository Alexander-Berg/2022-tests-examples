#include <maps/analyzer/libs/hgram/include/hgram.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <iostream>


using maps::analyzer::hgram::Bucket;
using maps::analyzer::hgram::Hgram;


namespace maps::yasm_metrics {

inline bool operator== (const Bucket& lhs, const Bucket& rhs) {
    return lhs.boundary == rhs.boundary && lhs.weight == rhs.weight;
}

inline ::std::ostream& operator<< (::std::ostream& os, const Bucket& b) {
    return os << "{" << b.boundary << ";" << b.weight << "}";
}

} // maps::yasm_metrics


Y_UNIT_TEST_SUITE(hgram) {

Y_UNIT_TEST(hgramCreate) {
    auto h = Hgram::equalSizedN(0.0, 10.0, 10);
    EXPECT_EQ(h.bounds().front().boundary, 0.0);
    EXPECT_EQ(h.bounds().back().boundary, 10.0);
    EXPECT_EQ(h.bounds().size(), 11u); // 10 intervals is 11 boundaries

    h = Hgram::equalSized(0.0, 10.0, 1.0);
    EXPECT_EQ(h.bounds().front().boundary, 0.0);
    EXPECT_EQ(h.bounds().back().boundary, 10.0);
    EXPECT_EQ(h.bounds().size(), 11u); // 10 intervals is 11 boundaries

    h = Hgram::equalSizedN(0.0, 10.0, 1);
    EXPECT_EQ(h.bounds().size(), 2u); // only min and max

    h = Hgram::equalSized(0.0, 10.0, 5.0);
    EXPECT_EQ(h.bounds().size(), 3u);

    h = Hgram::equalSized(0.0, 10.0, 8.0);
    EXPECT_EQ(h.bounds().size(), 2u); // 8.0 is rounded to 10.0

    EXPECT_THROW(
        Hgram::equalSizedN(0.0, 10.0, 0),
        maps::RuntimeError
    );

    EXPECT_THROW(
        Hgram::equalSizedN(10.0, 0.0, 1.0),
        maps::RuntimeError
    );

    // too big step
    EXPECT_THROW(
        Hgram::equalSized(0.0, 10.0, 1000.0),
        maps::RuntimeError
    );
}

Y_UNIT_TEST(hgramUse) {
    auto h = Hgram::equalSizedN(0.0, 10.0, 10);

    h[-100.0] += 1.0;
    h[0.0] += 1.0;
    h[0.1] += 1.0;
    h[0.5] += 1.0;
    h[0.9] += 1.0;
    h[1.0] += 1.0;
    h[100.0] += 1.0;

    EXPECT_EQ(h.bounds()[0], (Bucket{0.0, 5.0}));
    EXPECT_EQ(h.bounds()[1], (Bucket{1.0, 1.0}));
    EXPECT_EQ(h.bounds()[9], (Bucket{9.0, 1.0}));
    EXPECT_EQ(h.bounds()[10], (Bucket{10.0, 0.0})); // rightmost boundary, never been filled
}

Y_UNIT_TEST(hgramRound) {
    auto h = Hgram::equalSizedN(0.0, 4.0, 2);
    ++h[2.0];
    EXPECT_EQ(h.bounds()[0], (Bucket{0.0, 0.0}));
    EXPECT_EQ(h.bounds()[1], (Bucket{2.0, 1.0}));

    h = Hgram::equalSizedN(-4.0, 0.0, 2);
    ++h[-2.0];
    EXPECT_EQ(h.bounds()[1], (Bucket{-2.0, 1.0}));
    EXPECT_EQ(h.bounds()[2], (Bucket{0.0, 0.0}));
}

Y_UNIT_TEST(hgramBoundaries) {
    auto h = Hgram{
        {1.0, 0.0, 0.5} // should sort
    };
    EXPECT_EQ(h.bounds()[0], (Bucket{0.0, 0.0}));
    EXPECT_EQ(h.bounds()[1], (Bucket{0.5, 0.0}));
    EXPECT_EQ(h.bounds()[2], (Bucket{1.0, 0.0}));

    h = Hgram{
        {1.0, 1.0, 0.0, 0.5, 0.5, 0.0} // should remove duplicates
    };
    EXPECT_EQ(h.bounds()[0], (Bucket{0.0, 0.0}));
    EXPECT_EQ(h.bounds()[1], (Bucket{0.5, 0.0}));
    EXPECT_EQ(h.bounds()[2], (Bucket{1.0, 0.0}));
}

}
