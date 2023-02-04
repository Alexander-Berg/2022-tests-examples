#pragma once

#include <solomon/libs/cpp/log_writer/log_writer.h>

#include <library/cpp/monlib/metrics/labels.h>

namespace NSolomon::NTest {

struct TMetricValue {
    TMetricValue(TInstant time, double num, ui64 denom)
        : Time{time}
        , Numerator{num}
        , Denominator{denom}
    {}

    TInstant Time;
    double Numerator;
    ui64 Denominator;
};

using TMetricValues = TVector<TMetricValue>;

struct TMetric {
    NMonitoring::TLabels Labels;
    NMonitoring::EMetricType Kind{NMonitoring::EMetricType::UNKNOWN};
    TAggrTimeSeries TimeSeries;
};

class TCollectingLogWriter: public ILogWriter {
private:
    void Close() override {
    }

    void OnPoint(ELogFlagsComb flags, TAggrPointWithType&& aggrPoint) override {
        // TODO:
        Y_UNUSED(flags);

        Metrics.back().TimeSeries.Add(std::move(aggrPoint));
    }

    void OnTimeSeries(ELogFlagsComb flags, const TAggrTimeSeries& timeseries) override {
        // TODO:
        Y_UNUSED(flags);

        Metrics.back().TimeSeries = timeseries.Copy();
    }

    void OnStep(TDuration step) override {
        // TODO:
        Y_UNUSED(step);
    }

    void OnStreamBegin() override {
    }

    void OnStreamEnd() override {
    }

    void OnCommonTime(TInstant time) override {
        CommonTime = time;
    }

    void OnMetricBegin(NMonitoring::EMetricType kind) override {
        auto& metric = Metrics.emplace_back();
        metric.Kind = kind;
        IsInsideMetric = true;
    }

    void OnMetricEnd() override {
        IsInsideMetric = false;
    }

    void OnLabelsBegin() override {
    }

    void OnLabelsEnd() override {
    }

    void OnLabel(TStringBuf name, TStringBuf value) override {
        if (IsInsideMetric) {
            Metrics.back().Labels.Add(name, value);
        } else {
            CommonLabels.Add(name, value);
        }
    }

public:
    TVector<TMetric> Metrics;
    TInstant CommonTime;
    NMonitoring::TLabels CommonLabels;
    bool IsInsideMetric{false};
};

} // namespace NSolomon::NTest
