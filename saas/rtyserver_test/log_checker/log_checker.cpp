#include "log_checker.h"
#include "parsed_log.h"

#include <util/folder/path.h>
#include <util/system/env.h>
#include <util/system/fs.h>

TLogChecker::TLogChecker(const TString& logType)
    : LogType(logType)
{
    CHECK_WITH_LOG(LogType);
}

void TLogChecker::Prepare(const TString& logFileName, bool prefixed, const TString& testName, const TString& resourcesDir) {
    Prefixed = prefixed;
    LogFileName = logFileName;

    CanonFileName = resourcesDir + "/logging/rtyserver/" + testName + "." + LogType + (Prefixed ? ".kps" : "") + ".log";
    if (!TFsPath(CanonFileName).Exists())
        ythrow yexception() << "File not found: " << CanonFileName;

    CanonLog = ReadLog(CanonFileName);
    CHECK_WITH_LOG(CanonLog);
}

void TLogChecker::Check() {
    CHECK_WITH_LOG(LogFileName);
    if (!TFsPath(LogFileName).Exists())
        ythrow yexception() << "File not found: " << LogFileName;

    NUtil::ILog::TPtr log = ReadLog(LogFileName);
    CHECK_WITH_LOG(log);

    TString info;
    int result = log->Compare(CanonLog, info);
    if (result != 0) {
        TString target = GetEnv("LOG_PATH");
        if (!target)
            target = ".";
        target += "/" + TFsPath(CanonFileName).Basename();
        NFs::Copy(LogFileName, target);
        DEBUG_LOG << "Log file " << LogFileName << " copied into " << target << Endl;
        ythrow yexception() << "Fields values check result: " << info;
    }
}

NUtil::ILog::TPtr TLogChecker::ReadLog(const TString& fileName) const {
    NUtil::ILogReader::TPtr reader = CreateLogReader(fileName);
    NUtil::ILog::TPtr log = CreateLog();
    log->Read(reader);
    return log;
}

NUtil::ILog::TPtr TLogChecker::CreateLog() const {
    return new NUtil::TParsedLog;
}
