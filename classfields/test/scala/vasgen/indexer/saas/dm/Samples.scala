package vasgen.indexer.saas.dm

object Samples {

  val completeRelevConf: String =
    s"""
       |{
       |  "geo_layers": {
       |    "s_offer_region_g": {
       |      "stream_id": 0
       |    }
       |  },
       |  "user_functions": {
       |    "EmbeddingsJson": "DSSM"
       |  },
       |  "dynamic_factors": {
       |    "LongQuery": 0,
       |    "InvWordCount": 1
       |  },
       |  "user_factors": {
       |    "sort_hash": 21,
       |    "dssm_dot_product": {
       |      "index": 24,
       |      "default_value": 0.0
       |    }
       |  },
       |  "rty_dynamic_factors": {
       |    "_Time__f_f_offer_publish_date_g": 31
       |  },
       |  "static_factors": {
       |    "f_general_no_photo": {
       |      "index": 50,
       |      "default_value": 1
       |    }
       |  },
       |  "zone_factors": {
       |    "_BM25F_Sy_z_offer_title_g": 61
       |  },
       |  "formulas": {
       |    "pruning": {
       |      "matrixnet": "$${WorkDir and WorkDir or _BIN_DIRECTORY}/configs/formula-2415-matrixnet_remap.info",
       |      "polynom": "O10F30070SE08000AOL72U00000V30000SF1"
       |    }
       |  }
       |}
       |""".stripMargin

  val incompleteRelevConf: String =
    s"""
       |{
       |  "dynamic_factors": {
       |    "LongQuery": 0,
       |    "InvWordCount": 1
       |  },
       |  "static_factors": {
       |    "f_general_no_photo": {
       |      "index": 50,
       |      "default_value": 1
       |    }
       |  }
       |}
       |""".stripMargin

  val completeRtyState: String =
    s"""
       |{
       |    "Merger.MaxDocumentsToMerge" : "30000000",
       |    "Merger.MaxSegments" : "1",
       |    "SearchersCountLimit": "2",
       |    "Searcher.EnableUrlHash" : "1",
       |    "Searcher.SnippetsDeniedZones":"z_performer_zone,z_title_zone",
       |    "Searcher.ArchiveType" : "AT_MULTIPART",
       |    "Searcher.LockIndexFiles":"true",
       |    //"Searcher.ArchivePolicy": "INMEM",
       |    "PruneAttrSort":"formula:pruning",
       |
       |    "Searcher.HttpOptions.MaxQueueSize": "20",
       |    "Searcher.HttpOptions.Threads": "10",
       |    
       |    "Searcher.FactorsInfo" : "$${WorkDir and WorkDir or _BIN_DIRECTORY}/configs/relev.conf-vasgen_search_lb",
       |
       |    "Indexer.Memory.Enabled":"false",
       |    "Indexer.Common.Groups":"popularity:2:unique",
       |    "Indexer.Common.TokenizeUrl":"false",
       |    "Indexer.Common.TextArchiveParams.Compression" : "COMPRESSED_EXT",
       |    "Indexer.Common.TextArchiveParams.PopulationRate": "0.9",
       |    "Indexer.Common.TextArchiveParams.PartSizeLimit": "67108864",
       |    "Indexer.Common.TextArchiveParams.PartSizeDeviation": "0.25",
       |    "Indexer.Common.TextArchiveParams.MaxUndersizedPartsCount": "128",
       |
       |    "Indexer.Disk.TimeToLiveSec":"300",
       |
       |    "ComponentsConfig.FULLARC.ActiveLayers": "base",
       |
       |    "AdditionalModules" : "Synchronizer,DOCFETCHER",
       |    "PreserveModulesOrder" : true,
       |    "Searcher.AutoStartServer": false,
       |    "Indexer.Common.PauseOnStart": true,
       |
       |    "ModulesConfig.DOCFETCHER.Enabled": true,
       |    "ModulesConfig.DOCFETCHER.EnableSearchOnStart": "true",
       |    "ModulesConfig.DOCFETCHER.SysLogFile": "$${LOG_PATH or '/var/log'}/current-docfetcher-sys$${LOG_POSTFIX or '.log'}",
       |
       |    "ModulesConfig.DOCFETCHER.DatacenterChecker.StreamType": "PersQueue",
       |    "ModulesConfig.DOCFETCHER.DatacenterChecker.Host": "logbroker.yandex.net",
       |    "ModulesConfig.DOCFETCHER.DatacenterChecker.Query": "clusters",
       |    "ModulesConfig.DOCFETCHER.DatacenterChecker.Port": "8999",
       |
       |    "ModulesConfig.DOCFETCHER.Stream.Name": "logbroker",
       |    "ModulesConfig.DOCFETCHER.Stream.StreamType": "PersQueue",
       |    "ModulesConfig.DOCFETCHER.Stream.Server": "logbroker.yandex.net",
       |
       |    "ModulesConfig.DOCFETCHER.Stream.Ident": "saas@services@$${SERVICE}@$${CTYPE}@topics",
       |    "ModulesConfig.DOCFETCHER.Stream.Datacenters": "$${(LOCATION=='MAN' and 'man,vla,sas' or LOCATION=='VLA' and 'vla,sas,man' or 'sas,vla,man')}",
       |    "ModulesConfig.DOCFETCHER.Stream.UseShardedLogtype": true,
       |    "ModulesConfig.DOCFETCHER.Stream.Replica": "$${ BSCONFIG_INAME }",
       |    "ModulesConfig.DOCFETCHER.Stream.QueueSize": 500,
       |    "ModulesConfig.DOCFETCHER.Stream.MaxAgeToGetSec": 129600,
       |    "ModulesConfig.DOCFETCHER.Stream.OverlapAge": 60,
       |    "ModulesConfig.DOCFETCHER.Stream.LockServers": "saas-zookeeper1.search.yandex.net:14880,saas-zookeeper2.search.yandex.net:14880,saas-zookeeper3.search.yandex.net:14880,saas-zookeeper4.search.yandex.net:14880,saas-zookeeper5.search.yandex.net:14880",
       |    "ModulesConfig.DOCFETCHER.Stream.LockPath": "/saas10/logbroker/consumers/saas-cloud/$${SERVICE}/$${CTYPE}",
       |    "ModulesConfig.DOCFETCHER.Stream.SyncThreshold" : 129600,
       |    "ModulesConfig.DOCFETCHER.Stream.SyncServer" : "arnold",
       |    "ModulesConfig.DOCFETCHER.Stream.SyncPath" : 
       | "//home/saas/ferryman-$${CTYPE:gsub('%_', '-')}/$${SERVICE}/dishes",
       |    "ModulesConfig.DOCFETCHER.Stream.SnapshotManager" : "yt",
       |    "ModulesConfig.DOCFETCHER.Stream.ResourceFetchConfig.YTFetch.Proxy" : "arnold",
       |    "ModulesConfig.DOCFETCHER.Stream.ResourceFetchConfig.YTFetch.WriteBytesPerSec" : "$${10*1024*1024}",
       |    "ModulesConfig.DOCFETCHER.Stream.ResourceFetchConfig.SkyGet.DownloadSpeedBps": "$${10*1024*1024}",
       |    "ModulesConfig.DOCFETCHER.Stream.ResourceFetchConfig.SkyGet.UploadSpeedBps": "$${10*1024*1024}",
       |    "ModulesConfig.DOCFETCHER.Stream.ResourceFetchConfig.SkyGet.Timeout": "14400",
       |
       |    //SAAS-5262
       |    "Indexer.Common.TextArchiveParams.CompressionExtParams.CodecName" : "zstd08d-2",
       |    "Indexer.Common.TextArchiveParams.CompressionExtParams.BlockSize" : "16384",
       |    "Indexer.Common.TextArchiveParams.CompressionExtParams.LearnSize" : "524288",
       |
       |    "ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.BlockSize": "4096",
       |    "ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.CodecName": "zstd08d-1",
       |    "ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.LearnSize": "262144",
       |    "Repair.Enabled": false,
       |
       |    //SAASSUP-1774
       |    "Components":"INDEX,FULLARC,MinGeo,DSSM,ANN",
       |    "ComponentsConfig.FULLARC.LightLayers":"MinGeo,DSSM",
       |    "ComponentsConfig.FULLARC.Layers.MinGeo.Compression" : "RAW",
       |    "ComponentsConfig.FULLARC.Layers.DSSM.Compression" : "RAW",
       |    "ComponentsConfig.FULLARC.Layers.DSSM.ReadContextDataAccessType": "MEMORY_LOCKED_MAP",
       |    "ComponentsConfig.FULLARC.Layers.MinGeo.ReadContextDataAccessType":"MEMORY_FROM_FILE",
       |    
       |    "ComponentsConfig.Ann.DefaultLanguage": "rus",
       |    "ComponentsConfig.Ann.ImitateQrQuorum": "1",
       |    "Searcher.AdditionalLockedFiles": "indexarc.fat;indexsent;index.docurl;indexann.fat;indexann.sent;indexfactorann.fat;indexfactorann.sent",
       |    "ComponentsConfig.Ann.MultipartConfig.ReadContextDataAccessType": "MEMORY_LOCKED_MAP",
       |    "ComponentsConfig.Ann.MultipartConfig.PartSizeLimit": "10485760",
       |    "ComponentsConfig.Ann.MultipartConfig.MaxUndersizedPartsCount": "8"
       |}""".stripMargin

  val rtyState1: String =
    s"""
       |{
       |    //SAASSUP-1774
       |    "Components":"INDEX,FULLARC,MinGeo,DSSM"
       |}""".stripMargin

  val rtyState2: String =
    s"""
       |{
       |    "Repair.Enabled": false
       |}""".stripMargin

}
