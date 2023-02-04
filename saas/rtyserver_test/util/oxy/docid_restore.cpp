#include "docid_restore.h"

#include <saas/rtyserver/components/fullarchive/disk_manager.h>
#include <saas/library/daemon_base/config/daemon_config.h>

#include <saas/rtyserver/config/config.h>

namespace NSaas {
    THolder<TId2Url> LoadDocUrls(TString path) {
        THolder<TId2Url> result = MakeHolder<TId2Url>();

        /* const char s_FakeConfig[] =
            "<DaemonConfig>\nLogLevel: 0\n</DaemonConfig>\n<Server>\n<ComponentsConfig>\n"
            "    <FULLARC>\nActiveLayers: full\n</FULLARC>\n"
            "</ComponentsConfig>\n</Server>\n"; */
        TRTYServerConfig config(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false));
        THolder<TDiskFAManager> manager;
        TSet<TString> faLayers{NRTYServer::NFullArchive::BaseLayer, NRTYServer::NFullArchive::FullLayer};
        manager.Reset(new TDiskFAManager(TFsPath(path), 10000000u, config, 0, faLayers, false, true, true));
        bool ok = manager->Open();
        CHECK_WITH_LOG(ok && manager->IsOpened());
        for (TDiskFAManager::TIterator::TPtr iter = manager->CreateIterator(); iter->IsValid(); iter->Next()) {
            const ui32 realDocId = iter->GetDocId();
            if (manager->IsRemoved(realDocId)) {
                continue;
            }

            const NRTYServer::TParsedDoc& pd = iter->GetParsedDoc();
            if (!pd.HasDocument())
                continue;

            result->insert(std::make_pair(realDocId, pd.GetDocument().GetUrl()));
        }
        manager->Close();
        return result;
    }
};
