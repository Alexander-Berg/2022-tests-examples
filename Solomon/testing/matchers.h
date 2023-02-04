#pragma once

#include <library/cpp/testing/gtest/gtest.h>

#include <library/cpp/monlib/consumers/collecting_consumer.h>
#include <library/cpp/monlib/metrics/labels.h>
#include <library/cpp/monlib/metrics/metric_type.h>

namespace NMonitoring {
    inline std::ostream& operator<<(std::ostream& os, const NMonitoring::TLabel& label) {
        os << label.ToString();
        return os;
    }

    inline std::ostream& operator<<(std::ostream& os, NMonitoring::EMetricType type) {
        os << MetricTypeToStr(type);
        return os;
    }

    inline bool operator==(const NMonitoring::TLogHistogramSnapshot& lhs, const NMonitoring::TLogHistogramSnapshot& rhs) {
        return lhs.StartPower() == rhs.StartPower()
            && lhs.Count() == rhs.Count()
            && lhs.Base() == rhs.Base();
    }

    inline bool operator==(const NMonitoring::IHistogramSnapshot& lhs, const NMonitoring::IHistogramSnapshot& rhs) {
        if (lhs.Count() != rhs.Count()) {
            return false;
        }

        auto isMatch = true;
        for (auto i = 0u; i < lhs.Count(); ++i) {
            isMatch |= lhs.UpperBound(i) == rhs.UpperBound(i) && lhs.Value(i) == rhs.Value(i);
        }

        return isMatch;
    }

    inline bool operator==(const NMonitoring::ISummaryDoubleSnapshot& lhs, const NMonitoring::ISummaryDoubleSnapshot& rhs) {
        return lhs.GetCount() == rhs.GetCount()
            && lhs.GetSum() == rhs.GetSum()
            && lhs.GetLast() == rhs.GetLast()
            && lhs.GetMax() == rhs.GetMax()
            && lhs.GetMin() == rhs.GetMin();
    }
}

namespace NSolomon {
    inline std::ostream& operator<<(std::ostream& os, const NMonitoring::TMetricData* metric) {
        TStringBuilder sb;
        sb << "[ " << metric->Kind << ' ' << metric->Labels << ' ';
            if (NMonitoring::EMetricType::UNKNOWN == metric->Kind) {
                sb << "<null>";
            } else {
                metric->Values->ForEach([&] (auto ts, auto type, auto&& val) {
                    switch (type) {
                        case NMonitoring::EMetricValueType::DOUBLE:
                            sb << '{' << ts << ':' << val.AsDouble() << '}';
                            break;
                        case NMonitoring::EMetricValueType::INT64:
                            sb << '{' << ts << ':' << val.AsInt64() << '}';
                            break;
                        case NMonitoring::EMetricValueType::UINT64:
                            sb << '{' << ts << ':' << val.AsUint64() << '}';
                            break;
                        case NMonitoring::EMetricValueType::HISTOGRAM:
                            sb << '{' << ts << ':' << *val.AsHistogram() << '}';
                            break;
                        case NMonitoring::EMetricValueType::SUMMARY:
                            sb << '{' << ts << ':' << *val.AsSummaryDouble() << '}';
                            break;
                        case NMonitoring::EMetricValueType::LOGHISTOGRAM:
                            sb << '{' << ts << ':' << *val.AsLogHistogram() << '}';
                            break;
                        case NMonitoring::EMetricValueType::UNKNOWN:
                            Y_UNREACHABLE();
                    }
                });
            }

        sb << ']';
        os << sb;
        return os;
    }

    inline std::ostream& operator<<(std::ostream& os, const NMonitoring::TMetricData& metric) {
        os << (&metric);
        return os;
    }

    inline std::ostream& operator<<(std::ostream& os, const TVector<NMonitoring::TMetricData>& v) {
        os << '{';
        for (auto i = 0u; i < v.size(); ++i) {
            os << v[i];
            if (i < v.size() - 1) {
                os << ", ";
            }
        }

        os << '}';
        return os;
    }

    class TLabelsMatcher: public testing::MatcherInterface<NMonitoring::TLabels> {
    public:
        TLabelsMatcher(NMonitoring::TLabels labels);
        bool MatchAndExplain(NMonitoring::TLabels labels, testing::MatchResultListener* listener) const override;

        void DescribeTo(std::ostream* os) const override {
            *os << " lables match";
        }

        void DescribeNegationTo(std::ostream* os) const override {
            *os << " lables don't match";
        }

    private:
        NMonitoring::TLabels Expected_;
    };

    testing::Matcher<NMonitoring::TLabels> LabelsEq(NMonitoring::TLabels expected);

    class TMetricMatcher: public testing::MatcherInterface<const NMonitoring::TMetricData*> {
    public:
        TMetricMatcher(const NMonitoring::TMetricData& metric);
        bool MatchAndExplain(const NMonitoring::TMetricData* metric, testing::MatchResultListener* listener) const override;

        void DescribeTo(std::ostream* os) const override {
            *os << " metrics match";
        }

        void DescribeNegationTo(std::ostream* os) const override {
            *os << " metrics don't match";
        }

    private:
        const NMonitoring::TMetricData& Expected_;
    };

    testing::Matcher<const NMonitoring::TMetricData*> MetricEq(const NMonitoring::TMetricData& expected);

    testing::Matcher<const TVector<NMonitoring::TMetricData>&> MetricsEq(const TVector<NMonitoring::TMetricData>& expected);
    testing::Matcher<const TVector<NMonitoring::TMetricData>&> MetricsIsSuperset(const TVector<NMonitoring::TMetricData>& expected);
    testing::Matcher<const TVector<NMonitoring::TMetricData>&> MetricsIsSubset(const TVector<NMonitoring::TMetricData>& expected);
} // namespace NSolomon
