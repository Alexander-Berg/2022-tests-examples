#include <saas/rtyserver_test/testerlib/search_checker.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/library/daemon_base/actions_engine/controller_script.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/rtyserver/controller/controller_actions/clear_index_action.h>
#include <saas/rtyserver/common/common_messages.h>
#include <saas/rtyserver_test/util/tass_parsers.h>
#include <saas/library/daemon_base/unistat_signals/signals.cpp>

#include <saas/api/factors_erf.h>
#include <saas/util/bomb.h>

#include <util/generic/ymath.h>

START_TEST_DEFINE(TestNegativeSearchableAge)
    bool SearchableAgeIsNegative() {
        i64 searchableAge = 0;
        TString tassResult;
        i32 cnt = 10;
        while (cnt--) {
            Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);
            if (TRTYTassParser::GetTassValue(tassResult, "index-searchable-age-avg_avvv", &searchableAge)) {
                break;
            }
        }
        return searchableAge < 0;
    }

    bool Run() override {

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, true);
        messages[0].MutableDocument()->SetModificationTimestamp(Seconds() + 2 * 10000);

        IndexMessages(messages, DISK, 1, 1000);

        ReopenIndexers();

        sleep(5);
        ReportUnistatSignals(Cerr);
        sleep(5);

        return SearchableAgeIsNegative();
    }
    bool InitConfig() override {
        SetIndexerParams(DISK, 1, 1);
        (*ConfigDiff)["Server.IsPrefixedIndex"] = true;
        return true;
    }
};
