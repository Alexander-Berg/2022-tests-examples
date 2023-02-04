#include "matchers.h"

#include <util/generic/hash_set.h>

#include <utility>

using namespace testing;
using namespace NMonitoring;

namespace NSolomon {
namespace {
    bool MatchLabels(const TLabels& expected, const TLabels& actual, MatchResultListener* listener) {
        auto makeLabelSet = [] (auto&& src) {
            THashSet<TLabel> set;
            Transform(src.begin(), src.end(), std::inserter(set, set.begin()),
                [] (auto&& label) {
                    return TLabel{label.Name(), label.Value()};
                });
            return set;
        };

        auto expectedSet = makeLabelSet(expected);
        auto actualSet = makeLabelSet(actual);

        TVector<TLabel> extra;

        for (auto&& l: actualSet) {
            if (expectedSet.contains(l)) {
                expectedSet.erase(l);
                continue;
            }

            extra.emplace_back(l);
        }

        const auto isMatch = expectedSet.empty() && extra.empty();

        if (!isMatch) {
            if (!extra.empty()) {
                *listener << "extra labels: ";
                for (auto&& l: extra) {
                    *listener << l;
                }
            }

            if (!expectedSet.empty()) {
                *listener << "missing labels: ";
                for (auto&& l: expectedSet) {
                    *listener << l;
                }
            }
        }

        return isMatch;
    }

} // namespace
    Matcher<TLabels> LabelsEq(TLabels expected) {
        return MakeMatcher(new TLabelsMatcher{std::move(expected)});
    }

    TLabelsMatcher::TLabelsMatcher(TLabels labels)
        : Expected_{std::move(labels)}
    {
    }

    bool TLabelsMatcher::MatchAndExplain(TLabels labels, MatchResultListener* listener) const {
        return MatchLabels(Expected_, labels, listener);
    }

    TMetricMatcher::TMetricMatcher(const TMetricData& metric)
        : Expected_{metric}
    {
    }

    bool TMetricMatcher::MatchAndExplain(const TMetricData* metric, MatchResultListener* listener) const {
        if (metric->Kind != Expected_.Kind) {
            *listener << " metric kind mismatch: " << metric->Kind << " and " << Expected_.Kind;
            return false;
        }

        if (!MatchLabels(Expected_.Labels, metric->Labels, listener)) {
            return false;
        }

        if (NMonitoring::EMetricType::UNKNOWN == metric->Kind) {
            return true;
        } else {
            if (metric->Values->Size() != Expected_.Values->Size()) {
                *listener << " time series size mismatch";
                return false;
            }

            for (auto i = 0u; i < metric->Values->Size(); ++i) {
                auto r = (*metric->Values)[i];
                auto l = (*Expected_.Values)[i];

                if (r.GetTime() != l.GetTime()) {
                    *listener << " metric time mismatch: " << r.GetTime().Seconds() << "s "
                                                 << "and " << l.GetTime().Seconds() << "s";
                    return false;
                }

                const auto& rValue = r.GetValue();
                const auto& lValue = l.GetValue();

#define VALUE_EQ(r, l)                                                       \
    do {                                                                     \
        if (!((r) == (l))) {                                                  \
            *listener << "metric value mismatch: " << (r) << " and " << (l); \
            return false;                                                    \
        }                                                                    \
    } while (false)

                switch (metric->Kind) {
                    case NMonitoring::EMetricType::GAUGE:
                        VALUE_EQ(rValue.AsDouble(), lValue.AsDouble());
                        break;
                    case NMonitoring::EMetricType::IGAUGE:
                        VALUE_EQ(rValue.AsInt64(), lValue.AsInt64());
                        break;
                    case NMonitoring::EMetricType::COUNTER:
                    case NMonitoring::EMetricType::RATE:
                        VALUE_EQ(rValue.AsUint64(), lValue.AsUint64());
                        break;
                    case NMonitoring::EMetricType::HIST:
                    case NMonitoring::EMetricType::HIST_RATE:
                        VALUE_EQ(*rValue.AsHistogram(), *lValue.AsHistogram());
                        break;
                    case NMonitoring::EMetricType::LOGHIST:
                        VALUE_EQ(*rValue.AsLogHistogram(), *lValue.AsLogHistogram());
                        break;
                    case NMonitoring::EMetricType::DSUMMARY:
                        VALUE_EQ(*rValue.AsSummaryDouble(), *lValue.AsSummaryDouble());
                        break;
                    case NMonitoring::EMetricType::UNKNOWN:
                        Y_UNREACHABLE();
                }
#undef VALUE_EQ
            }
        };

        return true;
    }

    testing::Matcher<const TMetricData*> MetricEq(const TMetricData& expected) {
        return MakeMatcher(new TMetricMatcher(expected));
    }

    class TMetricsMatcher: public testing::MatcherInterface<const TVector<TMetricData>&> {
        TVector<::testing::Matcher<const TMetricData*>> MakeMatchers(const TVector<TMetricData>& v) {
            TVector<::testing::Matcher<const TMetricData*>> result;
            for (auto&& m: v) {
                result.push_back(MetricEq(m));
            }

            return result;
        }

    public:
        enum class EMatchType {
            Subset = 0,
            Superset = 1,
            Exact = 2,
        };

        TMetricsMatcher(const TVector<TMetricData>& expected, EMatchType matchType = EMatchType::Exact)
            : Expected_{expected}
            , Matchers_{MakeMatchers(Expected_)}
            , MatchType_{matchType}
        {
        }

        bool MatchAndExplain(const TVector<TMetricData>& actual, testing::MatchResultListener* listener) const override {
            using namespace ::testing;
            auto f = [] (const TMetricData& ref) -> const TMetricData* {
                return &ref;
            };

            Matcher<decltype(actual)> sizeMatcher;
            Matcher<decltype(actual)> m;
            switch (MatchType_) {
            case EMatchType::Subset:
                sizeMatcher = SizeIs(Le(actual.size()));
                m = Each(ResultOf(f, AnyOfArray(Matchers_)));
                break;
            case EMatchType::Superset: {
                sizeMatcher = SizeIs(Ge(actual.size()));
                auto ff = [&] (auto&& v) {
                    TVector<const TMetricData*> actualPtrs;
                    Transform(v.begin(), v.end(), std::back_inserter(actualPtrs), f);
                    return actualPtrs;
                };

                m = ResultOf(ff, IsSupersetOf(Matchers_));
                break;
            }
            case EMatchType::Exact:
                sizeMatcher = SizeIs(actual.size());
                m = Each(ResultOf(f, AnyOfArray(Matchers_)));
                break;
            }

            // using AllOf to combine matchers results in attempt to copy TMetricData
            if (!ExplainMatchResult(sizeMatcher, actual, listener)) {
                return false;
            }

            return ExplainMatchResult(m, actual, listener);
        }

        void DescribeTo(std::ostream* os) const override {
            switch (MatchType_) {
            case EMatchType::Subset:
                *os << " metric list is subset of " << Expected_;
                break;
            case EMatchType::Superset:
                *os << " metric list is superset of " << Expected_;
                break;
            case EMatchType::Exact:
                *os << " metric lists match " << Expected_;
                break;
            }
        }

        void DescribeNegationTo(std::ostream* os) const override {
            switch (MatchType_) {
            case EMatchType::Subset:
                *os << " metric list is not subset of " << Expected_;
                break;
            case EMatchType::Superset:
                *os << " metric list is not superset of " << Expected_;
                break;
            case EMatchType::Exact:
                *os << " metric lists don't match " << Expected_;
                break;
            }
        }

    private:
        const TVector<TMetricData>& Expected_;
        const TVector<::testing::Matcher<const TMetricData*>> Matchers_;
        EMatchType MatchType_;
    };

    testing::Matcher<const TVector<TMetricData>&> MetricsEq(const TVector<TMetricData>& expected) {
        return MakeMatcher(new TMetricsMatcher(expected, TMetricsMatcher::EMatchType::Exact));
    }

    testing::Matcher<const TVector<TMetricData>&> MetricsIsSuperset(const TVector<TMetricData>& expected) {
        return MakeMatcher(new TMetricsMatcher(expected, TMetricsMatcher::EMatchType::Superset));
    }

    testing::Matcher<const TVector<TMetricData>&> MetricsIsSubset(const TVector<TMetricData>& expected) {
        return MakeMatcher(new TMetricsMatcher(expected, TMetricsMatcher::EMatchType::Subset));
    }
} // namespace NSolomon
