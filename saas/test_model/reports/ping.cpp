#include <saas/tools/test_model/viewer_client.h>

class TReportPing : public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportPing> Registrator;
};

TViewerClientReportFactory::TRegistrator<TReportPing> TReportPing::Registrator("/ping");

void TReportPing::Process(void* /*ThreadSpecificResource*/) {
}
