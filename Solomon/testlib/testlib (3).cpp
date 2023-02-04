#include "testlib.h"

namespace NSolomon::NTsModel {

TGaugePoint Gauge(TStringBuf time, double num, ui64 denom, bool merge, ui64 count, TDuration step) {
    return Gauge(TInstant::ParseIso8601(time), num, denom, merge, count, step);
}

TGaugePoint Gauge(TInstant time, double num, ui64 denom, bool merge, ui64 count, TDuration step) {
    TGaugePoint res;
    res.Time = time;
    res.Num = num;
    res.Denom = denom;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

TCounterPoint Counter(TStringBuf time, i64 value, bool merge, ui64 count, TDuration step) {
    return Counter(TInstant::ParseIso8601(time), value, merge, count, step);
}

TCounterPoint Counter(TInstant time, i64 value, bool merge, ui64 count, TDuration step) {
    TCounterPoint res;
    res.Time = time;
    res.Value = value;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

TRatePoint Rate(TStringBuf time, i64 value, bool merge, ui64 count, TDuration step) {
    return Rate(TInstant::ParseIso8601(time), value, merge, count, step);
}

TRatePoint Rate(TInstant time, i64 value, bool merge, ui64 count, TDuration step) {
    TRatePoint res;
    res.Time = time;
    res.Value = value;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

TIGaugePoint IGauge(TStringBuf time, i64 value, bool merge, ui64 count, TDuration step) {
    return IGauge(TInstant::ParseIso8601(time), value, merge, count, step);
}

TIGaugePoint IGauge(TInstant time, i64 value, bool merge, ui64 count, TDuration step) {
    TIGaugePoint res;
    res.Time = time;
    res.Value = value;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

THistPoint Hist(TStringBuf time, std::vector<THistPoint::TBucket> buckets, ui64 denom, bool merge, ui64 count, TDuration step) {
    return Hist(TInstant::ParseIso8601(time), std::move(buckets), denom, merge, count, step);
}

THistPoint Hist(TInstant time, std::vector<THistPoint::TBucket> buckets, ui64 denom, bool merge, ui64 count, TDuration step) {
    THistPoint res;
    res.Time = time;
    res.Buckets = std::move(buckets);
    res.Denom = denom;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

THistRatePoint HistRate(TStringBuf time, std::vector<THistPoint::TBucket> buckets, ui64 denom, bool merge, ui64 count, TDuration step) {
    return HistRate(TInstant::ParseIso8601(time), std::move(buckets), denom, merge, count, step);
}

THistRatePoint HistRate(TInstant time, std::vector<THistPoint::TBucket> buckets, ui64 denom, bool merge, ui64 count, TDuration step) {
    THistRatePoint res;
    res.Time = time;
    res.Buckets = std::move(buckets);
    res.Denom = denom;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

TDSummaryPoint DSummary(TStringBuf time, i64 countValue, double sum, double min, double max, double last, bool merge, ui64 count, TDuration step) {
    return DSummary(TInstant::ParseIso8601(time), countValue, sum, min, max, last, merge, count, step);
}

TDSummaryPoint DSummary(TInstant time, i64 countValue, double sum, double min, double max, double last, bool merge, ui64 count, TDuration step) {
    TDSummaryPoint res;
    res.Time = time;
    res.CountValue = countValue;
    res.Sum = sum;
    res.Min = min;
    res.Max = max;
    res.Last = last;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

TLogHistPoint LogHist(TStringBuf time, i16 startPower, double base, std::vector<double> values, ui64 zeroCount, bool merge, ui64 count, TDuration step) {
    return LogHist(TInstant::ParseIso8601(time), startPower, base, std::move(values), zeroCount, merge, count, step);
}

TLogHistPoint LogHist(TInstant time, i16 startPower, double base, std::vector<double> values, ui64 zeroCount, bool merge, ui64 count, TDuration step) {
    TLogHistPoint res;
    res.Time = time;
    res.StartPower = startPower;
    res.Base = base;
    res.Values = std::move(values);
    res.ZeroCount = zeroCount;
    res.Step = step;
    res.Merge = merge;
    res.Count = count;
    return res;
}

} // namespace NSolomon::NTsModel
