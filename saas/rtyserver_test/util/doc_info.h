#pragma once

#include <library/cpp/json/json_value.h>

#include <util/generic/hash.h>
#include <util/generic/string.h>

class TDocInfo {
public:
    typedef TString TSentBits;
    typedef THashMap<TString, TSentBits> TZonesInfo;
    typedef THashMap<TString, ui32> TZonesLengths;
    struct TMakeupDocInfo {
        TZonesLengths ZonesLengths;
        TZonesInfo ZonesInfo;
    };
    typedef THashMap<TString, float> TErfDocInfo;
    typedef TErfDocInfo TDDKDocInfo;
    typedef THashMap<TString, TErfDocInfo> TErfByKeyDocInfo;

    typedef TErfByKeyDocInfo TCSDocInfo; // CS["block"]["factor"] == value
    typedef THashMap<TString, TErfByKeyDocInfo> TQSDocInfo; // QS["block"]["key"]["factor"] == value
    typedef THashMap<TString, TVector<TString>> TFastArchiveInfo;
    struct TStreamInfo {
        ui32 Region = 0;
        ui32 Break = 0;
        ui32 Category = 0;
        TString Stream;
        ui32 Value;
    };
    typedef TVector<TStreamInfo> TAnnotationsInfo;

public:
    TDocInfo(const NJson::TJsonValue& jsonDocInfo) {
        ParseMakeupInfo(jsonDocInfo["info"]["MAKEUP"]);
        ParseErfInfo(jsonDocInfo["info"]["ERF"]);
        ParseDDKInfo(jsonDocInfo["info"]["DDK"]);
        ParseCSInfo(jsonDocInfo["info"]["CS"]);
        ParseQSInfo(jsonDocInfo["info"]["QS"]);
        ParseFastArchiveInfo(jsonDocInfo["info"]["FASTARC"]);
        ParseAnnotationsInfo(jsonDocInfo["info"]["Ann"]);
    }

    TMakeupDocInfo& GetMakeupDocInfo() {
        return MakeupInfo;
    }

    TErfDocInfo& GetErfDocInfo() {
        return ErfInfo;
    }

    TDDKDocInfo& GetDDKDocInfo() {
        return DDKInfo;
    }

    TCSDocInfo& GetCSDocInfo() {
        return CSInfo;
    }

    TQSDocInfo& GetQSDocInfo() {
        return QSInfo;
    }

    TFastArchiveInfo& GetFastArchiveInfo() {
        return FastArchiveInfo;
    }

    TAnnotationsInfo& GetAnnotationsInfo() {
        return AnnotationsInfo;
    }
private:
    void ParseMakeupInfo(const NJson::TJsonValue& jsonMakeupInfo) {
        const NJson::TJsonValue::TMapType& lengthsMap = jsonMakeupInfo["lengths"].GetMap();
        for (NJson::TJsonValue::TMapType::const_iterator length = lengthsMap.begin(); length != lengthsMap.end(); ++length) {
            MakeupInfo.ZonesLengths[length->first] = length->second.GetUIntegerRobust();
        }

        const NJson::TJsonValue::TMapType& infosMap = jsonMakeupInfo["zones"].GetMap();
        for (NJson::TJsonValue::TMapType::const_iterator zone = infosMap.begin(); zone != infosMap.end(); ++zone) {
            const TString& name = zone->first;
            TSentBits& bits = MakeupInfo.ZonesInfo[name];
            bits = zone->second.GetString();
        }
    }

    void ParseAnnotationsInfo(const NJson::TJsonValue& jsonInfo) {
        for (const auto& block : jsonInfo["ann"].GetArray()) {
            TStreamInfo info;
            info.Stream = block["stream"].GetStringRobust();
            info.Value = block["data"].GetInteger();
            info.Region = block["region"].GetInteger();
            info.Break = block["break"].GetInteger();
            info.Category = block["category"].GetInteger();
            AnnotationsInfo.push_back(info);
        }
    }

    void ParseFastArchiveInfo(const NJson::TJsonValue& value) {
        for (const auto& property : value.GetMap()) {
            const TString& name = property.first;
            for (const auto& v : property.second.GetArray())
                FastArchiveInfo[name].push_back(v.GetString());
        }
    }

    void ParseErfInfo(const NJson::TJsonValue& jsonErfInfo) {
        DoParseErfInfo(jsonErfInfo, ErfInfo);
    }

    void ParseDDKInfo(const NJson::TJsonValue& jsonDDKInfo) {
        DoParseErfInfo(jsonDDKInfo, DDKInfo);
    }

    void ParseCSInfo(const NJson::TJsonValue& jsonCSInfo) {
        const NJson::TJsonValue::TMapType& map = jsonCSInfo.GetMap();
        for (NJson::TJsonValue::TMapType::const_iterator i = map.begin(); i != map.end(); ++i) {
            const TString& name = i->first;
            DoParseErfInfo(i->second, CSInfo[name]);
        }
    }

    void ParseQSInfo(const NJson::TJsonValue& jsonQSInfo) {
        const NJson::TJsonValue::TMapType& blocks = jsonQSInfo.GetMap();
        for (NJson::TJsonValue::TMapType::const_iterator i = blocks.begin(); i != blocks.end(); ++i) {
            const TString& qsBlockName = i->first;
            const NJson::TJsonValue::TMapType& keys = i->second.GetMap();
            for (NJson::TJsonValue::TMapType::const_iterator j = keys.begin(); j != keys.end(); ++j) {
                const TString& key = j->first;
                DoParseErfInfo(j->second, QSInfo[qsBlockName][key]);
            }
        }
    }

    void DoParseErfInfo(const NJson::TJsonValue& json, TErfDocInfo& info) {
        const NJson::TJsonValue::TMapType& map = json.GetMap();
        for (NJson::TJsonValue::TMapType::const_iterator i = map.begin(); i != map.end(); ++i) {
            const TString& name = i->first;
            const float value = i->second.GetDoubleRobust();
            info[name] = value;
        }
    }
private:
    TMakeupDocInfo MakeupInfo;
    TErfDocInfo ErfInfo;
    TDDKDocInfo DDKInfo;
    TCSDocInfo CSInfo;
    TQSDocInfo QSInfo;
    TFastArchiveInfo FastArchiveInfo;
    TAnnotationsInfo AnnotationsInfo;
};
