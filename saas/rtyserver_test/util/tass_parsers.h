#pragma once
#include <util/generic/string.h>
#include <library/cpp/json/json_reader.h>

class TRTYTassParser {
public:
    // The parser tries to interpret TUnistat Json "list of key-value pairs" as a map
    // (if it can't be interpreted in a such way, the parser will return false)
    class TTassCallbacks : public NJson::TParserCallbacks {
    public:
        TTassCallbacks(NJson::TJsonValue &value)
            : NJson::TParserCallbacks(value, /*throwOnError=*/ false)
        {
        }

        bool OnOpenArray() override {
            if (ValuesStack.size() == 0) {
                return NJson::TParserCallbacks::OnOpenMap();
            } else if (CurrentState == IN_MAP && ValuesStack.size() == 1) {
                return true; // Do nothing (an 2-nd level array is converted to a map key)
            }

            return NJson::TParserCallbacks::OnOpenArray();
        }

        bool OnCloseArray() override {
            if (CurrentState == IN_MAP) {
                if (ValuesStack.size() == 0)
                    return NJson::TParserCallbacks::OnCloseMap();
                if (ValuesStack.size() == 1)
                    return true; // A map key was succefully inserted
            }
            return NJson::TParserCallbacks::OnCloseArray();
        }

        bool OnString(const TStringBuf &val) override {
            if (CurrentState == IN_MAP && ValuesStack.size() == 1) {
                return NJson::TParserCallbacks::OnMapKey(val);
            }

            return NJson::TParserCallbacks::OnString(val);
        }
    };

    typedef TSimpleSharedPtr<NJson::TJsonValue> TJsonValuePtr;

    static TJsonValuePtr Parse(const TString& tassResults) {
        TJsonValuePtr out = MakeSimpleShared<NJson::TJsonValue>();
        TStringStream ss(tassResults);
        TTassCallbacks cb(*out);
        if (!NJson::ReadJson(&ss, /*allowComments=*/ true, &cb))
            out.Drop();
        return out;
    }

    static void GetFromValue(const NJson::TJsonValue* value, ui64* result) {
        *result = value->GetUInteger();
    }

    static void GetFromValue(const NJson::TJsonValue* value, i64* result) {
        *result = value->GetInteger();
    }

    template<typename TValue>
    static bool GetTassValue(const TString& tassResults, const TStringBuf &key, TValue* result) {
        TJsonValuePtr tass = Parse(tassResults);
        if (!tass)
            return false;

        const NJson::TJsonValue* value;
        if (!tass->GetValuePointer(key, &value))
            return false;

        if (result)
            GetFromValue(value, result);

        return true;
    }

    static bool GetHistogramTassValue(const TString& tassResults, TStringBuf key, double rangeMin, double rangeMax, double& result) {
        TJsonValuePtr tass = Parse(tassResults);
        if (!tass)
            return false;

        const NJson::TJsonValue* value;
        if (!tass->GetValuePointer(key, &value))
            return false;

        Y_ENSURE(value, "no key " << key);
        Y_ENSURE(value->IsArray(), key << " is not an array");

        result = 0;
        for (auto&& v : value->GetArray()) {
            const double range = v[0].GetDoubleRobust();
            if (range >= rangeMin && range <= rangeMax) {
                result += v[1].GetDoubleRobust();
            }
        }
        return true;
    }
};
