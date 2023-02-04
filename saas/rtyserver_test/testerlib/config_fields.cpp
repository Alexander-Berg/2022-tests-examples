#include "config_fields.h"

#include <library/cpp/logger/global/global.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/json/json_reader.h>
#include <util/stream/str.h>
#include <util/stream/file.h>


TString TConfigFields::Serialize() const {
    TStringStream requestFields;
    NJson::TJsonWriter writer(&requestFields, false);
    writer.OpenMap();
    for (TConfigFields::const_iterator i = begin(); i != end(); ++i) {
        writer.Write(i->first, i->second.Value);
    }
    writer.CloseMap();
    writer.Flush();
    return requestFields.Str();
}

TString TConfigFields::GetReadableString() const {
    TStringStream readableOut;
    for (const auto& paramsPair : *this) {
        readableOut << paramsPair.first << "=" << paramsPair.second.Value << ";";
    }
    return readableOut.Str();
}

void TConfigFields::Deserialize(const NJson::TJsonValue& data) {
    clear();
    const NJson::TJsonValue::TMapType& map = data.GetMap();
    for (NJson::TJsonValue::TMapType::const_iterator i = map.begin(); i != map.end(); ++i) {
        if (i->second.IsMap())
            operator [](i->first) = TConfigField(i->second["value"].GetString(), i->second["result"].GetBoolean());
        else
            operator [](i->first) = TConfigField(i->second.GetString());
    }
}

void TConfigFields::Deserialize(const TString& data) {
    TStringInput si(data);
    NJson::TJsonValue json;
    if (!NJson::ReadJsonTree(&si, &json))
        ythrow yexception() << "Errors in json";
    Deserialize(json);
}

void TConfigFields::Patch(TConfigFields& patch) {
    for (TConfigFields::iterator i = patch.begin(); i != patch.end(); ++i) {
        TConfigField& val = operator [](i->first);
        if (val.Value != i->second.Value) {
            val.Value = i->second.Value;
            DEBUG_LOG << "Value for " << i->first << " defined as " << i->second.Value << "(was " << val.Value << ")" << Endl;
            i->second.Changed = true;
        }
    }
}

TConfigPatches::TConfigPatches()
{}

TConfigPatches::TConfigPatches(const TString& path, const NJson::TJsonValue::TArray& json)
{
    VERIFY_WITH_LOG(!json.empty(), "Trying to construct pathes from empty list of alternatives");
    for (const auto& oneValue : json) {
        TConfigFieldsPtr newPatch(new TConfigFields);
        (*newPatch)[path] = oneValue.GetStringRobust();
        push_back(newPatch);
    }
}

void TConfigPatchesStorage::AddAlternatives(const TConfigPatches& patches) {
    Patches.push_back(patches);
}

TConfigPatchesStorage::TConfigPatchesStorage(const TString& jsonWithPatches)
{
    if (jsonWithPatches == TString())
        return;

    TAutoPtr<IInputStream> input;
    if (TFsPath(jsonWithPatches).Exists()) {
        input.Reset(new TUnbufferedFileInput(jsonWithPatches));
    } else {
        input.Reset(new TStringStream(jsonWithPatches));
    }
    NJson::TJsonValue val;

    if (!NJson::ReadJsonTree(input.Get(), &val)) {
        ythrow yexception() << "Invalid json with patches: " << jsonWithPatches << Endl;
        return;
    }

    if (!val.IsMap()) {
        ythrow yexception() << "patches are not map: " << jsonWithPatches << Endl;
    }

    for (const auto& oneConfigVarVals : val.GetMap()) {
        TConfigPatches alternatives(oneConfigVarVals.first, oneConfigVarVals.second.GetArray());
        Patches.push_back(alternatives);
    }
}

void GeneratePatches(TVector<TConfigPatches>::const_iterator start, TVector<TConfigPatches>::const_iterator end,
                     TConfigFieldsPtr constructedPatch, TConfigPatches* result)
{
    if (start == end) {
        result->push_back(new TConfigFields(*constructedPatch));
    } else {
        for (const auto& val : *start) {
            constructedPatch->Patch(*val);
            auto newStart = start;
            newStart++;
            GeneratePatches(newStart, end, constructedPatch, result);
        }
    }
}

void TConfigPatchesStorage::BuildAllCombinations(TConfigPatches& result) {
    TConfigFieldsPtr constructedPatch(new TConfigFields);
    GeneratePatches(Patches.begin(), Patches.end(), constructedPatch, &result);
}
