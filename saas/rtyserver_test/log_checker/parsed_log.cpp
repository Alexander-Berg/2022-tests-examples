#include "parsed_log.h"

#include <library/cpp/logger/global/global.h>

namespace NUtil {

    void TParsedLog::Read(ILogReader::TPtr reader) {
        Description = reader->GetDescription();
        while (ILogRecord::TPtr record = reader->Next())
            Records.push_back(record);
    }

    int TParsedLog::Compare(TPtr log, TString& info) const {
        const TParsedLog* ptr = dynamic_cast<const TParsedLog*>(log.Get());
        CHECK_WITH_LOG(ptr);

        DEBUG_LOG << "Start checking log " << Description << " versus log " << ptr->Description << Endl;

        int lineNo = 0;
        int result = 0;
        size_t compared = 0;

        CompareStart();
        ptr->CompareStart();

        TRecords::const_iterator it = Records.begin();
        TRecords::const_iterator jt = ptr->Records.begin();
        while (result == 0) {
            while (it != Records.end() && !IsValuableRecord(*it))
                it++;

            while (jt != ptr->Records.end() && !log->IsValuableRecord(*jt))
                jt++;

            if (it == Records.end() || jt == ptr->Records.end())
                break;

            result = (*it)->Compare(*jt, info);
            compared++;

            it++;
            jt++;
            lineNo++;
        }

        if (result == 0) {
            if (it != Records.end()) {
                CHECK_WITH_LOG(jt == ptr->Records.end());
                info = "Log " + Description + " has more valuable records than log " + ptr->Description;
                result = -1;
            }
            if (jt != ptr->Records.end()) {
                CHECK_WITH_LOG(it == Records.end());
                info = "Log " + Description + " has less valuable records than log " + ptr->Description;
                result = 1;
            }
        }

        CompareFinish();
        ptr->CompareFinish();

        DEBUG_LOG << "Log checking is complete, successfully compared " << compared << " lines: ";

        if (result == 0)
            DEBUG_LOG << "logs match" << Endl;
        else
            DEBUG_LOG << "logs do not match at line " << lineNo - 1 << Endl;

        return result;
    }

    bool TParsedLog::IsValuableRecord(ILogRecord::TPtr record) const {
        CHECK_WITH_LOG(record);
        return record->IsValuableRecord();
    }

    const TString& TParsedLog::GetDescription() const {
        return Description;
    }

}
