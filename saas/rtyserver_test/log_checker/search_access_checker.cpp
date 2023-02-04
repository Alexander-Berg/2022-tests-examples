#include "search_access_checker.h"

#include "tskv_log_record.h"
#include "file_reader.h"

#include <util/generic/algorithm.h>

namespace {

    const TVector<TString> RequiredKeys = {
        "tskv-format",
        "id",
        "service",
        "fake-uid",
        "cachehit",
        "timeouted",
    };

    class TAccessLogRecord : public NUtil::TTskvLogRecord<> {
    public:
        TAccessLogRecord(const TString& description)
            : TTskvLogRecord(description)
        {
        }

        bool KeepValue(const TString& key) const override {
            return IsIn(RequiredKeys, key);
        }
    };

    class TAccessLogReader : public NUtil::TLogFileReader {
    public:
        TAccessLogReader(const TString& fileName)
            : TLogFileReader(fileName)
        {
        }

    private:
        NUtil::IStreamLogRecord* CreateLogRecord(const TString& description) const override {
            return new TAccessLogRecord(description);
        }
    };

}

NUtil::ILogReader::TPtr TSearchProxyAccessLogChecker::CreateLogReader(const TString& fileName) const {
    return new TAccessLogReader(fileName);
}

TString TSearchProxyAccessLogChecker::GetLoggerType() const {
    return "SearchProxy.Logger.InfoLog";
}

TRtyTestNodeType TSearchProxyAccessLogChecker::GetBinaryType() const {
    return TNODE_SEARCHPROXY;
}

TLogChecker::TFactory::TRegistrator<TSearchProxyAccessLogChecker> TSearchProxyAccessLogChecker::Registrator("search-access");
