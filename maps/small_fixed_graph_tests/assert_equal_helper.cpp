#include "assert_equal_helper.h"

#include <library/cpp/testing/unittest/registar.h>

namespace {

template <typename T>
void assertEqual(T v1, T v2, double maxA = 1.0, double maxR = 0.01) {
    UNIT_ASSERT(v1.value() >= 0.0);
    UNIT_ASSERT(v2.value() >= 0.0);
    if (v1 == v2) return;
    const auto a = std::abs(v1.value() - v2.value());
    const auto r = a / std::min(v1.value(), v2.value());
    UNIT_ASSERT(a < maxA || r < maxR);
}

} // namespace

void assertEqual(leptidea7::Duration d1, leptidea7::Duration d2) {
    assertEqual<leptidea7::Duration>(d1, d2);
}

void assertEqual(leptidea7::Length l1, leptidea7::Length l2) {
    assertEqual<leptidea7::Length>(l1, l2);
}

void assertEqual(leptidea7::Penalty p1, leptidea7::Penalty p2) {
    assertEqual<leptidea7::Penalty>(p1, p2);
}

void assertEqual(leptidea7::Weight w1, leptidea7::Weight w2) {
    assertEqual(w1.duration(), w2.duration());
    assertEqual(w1.length(), w2.length());
    assertEqual(w1.penalty(), w2.penalty());
}
