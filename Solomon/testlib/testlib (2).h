#pragma once

#include <solomon/libs/cpp/slog/slog.h>
#include <solomon/libs/cpp/slog/log_data.h>
#include <solomon/libs/cpp/slog/unresolved_meta/builder.h>

namespace NSolomon::NSlog {

struct TDataPoint {
    TString Meta;
    TString Data;
};

class TGaugeBuilder;
class TCounterBuilder;
class TRateBuilder;
class TIGaugeBuilder;
class THistBuilder;
class THistRateBuilder;
class TDSummaryBuilder;
class TLogHistBuilder;
class TGenericBuilder;

class TDataPointBuilder {
    friend class TGaugeBuilder;
    friend class TCounterBuilder;
    friend class TRateBuilder;
    friend class TIGaugeBuilder;
    friend class THistBuilder;
    friend class THistRateBuilder;
    friend class TDSummaryBuilder;
    friend class TLogHistBuilder;
    friend class TGenericBuilder;

public:
    explicit TDataPointBuilder(
        ui32 numId,
        NMonitoring::TLabels commonLabels,
        TInstant commonTime,
        TDuration step,
        NMonitoring::ECompression compression);

public:
    TGaugeBuilder Gauge(NMonitoring::TLabels labels, bool merge = false);
    TCounterBuilder Counter(NMonitoring::TLabels labels, bool merge = false);
    TRateBuilder Rate(NMonitoring::TLabels labels, bool merge = false);
    TIGaugeBuilder IGauge(NMonitoring::TLabels labels, bool merge = false);
    THistBuilder Hist(NMonitoring::TLabels labels, bool merge = false);
    THistRateBuilder HistRate(NMonitoring::TLabels labels, bool merge = false);
    TDSummaryBuilder DSummary(NMonitoring::TLabels labels, bool merge = false);
    TLogHistBuilder LogHist(NMonitoring::TLabels labels, bool merge = false);
    TGenericBuilder Generic(NMonitoring::TLabels labels, NTsModel::EPointType type, bool merge = false);

    TDataPoint Done();

private:
    void OnTs(
        NMonitoring::TLabels labels,
        NTsModel::EPointType type,
        NSolomon::TAggrTimeSeries ts,
        bool merge);

private:
    THolder<TString> Meta_;
    THolder<TStringOutput> MetaStream_;
    NSolomon::NSlog::NUnresolvedMeta::IUnresolvedMetaBuilderPtr MetaBuilder_;
    THolder<TString> Data_;
    THolder<TStringOutput> DataStream_;
    NSolomon::NSlog::ILogDataBuilderPtr DataBuilder_;
};

inline TDataPointBuilder MakeSlog(
        ui32 numId,
        NMonitoring::TLabels commonLabels = {},
        TInstant commonTime = TInstant::ParseIso8601("2000-01-01T00:00:00Z"),
        TDuration step = TDuration::Seconds(15),
        NMonitoring::ECompression compression = NMonitoring::ECompression::IDENTITY)
{
    return TDataPointBuilder{numId, std::move(commonLabels), commonTime, step, compression};
}

class TGaugeBuilder {
public:
    TGaugeBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    TGaugeBuilder& Add(TStringBuf ts, double val, ui64 denom = 0, ui64 count = 1);
    TGaugeBuilder& Add(TInstant ts, double val, ui64 denom = 0, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class TCounterBuilder {
public:
    TCounterBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    TCounterBuilder& Add(TStringBuf ts, ui64 val, ui64 count = 1);
    TCounterBuilder& Add(TInstant ts, ui64 val, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class TRateBuilder {
public:
    TRateBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    TRateBuilder& Add(TStringBuf ts, ui64 val, ui64 count = 1);
    TRateBuilder& Add(TInstant ts, ui64 val, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class TIGaugeBuilder {
public:
    TIGaugeBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    TIGaugeBuilder& Add(TStringBuf ts, i64 val, ui64 count = 1);
    TIGaugeBuilder& Add(TInstant ts, i64 val, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class THistBuilder {
public:
    THistBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    THistBuilder& Add(TStringBuf ts, NMonitoring::IHistogramSnapshotPtr val, ui64 denom = 0, ui64 count = 1);
    THistBuilder& Add(TInstant ts, const NMonitoring::IHistogramSnapshotPtr& val, ui64 denom = 0, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class THistRateBuilder {
public:
    THistRateBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    THistRateBuilder& Add(TStringBuf ts, NMonitoring::IHistogramSnapshotPtr val, ui64 denom = 0, ui64 count = 1);
    THistRateBuilder& Add(TInstant ts, const NMonitoring::IHistogramSnapshotPtr& val, ui64 denom = 0, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class TDSummaryBuilder {
public:
    TDSummaryBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    TDSummaryBuilder& Add(TStringBuf ts, NMonitoring::ISummaryDoubleSnapshotPtr val, ui64 denom = 0, ui64 count = 1);
    TDSummaryBuilder& Add(TInstant ts, const NMonitoring::ISummaryDoubleSnapshotPtr& val, ui64 denom = 0, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class TLogHistBuilder {
public:
    TLogHistBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge);

public:
    TLogHistBuilder& Add(TStringBuf ts, NMonitoring::TLogHistogramSnapshotPtr val, ui64 denom = 0, ui64 count = 1);
    TLogHistBuilder& Add(TInstant ts, const NMonitoring::TLogHistogramSnapshotPtr& val, ui64 denom = 0, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
};

class TGenericBuilder {
public:
    TGenericBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, NTsModel::EPointType type, bool merge);

public:
    TGenericBuilder& Add(TStringBuf ts, i32 val, ui64 denom = 0, ui64 count = 1);
    TGenericBuilder& Add(TInstant ts, i32 val, ui64 denom = 0, ui64 count = 1);
    TDataPointBuilder& Done();

private:
    NSolomon::TAggrTimeSeries Ts_;
    NMonitoring::TLabels Labels_;
    bool Merge_;
    TDataPointBuilder* Builder_;
    NTsModel::EPointType MetricType_;
};

} // namespace NSolomon::NSlog
