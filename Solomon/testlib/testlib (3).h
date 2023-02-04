#pragma once

#include <solomon/libs/cpp/ts_model/points.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace NSolomon::NTsModel {

using TPoints = ::testing::Types<
    TGaugePoint, TCounterPoint, TRatePoint, TIGaugePoint, THistPoint, THistRatePoint, TDSummaryPoint, TLogHistPoint>;
using TScalarPoints = ::testing::Types<
    TGaugePoint, TCounterPoint, TRatePoint, TIGaugePoint>;

template <typename TIt1, typename TIt2>
void ExpectIteratorsEq(TIt1 a, TIt2 b) {
    typename TIt1::TPoint p1;
    bool has1 = false;
    typename TIt2::TPoint p2;
    bool has2 = false;

    size_t index = 0;

    while (true) {
        has1 = a.NextPoint(&p1);
        has2 = b.NextPoint(&p2);

        if (!has1 || !has2) {
            break;
        }

        EXPECT_EQ(p1, p2) << "points #" << index << " differ";

        index++;
    }

    if (has1) {
        ADD_FAILURE() << "left iterator have more values than right iterator, first extra value is " << p1;
    }

    if (has2) {
        ADD_FAILURE() << "right iterator have more values than left iterator, first extra value is " << p2;
    }
}

/**
 * Get value from a scalar point.
 */
inline double ScalarValue(TGaugePoint point) {
    return point.ValueDivided();
}
inline double ScalarValue(TCounterPoint point) {
    return point.Value;
}
inline double ScalarValue(TRatePoint point) {
    return point.Value;
}
inline double ScalarValue(TIGaugePoint point) {
    return point.Value;
}

/**
 * Set value to a scalar point.
 */
inline void SetScalarValue(TGaugePoint* point, i64 value) {
    point->Num = value;
    point->Denom = 0;
}
inline void SetScalarValue(TCounterPoint* point, i64 value) {
    point->Value = value;
}
inline void SetScalarValue(TRatePoint* point, i64 value) {
    point->Value = value;
}
inline void SetScalarValue(TIGaugePoint* point, i64 value) {
    point->Value = value;
}

/**
 * Point constructors to make creation of test data easier.
 */
TGaugePoint Gauge(
    TStringBuf time, double num, ui64 denom,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TGaugePoint Gauge(
    TInstant time, double num, ui64 denom,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TCounterPoint Counter(
    TStringBuf time, i64 value,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TCounterPoint Counter(
    TInstant time, i64 value,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TRatePoint Rate(
    TStringBuf time, i64 value,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TRatePoint Rate(
    TInstant time, i64 value,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TIGaugePoint IGauge(
    TStringBuf time, i64 value,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TIGaugePoint IGauge(
    TInstant time, i64 value,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
THistPoint Hist(
    TStringBuf time, std::vector<THistPoint::TBucket> buckets, ui64 denom,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
THistPoint Hist(
    TInstant time, std::vector<THistPoint::TBucket> buckets, ui64 denom,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
THistRatePoint HistRate(
    TStringBuf time, std::vector<THistPoint::TBucket> buckets, ui64 denom,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
THistRatePoint HistRate(
    TInstant time, std::vector<THistPoint::TBucket> buckets, ui64 denom,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TDSummaryPoint DSummary(
    TStringBuf time, i64 countValue, double sum, double min, double max, double last,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TDSummaryPoint DSummary(
    TInstant time, i64 countValue, double sum, double min, double max, double last,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TLogHistPoint LogHist(
    TStringBuf time, i16 startPower, double base, std::vector<double> values, ui64 zeroCount = 0,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));
TLogHistPoint LogHist(
    TInstant time, i16 startPower, double base, std::vector<double> values, ui64 zeroCount = 0,
    bool merge = false, ui64 count = 1, TDuration step = TDuration::Seconds(15));

} // namespace NSolomon::NTsModel
