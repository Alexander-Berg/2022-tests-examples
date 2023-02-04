#include "parsed_log_record.h"

#include <library/cpp/logger/global/global.h>

namespace NUtil {

    TParsedLogRecord::TParsedLogRecord(const TString& description)
        : Description(description)
    {
    }

    const TParsedLogRecord::TValues& TParsedLogRecord::GetValues() const {
        return Values;
    }

    int TParsedLogRecord::Compare(TPtr record, TString& info) const {
        const TParsedLogRecord* ptr = dynamic_cast<const TParsedLogRecord*>(record.Get());
        CHECK_WITH_LOG(ptr);
        if (!IsLessOrEqual(*ptr, info))
            return -1;
        if (!ptr->IsLessOrEqual(*this, info))
            return 1;
        return 0;
    }

    bool TParsedLogRecord::IsValuableRecord() const {
        return true;
    }

    bool TParsedLogRecord::IsValuableKey(const TString&) const {
        return true;
    }

    bool TParsedLogRecord::IsLessOrEqual(const TParsedLogRecord& record, TString& info) const {
        for (TValues::const_iterator it = Values.begin(); it != Values.end(); it++) {
            const TString& key = it->first;
            if (IsValuableKey(key)) {
                TValues::const_iterator jt = record.Values.find(key);
                if (jt == record.Values.end()) {
                    info = Sprintf("Field '%s' not found in %s", key.data(), record.Description.data());
                    return false;
                }
                if (it->second != jt->second) {
                    info = Sprintf("Value '%s' in field '%s' in %s does not match value '%s' in %s",
                                   it->second.data(), key.data(), Description.data(), jt->second.data(), record.Description.data());
                    return false;
                }
            }
        }
        return true;
    }

}
