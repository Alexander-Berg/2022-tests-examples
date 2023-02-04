#pragma once

#include <library/cpp/logger/global/global.h>
#include <util/system/mutex.h>
#include <util/generic/hash.h>

struct TRevisionInfo {
    ui64 Revision;
    TString Comment;
    TString Author;
    time_t Timestamp;
    TRevisionInfo()
        : Revision(0)
        , Timestamp(0)
    {}
};

class TSvnInfo {
public:
    TSvnInfo();
    struct TConfig {
        TString Url;
        TString UserName;
        TString CertPath;
        TString Password;
    };
    TRevisionInfo GetRevisionInfo(ui64 number);
    void StoreRevisionInfo(const TRevisionInfo& info);
    void SetConfig(const TConfig& config);

private:
    bool DownLoadRevision(TRevisionInfo& result);

private:
    typedef THashMap<ui64, TRevisionInfo> TRevisionStorage;
    TMutex Mutex;
    TRevisionStorage Storage;
    const TConfig* Config;
};
