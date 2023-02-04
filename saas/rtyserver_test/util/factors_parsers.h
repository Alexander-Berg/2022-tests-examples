#pragma once

#include <util/generic/vector.h>
#include <util/generic/yexception.h>
#include <util/generic/string.h>
#include <util/generic/ptr.h>
#include <library/cpp/json/json_reader.h>

class TRTYFactorsParser {
public:
    using TDocFactorValues = THashMap<TString, double>;
public:
    inline static TDocFactorValues GetJsonFactorsValues(const THashMultiMap<TString, TString>& docProps) {
        THashMultiMap<TString, TString>::const_iterator iter = docProps.find("_JsonFactors");
        if (iter == docProps.end())
            ythrow yexception() << "there is no _JsonFactors in result";
        NJson::TJsonValue value;
        TStringStream ss;
        ss << iter->second;
        NJson::ReadJsonTree(&ss, true, &value);
        NJson::TJsonValue::TArray arr;
        if (!value.GetArray(&arr))
            ythrow yexception() << "incorrect _JsonFactors format";

        THashMap<TString, double> docFactors;
        for (NJson::TJsonValue::TArray::const_iterator ji = arr.begin(), je = arr.end(); ji != je; ++ji) {
            NJson::TJsonValue::TMapType valMap;
            if (!ji->GetMap(&valMap)) {
                ythrow yexception() << "incorrect _JsonFactors format (map)";
            }
            double factorValue;
            if (!valMap.begin()->second.GetDouble(&factorValue)) {
                ythrow yexception() << "incorrect _JsonFactors format (str)";
            }
            docFactors[valMap.begin()->first] = factorValue;
        }
        return docFactors;
    }

    inline static TVector<TDocFactorValues> GetJsonFactorsValues(const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >& resultProps) {
        const size_t nDocs = resultProps.size();
        TVector<THashMap<TString, double> > factors;
        factors.reserve(nDocs);
        for (size_t i = 0; i < nDocs; ++i) {
            factors.emplace_back(GetJsonFactorsValues(*resultProps[i]));
        }
        return factors;
    }
};
