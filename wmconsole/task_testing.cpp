#include <util/generic/size_literals.h>
#include <util/string/subst.h>
#include <util/thread/pool.h>

#include <mapreduce/yt/interface/protos/yamr.pb.h>

#include <robot/library/yt/static/command.h>

#include <wmconsole/version3/library/conf/yt.h>
#include <wmconsole/version3/processors/indexing/sitetree/protos/searchbase.pb.h>
#include <wmconsole/version3/wmcutil/log.h>
#include <wmconsole/version3/wmcutil/yt/misc.h>

#include "config.h"

#include "task_cleanup.h"

namespace NWebmaster {

using namespace NJupiter;

struct TFilterMapper : public NYT::IMapper<NYT::TTableReader<NProto::TSearchBaseDiffRecord>, NYT::TTableWriter<NProto::TSearchBaseDiffRecord>> {
    Y_SAVELOAD_JOB(WebmasterHosts)

    TFilterMapper() = default;
    TFilterMapper(const THashSet<TString> &webmasterHosts)
        : WebmasterHosts(webmasterHosts)
    {
    }

    void Do(TReader *input, TWriter *output) override {
        for (; input->IsValid(); input->Next()) {
            const auto &row = input->GetRow();
            if (WebmasterHosts.contains(row.GetHost())) {
                output->AddRow(row);
            }
        }
    }

public:
    THashSet<TString> WebmasterHosts;
};

REGISTER_MAPPER(TFilterMapper)

struct TFilterKeyMapper : public NYT::IMapper<NYT::TTableReader<NYT::TYamr>, NYT::TTableWriter<NYT::TYamr>> {
    Y_SAVELOAD_JOB(WebmasterHosts)

    TFilterKeyMapper() = default;
    TFilterKeyMapper(const THashSet<TString> &webmasterHosts)
        : WebmasterHosts(webmasterHosts)
    {
    }

    void Do(TReader *input, TWriter *output) override {
        for (; input->IsValid(); input->Next()) {
            const auto &row = input->GetRow();
            if (WebmasterHosts.contains(row.GetKey())) {
                output->AddRow(row);
            }
        }
    }

public:
    THashSet<TString> WebmasterHosts;
};

REGISTER_MAPPER(TFilterKeyMapper)

TString GetTestingPath(const TString &input) {
    const static TString TEST_ENV_ROOT = TTestYTEnvironment().TABLE_ENV_ROOT;
    TString output = input;
    SubstGlobal(output, TCustomYTEnvironment::CInstance().TABLE_ENV_ROOT, TEST_ENV_ROOT);
    if (input == output) {
        ythrow yexception() << "input = output: " << input;
    }
    return output;
}

template<class Proto, class FilterMapper>
void FilterTable(NYT::ITransactionPtr tx, const THashSet<TString> &webmasterHosts, const TString &inputTable) {
    try {
        const TString outputTable = GetTestingPath(inputTable);
        TTable<Proto> input(tx, inputTable);
        TTable<Proto> output(tx, outputTable);
        if (output.Exists()) {
            output.Drop();
        }
        LOG_INFO("testing, processing %s -> %s", inputTable.c_str(), outputTable.c_str());

        TMapCmd<FilterMapper>(tx, new FilterMapper(webmasterHosts))
            .Input(input)
            .Output(output)
            .MaxRowWeight(128_MBs)
            .Ordered()
            .Do()
        ;

        TSortCmd<Proto>(tx, output)
            .By(input.GetSortedBy())
            .Do();

        NYT::TNode userAttrs = NYTUtils::GetAttr(tx, inputTable, "user_attribute_keys");
        for (const NYT::TNode &attrName : userAttrs.AsList()) {
            const NYT::TNode attr = GetYtAttr(tx, inputTable, attrName.AsString());
            SetYtAttr(tx, outputTable, attrName.AsString(), attr);
        }
    } catch (yexception &e) {
        LOG_ERROR("testing, unable to process table %s: %s", inputTable.c_str(), e.what());
    }
}

int TaskTesting(int, const char **) {
    const auto &cfg = TConfig::CInstance();

    NYT::IClientPtr client = NYT::CreateClient(cfg.MR_SERVER_HOST);
    NYT::ITransactionPtr tx = client->StartTransaction();

    THashSet<TString> webmasterHosts;
    if (!NYTUtils::LoadWebmastersHosts(tx, GetTestingPath(cfg.TABLE_SOURCE_WEBMASTER_HOSTS), webmasterHosts)) {
        ythrow yexception() << "could not load webmaster hosts table";
    }

    const TVector<TString> inputs = {
        cfg.TABLE_SEARCH_HISTORY_WMC,
        cfg.TABLE_SEARCH_SAMPLES_EXCLUDED,
        cfg.TABLE_SEARCH_SAMPLES_INSEARCH,
    };

    THolder<IThreadPool> queue(CreateThreadPool(4));
    for (const TString &inputTable : inputs) {
        queue->SafeAddFunc([=, &tx, &webmasterHosts]() {
            FilterTable<NProto::TSearchBaseDiffRecord, TFilterMapper>(tx, webmasterHosts, inputTable);
        });
    }

    // processing ready-* tables
    TDeque <NYTUtils::TTableInfo> prodSitetreeTables;
    TDeque <NYTUtils::TTableInfo> testSitetreeTables;
    NYTUtils::GetTableList(tx, cfg.TABLE_SITETREE_ROOT, prodSitetreeTables);
    NYTUtils::GetTableList(tx, GetTestingPath(cfg.TABLE_SITETREE_ROOT), testSitetreeTables);
    THashSet<TString> readyTablesToProcess;
    for (const NYTUtils::TTableInfo &tableInfo : prodSitetreeTables) {
        if (tableInfo.Name.StartsWith(cfg.TABLE_SITETREE_READY_PREFIX)) {
            readyTablesToProcess.insert(NYTUtils::GetTableName(tableInfo.Name));
        }
    }
    for (const NYTUtils::TTableInfo &tableInfo : testSitetreeTables) {
        readyTablesToProcess.erase(NYTUtils::GetTableName(tableInfo.Name));
    }
    for (const TString &tableName : readyTablesToProcess) {
        queue->SafeAddFunc([=, &tx, &webmasterHosts]() {
            FilterTable<NYT::TYamr, TFilterKeyMapper>(tx, webmasterHosts, NYTUtils::JoinPath(cfg.TABLE_SITETREE_ROOT, tableName));
        });
    }

    queue->Stop();
    tx->Commit();

    return 0;
}

} //namespace NWebmaster
