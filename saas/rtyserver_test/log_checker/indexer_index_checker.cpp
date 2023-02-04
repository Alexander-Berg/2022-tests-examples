#include "indexer_index_checker.h"

#include <dict/dictutil/str.h>

#include <util/generic/algorithm.h>

#include "tskv_log_record.h"
#include "file_reader.h"
#include "parsed_log.h"

namespace {

    const TVector<TString> RequiredKeys = {
        "http_code",
        "from_meta",
        "origin",
        "service",
        "source",
        "status",
        "url",
        "key", // docurl
    };

    class TIndexLogRecord : public NUtil::TTskvLogRecord<> {
    public:
        TIndexLogRecord(const TString& description)
            : TTskvLogRecord(description)
        {
        }

        bool KeepValue(const TString& key) const override {
            return IsIn(RequiredKeys, key);
        }

        bool IsValuableKey(const TString& key) const override {
            return key != "key" && key != "id" && key != "unixtime" && key != "iso_eventtime";
        }
    };

    class TIndexLogReader : public NUtil::TLogFileReader {
    public:
        TIndexLogReader(const TString& fileName)
            : TLogFileReader(fileName)
        {
        }

    private:
        NUtil::IStreamLogRecord* CreateLogRecord(const TString& description) const override {
            return new TIndexLogRecord(description);
        }
    };

    class TIndexLog : public NUtil::TParsedLog {
        void CompareStart() const override {
            PrevDocUrl.clear();
        }

        bool IsValuableRecord(NUtil::ILogRecord::TPtr record) const override {
            NUtil::TParsedLogRecord* ptr = dynamic_cast<NUtil::TParsedLogRecord*>(record.Get());
            CHECK_WITH_LOG(ptr);

            const NUtil::TParsedLogRecord::TValues& values = ptr->GetValues();
            NUtil::TParsedLogRecord::TValues::const_iterator it = values.find("http_code");
            VERIFY_WITH_LOG(it != values.end(), "Failed to find required field 'http_code'");
            if (it->second != "202")
                return true;

            it = values.find("key");
            VERIFY_WITH_LOG(it != values.end(), "Failed to find required field 'key' (doc url)");
            if (!PrevDocUrl || it->second != PrevDocUrl) {
                PrevDocUrl = it->second;
                return true;
            }

            return false;
        }

    private:
        mutable TString PrevDocUrl;
    };

}

NUtil::ILog::TPtr TIndexerProxyIndexLogChecker::CreateLog() const {
    return new TIndexLog;
}

NUtil::ILogReader::TPtr TIndexerProxyIndexLogChecker::CreateLogReader(const TString& fileName) const {
    return new TIndexLogReader(fileName);
}

TString TIndexerProxyIndexLogChecker::GetLoggerType() const {
    return "Proxy.IndexLog";
}

TRtyTestNodeType TIndexerProxyIndexLogChecker::GetBinaryType() const {
    return TNODE_INDEXERPROXY;
}

TLogChecker::TFactory::TRegistrator<TIndexerProxyIndexLogChecker> TIndexerProxyIndexLogChecker::Registrator("indexer-index");
