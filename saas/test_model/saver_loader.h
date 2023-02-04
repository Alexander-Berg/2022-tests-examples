#pragma once

#include "model.h"
#include <util/folder/path.h>

struct TSaveLoadConfig {
    TFsPath RootDir;
};

class TFileWrapper;

class TModelSaver {
public:
    static const ui64 DefaultVersion;
    TModelSaver(const TSaveLoadConfig& config);
    void SaveTest(const TDataBase& db, const TTest& test, ui64 fromRevision, ui64 toRevision, ui64 version = DefaultVersion);
private:
    TFsPath GetDbPath(const TDataBase& db) const;
    TFsPath GetTestPath(const TDataBase& db, const TTest& test) const;
    void SaveDbData(const TDataBase& db, ui64 version);
    void SaveTestData(const TDataBase& db, const TTest& test, ui64 version);
    void SaveTestExecution(const TDataBase& db, const TTest& test, const TTestExecution& execution, ui64 version);
    const TSaveLoadConfig& Config;
};
