#include "backend.h"
#include "config_fields.h"

#include <saas/rtyserver/components/ddk/ddk_component.h>
#include <saas/rtyserver/components/indexer/index_component.h>
#include <saas/rtyserver/components/suggest/component.h>
#include <saas/rtyserver/components/zones_makeup/makeup_component.h>
#include <saas/rtyserver/components/fastarchive/globals.h>
#include <saas/rtyserver/components/fullarchive/globals.h>

#include <saas/rtyserver/config/common_indexers_config.h>
#include <saas/rtyserver/config/config.h>
#include <saas/rtyserver/config/indexer_config.h>
#include <saas/rtyserver/config/searcher_config.h>

#include <saas/rtyserver/components/suggest/config/config.h>
#include <saas/library/sharding/sharding.h>
#include <saas/util/logging/exception_process.h>
#include <saas/rtyserver/components/special_keys/const.h>

const TString SuggestConfigText =
"Zones : { ZoneName : \"aaTitle1\" Weight : 22 }\n"
"Zones : { ZoneName : \"pscaption1\" Weight : 2 }\n"
"DefaultWeight : 10"
;

TString GetDaemonConfigText(const TString& runPath) {
    TStringStream ss;
    ss << "<DaemonConfig>" << Endl;
    ss << "LoggerType :" << Endl;
    ss << "IndexLog :" << Endl;
    ss << "AccessLog :" << Endl;
    ss << "StdOut:" << Endl;
    ss << "StdErr:" << Endl;
    ss << "LogLevel: 8" << Endl;
    ss << "LogRotation: true" << Endl;
    ss << "<Controller>" << Endl;
    ss << "Port : 11000" << Endl;
    ss << "StartServer : false" << Endl;
    ss << "Threads : 2" << Endl;
    ss << "ConfigsRoot : " << runPath << "/configs/" << Endl;
    ss << "StateRoot : " << runPath << "/state/" << Endl;
    ss << "</Controller>" << Endl;
    ss << "</DaemonConfig>";
    return ss.Str();
}

const TString PrototypeConfigDefault(const TString& rootDir);
extern const TString XmlParserConfig;
extern const TString HtmlParserConfig;
extern const TString FactorsConfig;

void PutPreffixedPatch(TConfigPatches* patches, bool prefixed, ui32 shardsNumber) {
    TConfigFieldsPtr onePatch (new TConfigFields());
    if (prefixed) {
        (*onePatch)["Server.IsPrefixedIndex"] = true;
        (*onePatch)["Server.ShardsNumber"] = shardsNumber;
    } else {
        (*onePatch)["Server.IsPrefixedIndex"] = false;
        (*onePatch)["Server.ShardsNumber"] = 1;
    }

    patches->push_back(onePatch);
}

bool InitParserConfig(const TString& rootDir, const TString& config, const TString& filename) {
    TRY {
        if (!NFs::Exists(rootDir)) {
            TFsPath(rootDir.data()).MkDirs();
        }
        TUnbufferedFileOutput file(filename);
        file.Write(config.data());
        return true;
    } CATCH ("Cannot write " + filename) {
        return false;
    }
}

void BuildFiles(const TString& rootDir) {
    const TString xmlParserConfigFileName = rootDir + GetDirectorySeparator() + "xml_parser.cfg";
    const TString htmlParserConfigFileName = rootDir + GetDirectorySeparator() + "html_parser.cfg";
    const TString suggestZonesFileName = rootDir + GetDirectorySeparator() + "suggest_zones.cfg";
    const TString factorsFileName = rootDir + GetDirectorySeparator() + "factors.cfg";
    const TString factorsFileNameNew = factorsFileName + "New";
    const TString factorsFileNameNewSmall = factorsFileName + "NewSmall";


    VERIFY_WITH_LOG(InitParserConfig(rootDir, HtmlParserConfig, htmlParserConfigFileName), "Can't create file config parser html");
    VERIFY_WITH_LOG(InitParserConfig(rootDir, XmlParserConfig, xmlParserConfigFileName), "Can't create file config parser xml");
    VERIFY_WITH_LOG(InitParserConfig(rootDir, FactorsConfig, factorsFileName), "Can't create file factors configuration");
    VERIFY_WITH_LOG(InitParserConfig(rootDir, SuggestConfigText, suggestZonesFileName), "Can't create file suggest configuration");
}

void InitConfig(TRTYServerConfig& config, const TString& rootDir) {
    const TString xmlParserConfigFileName = rootDir + GetDirectorySeparator() + "xml_parser.cfg";
    const TString htmlParserConfigFileName = rootDir + GetDirectorySeparator() + "html_parser.cfg";

    config.GetCommonIndexers().XmlParserConfigFile = xmlParserConfigFileName;
    config.GetCommonIndexers().HtmlParserConfigFile = htmlParserConfigFileName;
}

void ConfigureAndStartBackend(const TString& rootDir, const TDaemonConfig& daemonConfig, const TString& protocolType, int maxDocs) {
    TString configString = daemonConfig.ToString("DaemonConfig") + PrototypeConfigDefault(rootDir);
    TServerConfigConstructorParams confConstParams(configString.data());
    BuildFiles(rootDir + "/def_cfg");
    TRTYServerConfig config(confConstParams);

    config.GetSearcherConfig().ServerOptions.Host = TString(); //TestsHostName(); This makes port_checker watchdog happy in TE tests
    if (protocolType)
        config.GetCommonIndexers().ProtocolType = protocolType;
    if (maxDocs > 0)
        config.GetIndexerDiskConfig().MaxDocuments = maxDocs;

    InitConfig(config, rootDir + "/def_cfg");
    TUnbufferedFileOutput fo(rootDir + "/def_cfg/rtyserver.conf");
    fo << config.ToString();
}

const TString PrototypeConfigDefault(const TString& rootDir) {
    return (GetDaemonConfigText(rootDir) +
    "<Server>\n"
    "Components:" INDEX_COMPONENT_NAME "," MAKEUP_COMPONENT_NAME "," DDK_COMPONENT_NAME "," FASTARCHIVE_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME "," SUGGEST_COMPONENT_NAME "," + NRTYServer::KeysComponentName + "\n"
        "AdditionalModules:DOCFETCHER, Synchronizer\n"
        "<ModulesConfig>\n"
        "<DOCFETCHER>\n"
            "<Stream>\n"
                "DistributorServers:localhost:20108\n"
                "ProxyType : RTYSERVER\n"
                "ShardMin:0\n"
                "ShardMax:0\n"
            "</Stream>\n"
            "LogFile: " + rootDir + "/df.log\n"
            "StateFile: " + rootDir + "/df.state\n"
        "</DOCFETCHER>\n"
        "<Synchronizer>\n"
        "DetachPath:\n"
        "</Synchronizer>\n"
        "</ModulesConfig>\n"
        "<ComponentsConfig>\n"
            "<" INDEX_COMPONENT_NAME ">\n"
                "Index1 : value_i1\n"
                "Index2 : value_i1\n"
            "</" INDEX_COMPONENT_NAME ">\n"
            "<" DDK_COMPONENT_NAME ">\n"
            "ddk1 : value_ddk1\n"
            "ddk2 : value_ddk1\n"
            "</" DDK_COMPONENT_NAME ">\n"
            "<" SUGGEST_COMPONENT_NAME ">\n"
                "ZonesWeightsFileInfo : " + rootDir + "/def_cfg/suggest_zones.cfg\n"
                "ShardMin:0\n"
                "ShardMax:" + ToString(NSearchMapParser::SearchMapShards) + "\n"
                "ClearUnusefulData : false\n"
            "</" SUGGEST_COMPONENT_NAME ">\n"
            "<Ann>\n"
                "DefaultLanguage : rus\n"
            "</Ann>\n"
            "<FULLARC>\n"
                "ActiveLayers : base,full,geo\n"
                "<DefaultLayerConfig>\n"
                "ReadContextDataAccessType: MEMORY_MAP\n"
                "</DefaultLayerConfig>\n"
            "</FULLARC>\n"
        "</ComponentsConfig>\n"
        "<Monitoring>\n"
        "Enabled : true\n"
        "</Monitoring>\n"
        "<Merger>\n"
            "MaxSegments : 1\n"
            "MaxDocumentsToMerge : 10000000\n"
            "Enabled : false\n"
            "MergerCheckPolicy : TIME\n"
            "TimingCheckIntervalMilliseconds : 10000\n"
            "Threads : 8\n"
            "UserExternalMerger :\n"
            "IndexSwitchSystemLockFile :\n"
        "</Merger>\n"
        "<Repair>\n"
            "Threads : 10\n"
            "Enabled : false\n"
        "</Repair>\n"
        "<Logger>\n"
        "</Logger>\n"
        "IndexDir : \n"
        "IsPrefixedIndex : false\n"
        "VerificationPolicy : Testing\n"
        "DoStoreArchive : true\n"
        "StartAsDaemon : false\n"
        "ShardsNumber : 20\n"
        "EnableDeleter : true\n"
        "NoMorphology : false\n"
        "<Searcher>\n"
            "SnippetsDeniedZones : no_snip,   no_snip1, denied_*\n"
            "WildcardSearch : infix\n"
            "ReArrangeOptions : \n"
            "ExternalSearch : rty_relevance\n"
            "EventLog :\n"
            "ArchivePolicy : INMEM\n"
            "ArchiveType : AT_FLAT\n"
            "LockIndexFiles : true\n"
            "PrefetchSizeBytes : 100000\n"
            "<HttpOptions>\n"
                "Port : 15000\n"
                "Threads : 8\n"
            "</HttpOptions>\n"
            "<QueryLanguage>\n"
                "hdr_subject: ZONE\n"
                "hdr_to: ZONE\n"
                "hdr_from: ZONE\n"
                "hdr_cc: ZONE\n"
                "hdr_bcc: ZONE\n"
                "text: ZONE\n"
                "hdr_replyto: ZONE\n"
                "attachname: ZONE\n"
                "body_text: ZONE\n"
                "headers: ZONE\n"
                "hhh: ZONE\n"
                "aaa: ZONE\n"
                "sss: ZONE\n"
                "bbb: ZONE\n"
                "fff: ZONE\n"
                "grrr: ZONE\n"
                "zone: ZONE,,template\n"

                "domain: ATTR_LITERAL, doc\n"
                "rhost: ATTR_LITERAL, doc\n"
                "grr_attr: ATTR_LITERAL, zone\n"
                "int_zone_attr: ATTR_INTEGER, zone\n"
                "x_urls: ATTR_LITERAL, doc\n"
                "attachsize: ATTR_INTEGER, doc\n"
                "attachsize_b: ATTR_INTEGER, doc\n"
                "mimetype: ATTR_LITERAL, doc\n"
                "suid: ATTR_LITERAL, doc\n"
                "mid: ATTR_LITERAL, doc\n"
                "received_date: ATTR_DATE, zone\n"
                "zone_attr: ATTR_LITERAL, zone\n"
                "lang: ATTR_INTEGER, doc\n"
                "hid: ATTR_LITERAL, doc\n"
                "test: ATTR_LITERAL, doc\n"
                "tag: ATTR_LITERAL, doc\n"
                "link: ATTR_LITERAL, doc\n"
                "attr_bb_prop: ATTR_LITERAL, doc\n"
                "xxx_urls: ATTR_URL, doc\n"
                "apocalipse_date: ATTR_DATE, doc\n"
                "bool_flag: ATTR_BOOLEAN, doc\n"
                "search_attr_lit: ATTR_LITERAL, doc, template\n"
                "search_attr_int: ATTR_INTEGER, doc, template\n"
                "porno: ATTR_INTEGER, doc\n"
                "ft: ATTR_INTEGER, doc\n"
                "z_: ZONE, doc, template\n"
                "s_ : ATTR_LITERAL, doc, template\n"
                "i_ : ATTR_INTEGER, doc, template\n"
                "sz_ : ATTR_LITERAL, zone, template\n"
                "iz_ : ATTR_INTEGER, zone, template\n"
                "ngr_ : ZONE, doc, template, ngr-3\n"
         "</QueryLanguage>\n"
        "</Searcher> \n"
        "<BaseSearchersServer>\n"
            "Port : 13000\n"
            "Threads: 16\n"
            "MaxConnections: 0\n"
        "</BaseSearchersServer>\n"
        "<Indexer>\n"
            "<Common>\n"
                "<HttpOptions>\n"
                    "Port : 17000\n"
                    "Threads : 40\n"
                    "ClientTimeout : 1000\n"
                "</HttpOptions>\n"
                "UseSlowUpdate: true\n"
                "UseHTML5Parser: true\n"
                "Groups : mid:2 single:1 attr_aa_grp:2 unique_attr:2:unique attr_cc_grp:2:named unique_attr_1:2:unique\n"
                "DocProperty : attr_bb_prop\n"
                "<ZonesToProperties>\n"
                    "z_text : p_text\n"
                "</ZonesToProperties>\n"
                "<TextArchiveParams>\n"
                    "MinPartSizeFactor : 0.8\n"
                    "MaxPartCount : 64\n"
                "</TextArchiveParams>\n"
                "RecognizeLibraryFile : NOTSET\n"
                "DefaultCharset : utf8\n"
                "DefaultLanguage : ru\n"
                "DefaultLanguage2 : ru\n"
            "</Common>\n"
            "<Memory>\n"
                "Threads : 2\n"
                "DocumentsQueueSize : 50000\n"
                "ConnectionTimeout : 100\n"
                "ProtocolType : default.local\n"
                "UserExternalIndexer :\n"
            "</Memory>\n"
            "<Disk>\n"
                "Threads : 4\n"
                "MaxDocuments : 2000\n"
                "DocumentsQueueSize : 50000\n"
                "ConnectionTimeout : 100\n"
                "PortionDocCount : 300\n"
                "ProtocolType : default.local\n"
                "UserExternalIndexer :\n"
            "</Disk>\n"
        "</Indexer>\n"
    "</Server>\n");
}

const TString XmlParserConfig (
        "<XMLParser>\n"
            "<DOCTYPE>\n"
                "<Zones>\n"
                    "hdr_subject: HDR_SUBJECT\n"
                    "hdr_to: HDR_TO\n"
                    "hdr_from: HDR_FROM\n"
                    "hdr_cc: HDR_CC\n"
                    "hdr_bcc: HDR_BCC\n"
                    "hdr_replyto: REPLY_TO\n"
                    "attachname: ATTACHNAME\n"
                    "body_text: BODY_TEXT\n"
                    "headers: HEADER\n"
                    "grrr: grrr\n"
                "</Zones>\n"
                "<Attributes>\n"
                    "x_urls: URL/root.x_urls\n"
                    "attachsize: INTEGER/root.attachsize\n"
                    "attachsize_b: INTEGER/root.attachsize_b\n"
                    "mimetype: LITERAL/root.attachtype\n"
                    "suid: LITERAL/root.suid\n"
                    "mid: LITERAL/root.mid\n"
                    "received_date: DATE,headers/root.received_date\n"
                    "hid: LITERAL/root.hid\n"
                    "_: LITERAL,any/grrr._\n"
                "</Attributes>\n"
                "<TextFlags>\n"
                    "WEIGHT_LOW: HDR_CC\n"
                    "WEIGHT_BEST: HDR_BCC\n"
                "</TextFlags\n>"
            "</DOCTYPE>\n"
        "</XMLParser>\n");

const TString HtmlParserConfig (
        "<HtmlParser>\n"
            "<Zones>\n"
                "hdr_from = div/HDR_FROM_attr\n"
                "reply_to = div/REPLY_TO_attr\n"
                "hdr_to = div/HDR_TO_attr\n"
                "hdr_subject = div/HDR_SUBJECT_attr\n"
                "body_text = div/BODY_TEXT_attr\n"
                "hdr_cc = div/HDR_CC_attr\n"
                "hdr_bcc = div/HDR_BCC_attr\n"
                "attachname = div/ATTACHNAME_attr\n"
                "x_urls = div/URLS_attr\n"
                "headers = div/HEADERS_attr\n"
                "z_text = div/marker_attr\n"
                "</Zones>\n"
            "<Attributes>\n"
                "HDR_FROM_attr = LITERAL,any/div.yx:HDR_FROM\n"
                "REPLY_TO_attr = LITERAL,any/div.yx:REPLY_TO\n"
                "HDR_TO_attr = LITERAL,any/div.yx:HDR_TO\n"
                "HDR_SUBJECT_attr = LITERAL,any/div.yx:HDR_SUBJECT\n"
                "attr_aa_grp = INTEGER,doc,parse_string_to_fnvui32,ignore/meta.attr_aa\n"
                "attr_bb_prop = LITERAL,doc,,any/meta.attr_bb\n"
                "attr_cc_grp = LITERAL,doc,,any/meta.attr_cc\n"
                "BODY_TEXT_attr = LITERAL,any/div.yx:BODY_TEXT\n"
                "HDR_CC_attr = LITERAL,any/div.yx:HDR_CC\n"
                "HDR_BCC_attr = LITERAL,any/div.yx:HDR_BCC\n"
                "ATTACHNAME_attr = LITERAL,any/div.yx:ATTACHNAME\n"
                "URLS_attr = LITERAL,any/div.yx:X_URLS\n"
                "HEADERS_attr = LITERAL,any/div.yx:HEADERS\n"
                "marker_attr = LITERAL,doc/div.marker\n"
                "received_date = DATE,headers,,,,any/a.date\n"
                "attachsize = INTEGER/a.attachsize\n"
                "xxx_urls = URL/a.xxx_urls\n"
                "bool_flag = BOOLEAN/a.bool_flag\n"
                "tag = LITERAL/a.tag\n"
                "apocalipse_date = DATE/a.app_date\n"
                "grr_attr: LITERAL,any/div.group\n"
                "</Attributes>\n"
        "</HtmlParser>\n"
);

const TString FactorsConfig (
R"({
    "static_factors":{"stat1":7,"stat2":1,"stat3":2},
    "ignored_factors": {"ignore1": 200, "ignore2": 201},
    "ann_streams":{"DT_CORRECTED_CTR":{"index_type":"SI_ANN"}, "DT_ONE_CLICK":{"index_type":"SI_ANN"}},
    "dynamic_factors":{"TR":3,"TRDocQuorum":30,"TextBM25":4,"DocLen":0},
    "user_factors":{"user1":5,"user2":6},
    "rty_dynamic_factors":{"RefineUserFactor":10},
    "formulas":{
        "default":{
            "polynom":"100500E00400G6JPCEFQC6J9UH6JPCQ7" // 0.1 * f[0] + 0.2 * f[1] + 0.3 * f[2];
        },
        "dyn":{
            "polynom":"400V10M00400G6JPCEF0000K02000G28" // 0.1 * f[3] + 5 * f[30] + 5 * f[4];
        },
        "alternative":{
            "polynom":"800D002008JPC6N70000A01" // 0.1 * f[7] + 5.0 * f[5];
        }
    }
    })");
