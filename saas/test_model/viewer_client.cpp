#include "viewer_client.h"
#include <library/cpp/json/json_writer.h>
#include <saas/util/logging/exception_process.h>

TViewerClient::TViewerClient(TModel& model)
: Model(model)
{}

bool TViewerClient::Reply(void* /*ThreadSpecificResource*/) {
    try {
        if (strnicmp(RequestString.data(), "GET ", 4)) {
            Output() << "HTTP/1.1 501 Not Implemented\r\n\r\n";
        } else {
            if (!ProcessHeaders()) {
                throw yexception() << "invalid headers";
            }
            RD.Scan();
            THolder<TViewerClientReport> report(TViewerClientReportFactory::Construct(
                        TCiString{RD.ScriptName().data(), RD.ScriptName().size()}));
            if(!report)
                throw yexception() << "Unknown path: " << RD.ScriptName();
            report->SetParams(Model, RD);
            report->Process(nullptr);
            if (!report->IsSuccess())
                throw yexception() << report->GetErrors();
            TRY
                TStringStream ss;
                Output() << "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n";
                NJson::WriteJson(&ss, &report->GetResult(), true);
                Output() << ss.Str();
            CATCH("While write report")
        }
    } catch (TSystemError& ex) {
        Cout << "System error (" << ex.Status() << ")" << ex.what() << Endl;
        DEBUG_LOG << "System error (" << ex.Status() << ")" << ex.what() << Endl;
        Output() << "HTTP/1.1 500 Internal Server Error\r\n"
            << "Content-Type: text/html\r\n"
            << "Content-Length: " << TString::StrLen(ex.what()) << "\r\n\r\n"
            << ex.what();

    } catch (const yexception& e) {
        DEBUG_LOG << "yexception"<< e.what() << Endl;
        Cout << "yexception" << e.what() << Endl;
        Output() << "HTTP/1.1 500 Internal Server Error\r\n"
            << "Content-Type: text/html\r\n"
            << "Content-Length: " << TString::StrLen(e.what()) << "\r\n\r\n"
            << e.what();
    }
    return true;
}
