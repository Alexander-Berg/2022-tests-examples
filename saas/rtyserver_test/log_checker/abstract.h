#pragma once

#include <util/generic/ptr.h>
#include <util/generic/string.h>

namespace NUtil {

    class ILogRecord {
    public:
        typedef TSimpleSharedPtr<ILogRecord> TPtr;

        virtual ~ILogRecord() {}
        virtual int Compare(TPtr record, TString& info) const = 0;
        virtual bool IsValuableRecord() const = 0;
        virtual bool IsValuableKey(const TString& key) const = 0;
    };

    class ILogReader {
    public:
        typedef TSimpleSharedPtr<ILogReader> TPtr;

        virtual ~ILogReader() {}
        virtual ILogRecord::TPtr Next() = 0;
        virtual TString GetDescription() const = 0;
    };

    class ILog {
    public:
        typedef TSimpleSharedPtr<ILog> TPtr;

        virtual ~ILog() {}
        virtual void Read(ILogReader::TPtr reader) = 0;
        virtual int Compare(TPtr log, TString& info) const = 0;
        virtual bool IsValuableRecord(ILogRecord::TPtr record) const = 0;
    };

}
