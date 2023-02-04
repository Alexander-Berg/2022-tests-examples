#include "testlib.h"

namespace NSolomon::NSlog {

TDataPointBuilder::TDataPointBuilder(
        ui32 numId,
        NMonitoring::TLabels commonLabels,
        TInstant commonTime,
        TDuration step,
        NMonitoring::ECompression compression)
    : Meta_{new TString{}}
    , MetaStream_{new TStringOutput{*Meta_}}
    , MetaBuilder_{NSolomon::NSlog::NUnresolvedMeta::CreateUnresolvedMetaBuilder(
        numId,
        compression,
        MetaStream_.Get())}
    , Data_{new TString{}}
    , DataStream_{new TStringOutput{*Data_}}
    , DataBuilder_{NSolomon::NSlog::CreateLogDataBuilder(
        numId,
        NSolomon::NSlog::EDataCodingScheme::Slog,
        NMonitoring::ETimePrecision::SECONDS,
        compression,
        DataStream_.Get())}
{
    MetaBuilder_->OnCommonLabels(std::move(commonLabels));
    DataBuilder_->OnCommonTime(commonTime);
    DataBuilder_->OnStep(step);
}

TGaugeBuilder TDataPointBuilder::Gauge(NMonitoring::TLabels labels, bool merge) {
    return TGaugeBuilder{this, std::move(labels), merge};
}

TCounterBuilder TDataPointBuilder::Counter(NMonitoring::TLabels labels, bool merge) {
    return TCounterBuilder{this, std::move(labels), merge};
}

TRateBuilder TDataPointBuilder::Rate(NMonitoring::TLabels labels, bool merge) {
    return TRateBuilder{this, std::move(labels), merge};
}

TIGaugeBuilder TDataPointBuilder::IGauge(NMonitoring::TLabels labels, bool merge) {
    return TIGaugeBuilder{this, std::move(labels), merge};
}

THistBuilder TDataPointBuilder::Hist(NMonitoring::TLabels labels, bool merge) {
    return THistBuilder{this, std::move(labels), merge};
}

THistRateBuilder TDataPointBuilder::HistRate(NMonitoring::TLabels labels, bool merge) {
    return THistRateBuilder{this, std::move(labels), merge};
}

TDSummaryBuilder TDataPointBuilder::DSummary(NMonitoring::TLabels labels, bool merge) {
    return TDSummaryBuilder{this, std::move(labels), merge};
}

TLogHistBuilder TDataPointBuilder::LogHist(NMonitoring::TLabels labels, bool merge) {
    return TLogHistBuilder{this, std::move(labels), merge};
}

TGenericBuilder TDataPointBuilder::Generic(NMonitoring::TLabels labels, NTsModel::EPointType type, bool merge) {
    return TGenericBuilder{this, std::move(labels), type, merge};
}

TDataPoint TDataPointBuilder::Done() {
    MetaBuilder_->Close();
    MetaStream_->Flush();
    DataBuilder_->Close();
    DataStream_->Flush();

    return TDataPoint{std::move(*Meta_), std::move(*Data_)};
}

void TDataPointBuilder::OnTs(
        NMonitoring::TLabels labels,
        NTsModel::EPointType type,
        NSolomon::TAggrTimeSeries ts,
        bool merge)
{
    auto flags = NSolomon::CombineLogFlags(
        NSolomon::ELogFlags::Denom,
        NSolomon::ELogFlags::Count,
        merge ? NSolomon::ELogFlags::Merge : NSolomon::ELogFlags::None);
    size_t points = ts.Size();
    size_t bytes = DataBuilder_->OnTimeSeries(type, flags, std::move(ts));
    MetaBuilder_->OnMetric(type, std::move(labels), points, bytes);
}

TGaugeBuilder::TGaugeBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

TGaugeBuilder& TGaugeBuilder::Add(TStringBuf ts, double val, ui64 denom, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), val, denom, count);
}

TGaugeBuilder& TGaugeBuilder::Add(TInstant ts, double val, ui64 denom, ui64 count) {
    Ts_.Add(ts, val, denom, count);
    return *this;
}

TDataPointBuilder& TGaugeBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::DGauge, std::move(Ts_), Merge_);
    return *Builder_;
}

TCounterBuilder::TCounterBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

TCounterBuilder& TCounterBuilder::Add(TStringBuf ts, ui64 val, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), val, count);
}

TCounterBuilder& TCounterBuilder::Add(TInstant ts, ui64 val, ui64 count) {
    Ts_.Add(ts, val, 0, count);
    return *this;
}

TDataPointBuilder& TCounterBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::Counter, std::move(Ts_), Merge_);
    return *Builder_;
}

TRateBuilder::TRateBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

TRateBuilder& TRateBuilder::Add(TStringBuf ts, ui64 val, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), val, count);
}

TRateBuilder& TRateBuilder::Add(TInstant ts, ui64 val, ui64 count) {
    Ts_.Add(ts, val, 0, count);
    return *this;
}

TDataPointBuilder& TRateBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::Rate, std::move(Ts_), Merge_);
    return *Builder_;
}

TIGaugeBuilder::TIGaugeBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

TIGaugeBuilder& TIGaugeBuilder::Add(TStringBuf ts, i64 val, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), val, count);
}

TIGaugeBuilder& TIGaugeBuilder::Add(TInstant ts, i64 val, ui64 count) {
    Ts_.Add(ts, val, 0, count);
    return *this;
}

TDataPointBuilder& TIGaugeBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::IGauge, std::move(Ts_), Merge_);
    return *Builder_;
}

THistBuilder::THistBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

// NOLINTNEXTLINE(performance-unnecessary-value-param): false positive
THistBuilder& THistBuilder::Add(TStringBuf ts, NMonitoring::IHistogramSnapshotPtr val, ui64 denom, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), std::move(val), denom, count);
}

THistBuilder& THistBuilder::Add(TInstant ts, const NMonitoring::IHistogramSnapshotPtr& val, ui64 denom, ui64 count) {
    Ts_.Add(ts, val.Get(), denom, count);
    return *this;
}

TDataPointBuilder& THistBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::Hist, std::move(Ts_), Merge_);
    return *Builder_;
}

THistRateBuilder::THistRateBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

// NOLINTNEXTLINE(performance-unnecessary-value-param): false positive
THistRateBuilder& THistRateBuilder::Add(TStringBuf ts, NMonitoring::IHistogramSnapshotPtr val, ui64 denom, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), std::move(val), denom, count);
}

THistRateBuilder& THistRateBuilder::Add(TInstant ts, const NMonitoring::IHistogramSnapshotPtr& val, ui64 denom, ui64 count) {
    Ts_.Add(ts, val.Get(), denom, count);
    return *this;
}

TDataPointBuilder& THistRateBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::HistRate, std::move(Ts_), Merge_);
    return *Builder_;
}

TDSummaryBuilder::TDSummaryBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

// NOLINTNEXTLINE(performance-unnecessary-value-param): false positive
TDSummaryBuilder& TDSummaryBuilder::Add(TStringBuf ts, NMonitoring::ISummaryDoubleSnapshotPtr val, ui64 denom, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), std::move(val), denom, count);
}

TDSummaryBuilder& TDSummaryBuilder::Add(TInstant ts, const NMonitoring::ISummaryDoubleSnapshotPtr& val, ui64 denom, ui64 count) {
    Ts_.Add(ts, val.Get(), denom, count);
    return *this;
}

TDataPointBuilder& TDSummaryBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::DSummary, std::move(Ts_), Merge_);
    return *Builder_;
}

TLogHistBuilder::TLogHistBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
{
}

// NOLINTNEXTLINE(performance-unnecessary-value-param): false positive
TLogHistBuilder& TLogHistBuilder::Add(TStringBuf ts, NMonitoring::TLogHistogramSnapshotPtr val, ui64 denom, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), std::move(val), denom, count);
}

TLogHistBuilder& TLogHistBuilder::Add(TInstant ts, const NMonitoring::TLogHistogramSnapshotPtr& val, ui64 denom, ui64 count) {
    Ts_.Add(ts, val.Get(), denom, count);
    return *this;
}

TDataPointBuilder& TLogHistBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), NTsModel::EPointType::LogHist, std::move(Ts_), Merge_);
    return *Builder_;
}

TGenericBuilder::TGenericBuilder(TDataPointBuilder* builder, NMonitoring::TLabels labels, NTsModel::EPointType type, bool merge)
    : Labels_{std::move(labels)}
    , Merge_{merge}
    , Builder_{builder}
    , MetricType_{type}
{
}

TGenericBuilder& TGenericBuilder::Add(TStringBuf ts, i32 val, ui64 denom, ui64 count) {
    return Add(TInstant::ParseIso8601(ts), val, denom, count);
}

TGenericBuilder& TGenericBuilder::Add(TInstant ts, i32 val, ui64 denom, ui64 count) {
    switch (MetricType_) {
        case NTsModel::EPointType::DGauge:
            Ts_.Add(ts, static_cast<double>(val), denom, count);
            break;
        case NTsModel::EPointType::Counter:
            Ts_.Add(ts, static_cast<ui64>(val), denom, count);
            break;
        case NTsModel::EPointType::Rate:
            Ts_.Add(ts, static_cast<ui64>(val), denom, count);
            break;
        case NTsModel::EPointType::IGauge:
            Ts_.Add(ts, static_cast<i64>(val), denom, count);
            break;
        case NTsModel::EPointType::Hist:
            Ts_.Add(ts, NMonitoring::ExplicitHistogramSnapshot({1, 2}, {static_cast<ui64>(val), 0}).Get(), denom, count);
            break;
        case NTsModel::EPointType::HistRate:
            Ts_.Add(ts, NMonitoring::ExplicitHistogramSnapshot({1, 2}, {static_cast<ui64>(val), 0}).Get(), denom, count);
            break;
        case NTsModel::EPointType::DSummary:
            Ts_.Add(ts, new NMonitoring::TSummaryDoubleSnapshot(val, val, val, val, 1), denom, count);
            break;
        case NTsModel::EPointType::LogHist:
            Ts_.Add(ts, new NMonitoring::TLogHistogramSnapshot(2, 0, 0, {static_cast<double>(val), 0}), denom, count);
            break;
        default:
            Y_FAIL("unknown metric type");
    }
    return *this;
}

TDataPointBuilder& TGenericBuilder::Done() {
    Builder_->OnTs(std::move(Labels_), MetricType_, std::move(Ts_), Merge_);
    return *Builder_;
}

} // namespace NSolomon::NSlog
