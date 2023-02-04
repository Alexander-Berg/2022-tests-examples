#include "svn.h"
#include "utils.h"
#include <library/cpp/charset/wide.h>
#include <saas/util/logging/exception_process.h>
#include <util/stream/pipe.h>

TSvnInfo::TSvnInfo()
: Config(nullptr)
{}

TRevisionInfo TSvnInfo::GetRevisionInfo(ui64 number) {
    if (!number)
        return TRevisionInfo();
    TGuard<TMutex> g(Mutex);
    TRevisionStorage::iterator i = Storage.find(number);
    if (i != Storage.end())
        return i->second;
    TRevisionInfo result;
    result.Revision = number;
    if (DownLoadRevision(result))
        Storage[number] = result;
    return result;
}

void TSvnInfo::StoreRevisionInfo(const TRevisionInfo& info) {
    TGuard<TMutex> g(Mutex);
    Storage[info.Revision] = info;
}

void TSvnInfo::SetConfig(const TConfig& config) {
    Config = &config;
}

bool TSvnInfo::DownLoadRevision(TRevisionInfo& result) {
    if (!Config) {
        ERROR_LOG << "Svn config not seted" << Endl;
        return false;
    }
    TRY
        TStringStream command;
        DEBUG_LOG << "download revision " << result.Revision << "..." << Endl;
        command << "svn log --non-interactive --xml --trust-server-cert"
                   " --config-dir=" << Config->CertPath <<
                   " --username=" << Config->UserName <<
                   " --password=" << Config->Password <<
                   " --revision=" << result.Revision <<
                   " " << Config->Url;
        TPipeInput pipe(command.Str());
        TString xml = pipe.ReadAll();
        NXml::TXmlDoc doc(xml.data(), xml.size());
        xmlNode* xmlEntry = GetChild("logentry", doc.GetRoot());
        if (!xmlEntry)
            return false;
        xmlNode* xmlDate = GetChild("date.text", xmlEntry);
        if (!xmlDate)
            return false;
        ParseISO8601DateTimeDeprecated((char*)xmlDate->content, result.Timestamp);
        xmlNode* xmlMsg = GetChild("msg.text", xmlEntry);
        if (!xmlMsg)
            return false;
        result.Comment = WideToChar(UTF8ToWide((char*)xmlMsg->content), CODES_YANDEX);
        xmlNode* xmlAuthor = GetChild("author.text", xmlEntry);
        if (xmlAuthor)
            result.Author = WideToChar(UTF8ToWide((char*)xmlAuthor->content), CODES_YANDEX);
        DEBUG_LOG << "download revision " << result.Revision << "...Ok" << Endl;
        return true;
    CATCH("While download revision " + ToString(result.Revision)+ " info");
    return false;
}
