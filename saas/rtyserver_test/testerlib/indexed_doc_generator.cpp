#include "indexed_doc_generator.h"
#include <saas/rtyserver/components/indexer/indexer/parsed_doc_storage.h>
#include <saas/rtyserver/factors/factors_config.h>
#include <saas/rtyserver/indexer_core/parsed_document.h>
#include <saas/rtyserver/indexer_core/index_component_storage.h>
#include <saas/rtyserver/components/indexer/index_component.h>
#include <saas/rtyserver/components/indexer/index_parsed_entity.h>
#include <saas/rtyserver/config/common_indexers_config.h>
#include <saas/rtyserver/config/config.h>
#include <saas/rtyserver/config/grouping_config.h>
#include <saas/rtyserver/config/searcher_config.h>
#include <saas/rtyserver/config/shards_config.h>
#include <kernel/docindexer/idstorage.h>

class TIndexedDocGenerator::TImpl {
public:
    TImpl(const TBackendProxy &backend);
    void ProcessDoc(NRTYServer::TMessage& message);

private:
    TAttrProcessorConfig AttrProcessorConfig;
    THolder<TIndexedDocStorage> IndexedDocStorage;
    THolder<TIndexedDocStorageConfig> IndexedDocStorageConfig;
    TRTYParsedDocStorage* ParsedDocStorage;
    TDaemonConfigPtr DaemonConfig;
    TRTYServerConfig Config;
};

TIndexedDocGenerator::TImpl::TImpl(const TBackendProxy &backend)
    : DaemonConfig(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false))
    , Config(DaemonConfig)
{
    TConfigFieldsPtr backEndConfig(new TConfigFields);
    TConfigFields& beConf = *backEndConfig;
    beConf["Indexer.Common.HtmlParserConfigFile"] = "";
    beConf["Indexer.Common.XmlParserConfigFile"] = "";
    beConf["Indexer.Common.RecognizeLibraryFile"] = "";
    beConf["Indexer.Common.DocProperty"] = "";
    beConf["Indexer.Common.Groups"] = "";
    beConf["Searcher.FactorsInfo"] = "";
    beConf["DoStoreArchive"] = "";
    beConf["NoMorphology"] = "";
    beConf["IsPrefixedIndex"] = "";
    beConf["PruneAttrSort"] = "";
    beConf["DefaultCharset"] = "";
    beConf["DefaultLanguage"] = "";
    beConf["DefaultLanguage2"] = "";
    beConf["ShardsNumber"] = "";
    backEndConfig = backend.GetConfigValues(backEndConfig);
    TConfigFields& beResp = *backEndConfig;
    NRTYServer::TCommonIndexersConfig& rtyIndConf = Config.GetCommonIndexers();
    rtyIndConf.HtmlParserConfigFile = beResp["Indexer.Common.HtmlParserConfigFile"].Value;
    rtyIndConf.XmlParserConfigFile = beResp["Indexer.Common.XmlParserConfigFile"].Value;
    rtyIndConf.RecognizeLibraryFile = beResp["Indexer.Common.RecognizeLibraryFile"].Value;
    rtyIndConf.DocProperty = beResp["Indexer.Common.DocProperty"].Value;
    rtyIndConf.GroupingConfig.Reset(new NRTYServer::TGroupingConfig(beResp["Indexer.Common.Groups"].Value));
    rtyIndConf.DefaultCharset = CharsetByName(beResp["DefaultCharset"].Value.data());
    rtyIndConf.DefaultLanguage = LanguageByName(beResp["DefaultLanguage"].Value);
    rtyIndConf.DefaultLanguage2 = LanguageByName(beResp["DefaultLanguage2"].Value);
    Config.IsPrefixedIndex = FromString<bool>(beResp["IsPrefixedIndex"].Value);
    Config.DoStoreArchive = FromString<bool>(beResp["DoStoreArchive"].Value);
    Config.NoMorphology = FromString<bool>(beResp["NoMorphology"].Value);
    Config.GetSearcherConfig().Factors.Reset(new NRTYFactors::TConfig(beResp["Searcher.FactorsInfo"].Value.data()));
    Config.Pruning.Reset(TPruningConfig::Create(beResp["Searcher.PruneAttrSort"].Value));
    Config.GetShardsConfig().Init(beResp["ShardsNumber"].Value.data());

    TBaseConfig baseConfig;
    baseConfig.DocCount = 1;
    baseConfig.MaxMemory = 0;
    baseConfig.Indexaa = "indexaa";
    baseConfig.RecognizeLibraryFile = rtyIndConf.RecognizeLibraryFile == "NOTSET" ? "" : rtyIndConf.RecognizeLibraryFile;
    baseConfig.Groups = rtyIndConf.GroupingConfig->ToString();
    baseConfig.DocProperties = rtyIndConf.DocProperty;
    baseConfig.StoreSegmentatorData = false;
    baseConfig.NoMorphology = Config.NoMorphology;
    baseConfig.UseArchive = Config.DoStoreArchive;
    baseConfig.ParserConfig = rtyIndConf.HtmlParserConfigFile;
    baseConfig.XmlParserConf = rtyIndConf.XmlParserConfigFile;

    IndexedDocStorageConfig.Reset(new TIndexedDocStorageConfig(baseConfig));
    IndexedDocStorageConfig->UseDater = false;
    IndexedDocStorageConfig->IsRtYServer = true;
    TIndexedDocStorageConstructionData idscd(*IndexedDocStorageConfig, AttrProcessorConfig);
    ParsedDocStorage = new TRTYParsedDocStorage(baseConfig, Config.GetCommonIndexers().ZonesToProperties);
    idscd.CustomParsedDocStorage.Reset(ParsedDocStorage);
    IndexedDocStorage.Reset(new TIndexedDocStorage(idscd));
}

void TIndexedDocGenerator::TImpl::ProcessDoc(NRTYServer::TMessage& message) {
    if (message.GetMessageType() != NRTYServer::TMessage::ADD_DOCUMENT
        && message.GetMessageType() != NRTYServer::TMessage::MODIFY_DOCUMENT)
        return;

    TParsedDocument document(Config);
    NRTYServer::TDocParseContext parseContext;
    TIndexComponentsStorage::Instance().GetDocumentParser().Parse(document, message.GetDocument(), message.GetMessageType(), parseContext);
    TIndexParsedEntity* docIndex = document.GetComponentEntity<TIndexParsedEntity>(INDEX_COMPONENT_NAME);
    VERIFY_WITH_LOG(docIndex, "no dicIndex");
    TDocInfoEx& docInfo = docIndex->MutableDocInfo();
    docInfo.DocId = 0;
    NRealTime::TIndexedDoc* indexedDoc = message.MutableDocument()->MutableIndexedDoc();
    IndexedDocStorage->IndexDoc(docInfo, docIndex->MutableExtAttrs(), indexedDoc, docInfo.FullUrl, false);
    indexedDoc->SetUrl(docInfo.FullUrl);
    message.MutableDocument()->ClearSearchAttributes();
    message.MutableDocument()->ClearGroupAttributes();
    message.MutableDocument()->ClearBody();
    message.MutableDocument()->ClearDocumentProperties();
}

void TIndexedDocGenerator::ProcessDoc(NRTYServer::TMessage& message) {
    Impl->ProcessDoc(message);
}

TIndexedDocGenerator::TIndexedDocGenerator(const TBackendProxy &backend)
: Impl(new TImpl(backend))
{}

TIndexedDocGenerator::~TIndexedDocGenerator() {
    delete Impl;
}
