#include "export.h"

#include <library/cpp/bucket_quoter/bucket_quoter.h>
#include <library/cpp/logger/global/global.h>
#include <yweb/robot/kiwi/egg/egg.h>
#include <yweb/robot/kiwi/export/iexport.h>
#include <yweb/robot/kiwi/clientlib/subscriber.h>
#include <yweb/robot/kiwi/domain/objdomain.h>
#include <yweb/robot/kiwi/domain/shards.h>
#include <yweb/robot/kiwi/domain/tuples.h>
#include <yweb/robot/kiwi/export/config.h>
#include <yweb/robot/kiwi/export/exporter.h>
#include <yweb/robot/kiwi/export/proxy.h>
#include <yweb/robot/kiwi/export/subscribersrv.h>
#include <yweb/robot/kiwi/stubs/metalib/metaclientstub.h>
#include <robot/library/oxygen/indexer/input/protobin/protobin.h>
#include <robot/library/oxygen/indexer/input/prototext/prototext.h>
#include <robot/library/oxygen/indexer/input/calctext/calctext.h>
#include <robot/library/oxygen/indexer/tuple_value/tuple_value.h>

#include <util/generic/buffer.h>
#include <util/random/random.h>
#include <util/system/hostname.h>

using namespace NKiwi;

NExport::TConfig::EProgram gProgram = NExport::TConfig::Nest; // Program to be emulated

NOxygen::TObjectContext TFromTuplesGenerator::GetExportData() {
    CHECK_WITH_LOG(InputStream);
    return InputStream->ReadObject();
}

TFromTuplesGenerator::TFromTuplesGenerator(const TString& inputFile)
    : FileInput(inputFile)
{
    if (inputFile.EndsWith("protobin")) {
        InputStream = MakeHolder<NOxygen::TProtobinInputStream>(FileInput);
    } else if (inputFile.EndsWith("prototext")) {
        InputStream = MakeHolder<NOxygen::TPrototextInputStream>(FileInput);
    } else {
        InputStream = MakeHolder<NOxygen::TCalctextInputStream>(FileInput);
    }
}

struct TSubscriberTestConfig {
    EDomain Domain;

    // Export data settings/metadata
    TString Name;
    ui16 KeyType;
    ui64 Version;
    TString DestHost;
    ui16 DestPort;
    NExport::TConfig Export;
    ui16 AttrId;
    ui16 BranchId;

    // Subscriber settings/metadata
    TString MRServer;
    TString MRFolder;
    TString MRUser;
    ui32 MRTableSizeMB;
    ui32 MRTableRecords;
    ui32 MRTableRotationInterval;
    ui32 MRTableIdleTimeout;
    bool YtMode;

    // Hen settings
    TString HenHost;
    ui16 HenPort;
    ui16 NestPort;
    TString MetaFilesPrefix;

    // Quota
    ui64 Speed;

    // For ParseLookupConf
    unsigned DiskSegmentsNum;

    // SAAS-specific
    TString DataFile;

    TSubscriberTestConfig() {
        Domain = ED_TUPLE;
        Name = "SubscriberTestExport";
        KeyType = NKwTupleMeta::KT_DOC_DEF;
        Version = 0;
        DestHost = "localhost";
        DestPort = NDefs::SubscriberMsgBusPort;
        Export.Clear(gProgram);
        AttrId = 1;
        BranchId = 0;

        MRServer = "localhost";
        MRFolder = "omitted/folder/";
        MRTableSizeMB = 0;
        MRTableRecords = 0;
        MRTableRotationInterval = 1;
        MRTableIdleTimeout = 0;
        YtMode = false;

        HenHost = "localhost";
        HenPort = NDefs::HenMsgBusPort;
        NestPort = NDefs::NestMsgBusPort;

        Export.SubscriberCli.BufferizerMsgSize = 10;
        Export.SubscriberCli.BufferizerTotalSize = 20;
    }
};

template <class TDomain>
int Run(const TSubscriberTestConfig& config)
{
    TString proxies =
        "MsgBusProxyAddresses {\n"
        "    Host : \"" + config.DestHost + "\"\n"
        "    Port : " + ToString(config.DestPort) + "\n"
        "}\n"
        "RsyncProxyAddresses {\n"
        "    Host : \"" + config.DestHost + "\"\n"
        "    Port : " + ToString(config.DestPort) + "\n"
        "}\n"
        "MRProxyAddresses {\n"
        "    Host : \"" + config.DestHost + "\"\n"
        "    Port : " + ToString(config.DestPort) + "\n"
        "}\n";
    ;

    TString egg =
        "HenBalancerAddress {\n"
        "    Host : \"" + config.HenHost + "\"\n"
        "    Port : " + ToString(config.HenPort) + "\n"
        "}\n"
        ;

    TString points =
        "Nodes {"
        "  DC: 0"
        "  Name: \"" + TString(GetFQDNHostName()) + "\""
        "  Fast: \"" + TString(GetFQDNHostName()) + "\""
        "  Port: " + ToString(config.NestPort) + ""
        "}"
        "NReplicas: 1"
        ;

    TString triggers =
        "MetaAttrs { AttrId : " + ToString(config.AttrId) + "; Scheme { Name : \"AttrName\"; Type : AT_STRING } }\n"
        "Branches {\n"
        "    Name : \"TRUNK\"\n"
        "    Bid : 0\n"
        "    PBid : 0\n"
        "    CrtTime : 0\n"
        "    DelTime : 0xffffffff\n"
        "    Objects {\n"
        "        KeyAttrId : " + ToString(config.AttrId) + "\n"
        "        KeyType : KT_DOC_DEF\n"
        "        Attributes { Name: \"AttrName\"; CrtTime : 0; DelTime : 0xffffffff; States { Time : 0; KeepOldest : true; Phantom : true } }\n"
        "    }\n"
        "}\n"
        ;

    TString exports =
        "Exports {\n"
            "  Name: \"" + config.Name + "\"\n"
            "  SubscriberName: \"" + ToString(config.Name) + "\"\n"
            "  SubscriberType: ST_MAPREDUCE\n"
            "  Version: " + ToString(config.Version) + "\n"
            "  ExportTypes: 0\n"
            "  KeyType: " + NKwTupleMeta::EKeyType_Name((NKwTupleMeta::EKeyType)config.KeyType) + "\n"
            "  Program: \"\"\n"
            "  Condition: \"\"\n"
            "}\n"
            "MapReduceSubscribers {\n"
            "  Name: \"" + config.Name + "\"\n"
            "  DataFormat {\n"
            "    Format: DF_TEXT\n"
            "    TextOpts {\n"
            "      FieldSep: \"\\n\"\n"
            "      AttrFormat: SM_NONE\n"
            "      BranchFormat: SM_NONE\n"
            "      TsFormat: SM_NONE\n"
            "      DoNotEscapeStrings: true\n"
            "      ShowLabel: false\n"
            "      ErrorShowMode: ESM_INFO\n"
            "      DoNotShowKey: true\n"
            "    }\n"
            "  }\n"
            "  MRServer: \"" + config.MRServer + "\"\n"
            "  MRFolder: \"" + config.MRFolder + "\"\n"
            "  MRUser: \"" + config.MRUser + "\"\n"
            "  MRTableSizeMB: " + ToString(config.MRTableSizeMB) + "\n"
            "  MRTableRecords: " + ToString(config.MRTableRecords) + "\n"
            "  MRTableRotationInterval: " + ToString(config.MRTableRotationInterval) + "\n"
            + (config.MRTableIdleTimeout != 0 ? "  MRTableIdleTimeout: " + ToString(config.MRTableIdleTimeout) + "\n" : TString()) +
            "  YtMode: " + (config.YtMode? "true": "false") + "\n"
            "}\n"
        ;

    INFO_LOG << "Use the following metadata" << Endl;
    INFO_LOG << proxies << egg << triggers << exports;

    INFO_LOG << "Starting meta client stub" << Endl;
    THolder<IMetaClient> Meta(new TMetaClientStub(config.Domain, triggers, TString(), TString(), proxies, TString(), egg, TString(), exports));
    Meta->RegisterParser(META_POINTS, IMetaClient::TParseFunc(std::bind(&NKiwi::NKwLookup::ParseLookupConf<TDomain, TSubscriberTestConfig>, &config, std::placeholders::_1, std::placeholders::_2)));
    Meta->RegisterParser(META_TRIGGERS, IMetaClient::TParseFunc(&TDomain::ParseDomainMeta));
    Meta->RegisterParser(META_EXPORTS, IMetaClient::TParseFunc(&NExport::TExportScheme::Parse));
    Meta->RegisterParser(META_PROXIES, IMetaClient::TParseFunc(&NExport::TProxyConfig::Parse));
    Meta->RegisterParser(META_EGG, IMetaClient::TParseFunc(&TEgg::Parse));
    NKiwi::NKwLookup::SetStub(NKiwi::NKwLookup::TImplConf::Make<TTuplesDomain>(1), "localhost:1027:1");
    Meta->Start("not-used");

    INFO_LOG << "Starting Exporter" << Endl;
    NExport::TExporter Exporter(config.Export, Meta.Get(), gProgram);
    NExport::TExportMonCounters* ExportCounters;
    NExport::TExportMonCounters* TotalCounters;
    Exporter.GetCounters()->Add(config.Name, NExport::EC_EXPORT, &ExportCounters, &TotalCounters);

    INFO_LOG << "Subscriber test started" << Endl;
    TFromTuplesGenerator generator(config.DataFile);

    TBucketQuoter<int> quoter(config.Speed, 2 * config.Speed, nullptr, nullptr, nullptr);

    NOxygen::TObjectContext objectContext = NOxygen::TObjectContext::Null;
    while (!(objectContext = generator.GetExportData()).IsNull()) {
        NKwExport::TExportRecord rec;
        ui8 keyType = objectContext.GetOrElse<ui8>(NOxygen::KEYWORD_KEYTYPE, NKwTupleMeta::KT_DOC_DEF);
        TStringBuf key = objectContext.GetOrElse<TStringBuf>(NOxygen::KEYWORD_KEY, "");
        rec.SetKeyType(keyType);
        rec.SetKey(TString(key));
        {
            TString kiwiObject;
            for (const auto& attr : objectContext) {
                NOxygen::TTupleAttrValuePtr tupleAttr = NOxygen::ConvertToTupleValue(attr);
                tupleAttr->AppendAsTuple(kiwiObject);
            }
            rec.SetData(kiwiObject);
        }

        INFO_LOG << "Exporting URL " << key << " keytype " << int(keyType) << Endl;
        quoter.Sleep();
        quoter.Use(1);
        Exporter.Export(rec, config.Name, config.DestHost, config.DestPort, config.Version);
    }

    Y_UNUSED(NObjDomain::TableScheme[0]);
    INFO_LOG << "Exiting from kiwi export" << Endl;
    return 0;
}

void ExportFromDump(const TString& dump, ui16 port, const TString& host, ui16 rps) {
    TSubscriberTestConfig config;
    config.DataFile = dump;
    config.DestHost = host;
    config.Speed = rps;
    if (port) {
        config.DestPort = port;
    }
    Run<TTuplesDomain>(config);
}
