#pragma once

#include <library/cpp/json/json_value.h>
#include <library/cpp/charset/ci_string.h>
#include <util/generic/map.h>
#include <util/string/cast.h>


struct TConfigField {
    TConfigField()
        : Changed(false)
    {}

    TConfigField(const TString& value, bool changed = false)
        : Value(value)
        , Changed(changed)
    {}

    template<class T>
    TConfigField(const T& value)
        : Value(ToString(value))
        , Changed(false)
    {}

    TString Value;
    bool Changed;
};

class TConfigFields : public TMap<TCiString, TConfigField> {
public:
    TString Serialize() const;
    TString GetReadableString() const;
    void Deserialize(const TString& data);
    void Deserialize(const NJson::TJsonValue& data);
    void Patch(TConfigFields& patch);

    using TPtr = TAtomicSharedPtr<TConfigFields>;
};

typedef TConfigFields::TPtr TConfigFieldsPtr;
typedef TSimpleSharedPtr<NJson::TJsonValue> TJsonPtr;

class TConfigPatches : public TVector<TConfigFieldsPtr> {
public:
    TConfigPatches();
    TConfigPatches(const TString& path, const NJson::TJsonValue::TArray& json);
};

class TConfigPatchesStorage {
public:
    TConfigPatchesStorage(const TString& jsonWithPatches);
    void AddAlternatives(const TConfigPatches& patch);
    void BuildAllCombinations(TConfigPatches&);

private:
    TVector<TConfigPatches> Patches;
};
