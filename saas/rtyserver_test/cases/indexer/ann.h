#pragma once

#include <saas/rtyserver_test/util/factors_parsers.h>

#include <library/cpp/packedtypes/packedfloat.h>
#include <library/cpp/logger/global/global.h>
#include <util/generic/map.h>

namespace NSaas {

class TAnnFormats {
public:
    static TString GetStreamRawData(float value) {
        ui8 data = Float2Frac<ui8>(value);
        return TString((char*)&data, sizeof(data));
    }

    static ui32 GetStreamIntegerData(float value) {
        ui8 data = Float2Frac<ui8>(value);
        return data;
    }
};

class TDocFactorsView {
public:
    using TSearchResultProps = THashMultiMap<TString, TString>;
    using TData = TRTYFactorsParser::TDocFactorValues;
private:
    TData Data;
public:
    void Clear() {
        Data.clear();
    }

    const TData& GetData() const {
        return Data;
    }

    TData* MutableData() {
        return &Data;
    }
public:
    void AssignFromSearchResult(const TSearchResultProps& docProps) {
        Data = TRTYFactorsParser::GetJsonFactorsValues(docProps);
    }

    void DebugPrint() const {
        TMultiMap<TString, double> orderedData(Data.begin(), Data.end());
        for (auto f : orderedData) {
            DEBUG_LOG << f.first << "=" << f.second << Endl;
        }
    }

    double GetFactor(const TString& factor) const {
        TData::const_iterator iFactor = Data.find(factor);
        if (iFactor == Data.end())
            ythrow yexception() << "there is no " << factor << " in result";
        return iFactor->second;
    }

    double CheckFactor(const TString& factor, float value) const {
        const double f = GetFactor(factor);
        if (fabs(f - value) > 0.01)
            ythrow yexception() << "Invalid " << factor << " value: " << f << " != " << value;
        return f;
    }
};
}

