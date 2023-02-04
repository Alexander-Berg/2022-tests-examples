# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import sys
import time
import string

from datetime import datetime

from faker import Faker
from faker.providers import BaseProvider

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider

fake = Faker()
fake.add_provider(CommonProvider)


COMMON_MOUNT_POINTS = ('/logs', '/data', '/cores', '/state')


class Provider(BaseProvider):
    def info_server_resource(self, size=0, cached=None, verification=None):
        size = size if size is not None else self.random_int(0, sys.maxsize)
        cached = cached if cached is not None else fake.pybool()
        verification = verification if verification is not None else "MD5:{}".format(fake.md5)
        return {
            "@class": "ru.yandex.iss.Resource",
            "cached": cached,
            "size": size,
            "storage": "/place",
            "trafficClass": {"downloadSpeedLimit": 0},
            "urls": ["rbtorrent:{}".format(fake.random_hexadecimal_string(40))],
            "uuid": fake.random_hexadecimal_string(40),
            "verification": {"checkPeriod": "0d0h0m", "checksum": verification}
        }

    def info_server_volume(self, quota, root=False, mount_point=None, shared=None, has_uuid=None, properties=None):
        if mount_point is None:
            mount_point = '/' if root else self.random_element(COMMON_MOUNT_POINTS)
        if shared is None:
            shared = False if root else True
        if has_uuid is None:
            has_uuid = False if root else True

        volume = {
            "@class": "ru.yandex.iss.LayeredVolume",
            "mountPoint": mount_point,
            "quota": str(quota),
            "quotaCwd": str(quota),
            "rootVolume": root,
            "shared": shared,
            "storage": "/place",
            "uuidIfSet": {"present": has_uuid}
            }

        if has_uuid:
            volume['uuid'] = self.bothify('################-################', letters=string.ascii_lowercase+string.digits)

        if root:
            volume['layers'] = [self.info_server_resource(0, False, 'EMPTY:')]
            volume['properties'] = {'bind': '/usr/local/yasmagent /usr/local/yasmagent ro'}

        if properties is not None:
            volume['properties'] = properties

    def info_server_volumes(self):
        return [
            self.info_server_volume(1073741824, root=True),
            self.info_server_volume(42949672960, mount_point='/logs'),
            self.info_server_volume(34359738368, mount_point='/cores'),
            self.info_server_volume(53687091200, mount_point='/data'),
            self.info_server_volume(1073741824,  mount_point='/state'),
            self.info_server_volume(
                107373108658176,
                mount_point='/Berkanavt/supervisor',
                shared=False, has_uuid=False,
                properties={
                    'backend': 'bind',
                    'read_only': 'true',
                    'storage': '/Berkanavt/supervisor'
                }
            )
        ]

    def random_interval(self, shards=1):
        if shards == 1:
            return 0, 65533
        else:
            shard_number = self.random_int(1, shards)
            shard_size = 65533//shards
            shard_start = 0 if shard_number == 1 else shard_size * (shard_number - 1)
            shard_end = 65533 if shard_number == shards else shard_size * shard_number - 1
            return shard_start, shard_end

    def info_server(self, search_ban=None, physical_host=None):
        search_ban = search_ban if search_ban is not None else fake.pybool()
        info_server = {
            "command": "get_info_server",
            "description": "arguments: filter=string (regex are supported)",
            "id": "cc_2019-08-20T11:13:13.187941Z_139982580832016",
            "result": {
                "Build_date": "",
                "Build_host": "linux-ubuntu-12-04-precise",
                "Svn_author": "derrior",
                "Svn_revision": "5430367",
                "Svn_root": "svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/branches/saas/2019.08.03/arcadia",
                "active_contexts": 0,
                "active_repliers": 0,
                "add_docs": 0,
                "archive_info": {
                    "alive_count": 0,
                    "full_size_bytes": 0,
                    "optimizations": {},
                    "parts_count": 0,
                    "removed_count": 0
                },
                "caches_state": {"index_0000000000_0000000308": 1},
                "closing_indexers": 0,
                "components": {},
                "config": {
                    "DaemonConfig": [
                        {
                            "Controller": [
                                {
                                    "AutoStop": "0",
                                    "BindAddress": "",
                                    "ClientTimeout": "200",
                                    "CompressionEnabled": "0",
                                    "ConfigsControl": "1",
                                    "ConfigsRoot": "/place/db/iss3/instances/iw5y7w5xqelirokm_saas_yp_cloud_entity_lists_8qodrnYCk4O/configs",
                                    "DMOptions": [
                                        {
                                            "CType": "stable",
                                            "ConnectionTimeout": "100",
                                            "Enabled": "1",
                                            "Host": "saas-dm.yandex.net",
                                            "InteractionTimeout": "60000",
                                            "Port": "80",
                                            "Service": "entity_lists",
                                            "ServiceType": "rtyserver",
                                            "Slot": "iw5y7w5xqelirokm.man.yp-c.yandex.net:80",
                                            "TimeoutS": "45",
                                            "UriPrefix": ""
                                        }
                                    ],
                                    "EnableNProfile": "0",
                                    "Enabled": "1",
                                    "ExpirationTimeoutMs": "0",
                                    "FThreads": "1",
                                    "Host": "",
                                    "KeepAliveEnabled": "1",
                                    "ListenBacklog": "128",
                                    "Log": "/logs//current-controller-rtyserver",
                                    "MaxConnections": "0",
                                    "MaxFQueueSize": "0",
                                    "MaxQueueSize": "0",
                                    "OutputBufferSize": "0",
                                    "PollTimeoutMs": "0",
                                    "Port": "83",
                                    "ReinitLogsOnRereadConfigs": "1",
                                    "RejectExcessConnections": "0",
                                    "ReuseAddress": "1",
                                    "ReusePort": "0",
                                    "StartServer": "1",
                                    "StateRoot": "/state/",
                                    "Threading": [{"PresetName": ""}],
                                    "Threads": "20"
                                }
                            ],
                            "EnableStatusControl": "1",
                            "LockExecutablePages": "1",
                            "LogLevel": "6",
                            "LogQueueSize": "100000",
                            "LogRotation": "0",
                            "LoggerIName": "",
                            "LoggerType": "/logs//current-global-base",
                            "MetricsMaxAge": "10",
                            "MetricsPrefix": "Refresh_",
                            "PidFile": "",
                            "SpecialProcessors": "",
                            "StdErr": "/logs//current-rtyserver-stderr",
                            "StdOut": "/logs//current-rtyserver-stdout"
                        }
                    ],
                    "Server": [
                        {
                            "AdditionalModules": "DOCFETCHER,Synchronizer",
                            "BaseSearchersServer": [
                                {
                                    "BindAddress": "",
                                    "ClientTimeout": "200",
                                    "CompressionEnabled": "0",
                                    "ExpirationTimeoutMs": "0",
                                    "FThreads": "1",
                                    "FactorQueueSize": "0",
                                    "FactorThreads": "0",
                                    "FetchQueueSize": "0",
                                    "FetchThreads": "0",
                                    "Host": "",
                                    "InfoQueueSize": "0",
                                    "InfoThreads": "0",
                                    "KeepAliveEnabled": "1",
                                    "ListenBacklog": "128",
                                    "LongReqsQueueSize": "0",
                                    "LongReqsThreads": "0",
                                    "MainQueueSize": "5",
                                    "MainThreads": "6",
                                    "MaxConnections": "0",
                                    "MaxFQueueSize": "0",
                                    "MaxQueueSize": "5",
                                    "OutputBufferSize": "0",
                                    "PollTimeoutMs": "0",
                                    "Port": "81",
                                    "RejectExcessConnections": "0",
                                    "ReuseAddress": "1",
                                    "ReusePort": "0",
                                    "Threads": "6"
                                }
                            ],
                            "CanIgnoreIndexOriginOnRemoving": "1",
                            "Components": "",
                            "ComponentsConfig": [None],
                            "DDKManager": "DDK",
                            "DeadDocsClearIntervalSeconds": "20",
                            "DeadIndexesClearIntervalSeconds": "20",
                            "DoStoreArchive": "1",
                            "ExternalLogicConfig": [None],
                            "IndexDir": "/data/index/project/",
                            "IndexGenerator": "INDEX",
                            "Indexer": [
                                {
                                    "Common": [
                                        {
                                            "DefaultCharset": "utf-8",
                                            "DefaultLanguage": "rus",
                                            "DefaultLanguage2": "eng",
                                            "DocProperty": "",
                                            "DocsCountLimit": "18446744073709551615",
                                            "Enabled": "1",
                                            "Groups": "popularity:2:unique",
                                            "HtmlParserConfigFile": "",
                                            "HttpOptions": [
                                                {
                                                    "BindAddress": "",
                                                    "ClientTimeout": "200",
                                                    "CompressionEnabled": "0",
                                                    "ExpirationTimeoutMs": "0",
                                                    "FThreads": "1",
                                                    "Host": "",
                                                    "KeepAliveEnabled": "1",
                                                    "ListenBacklog": "128",
                                                    "MaxConnections": "0",
                                                    "MaxFQueueSize": "0",
                                                    "MaxQueueSize": "0",
                                                    "OutputBufferSize": "0",
                                                    "PollTimeoutMs": "0",
                                                    "Port": "82",
                                                    "RejectExcessConnections": "0",
                                                    "ReuseAddress": "1",
                                                    "ReusePort": "0",
                                                    "Threads": "4"
                                                }
                                            ],
                                            "IndexLog": "",
                                            "MadeUrlAttributes": "1",
                                            "NGrammZones": "",
                                            "OxygenOptionsFile": "",
                                            "PauseOnStart": "0",
                                            "ProtocolType": "",
                                            "RecognizeLibraryFile": "/place/db/iss3/instances/iw5y7w5xqelirokm_saas_yp_cloud_entity_lists_8qodrnYCk4O/dict.dict",
                                            "RejectDuplicates": "0",
                                            "StoreTextToArchive": "1",
                                            "StoreUpdateData": "0",
                                            "TextArchiveParams": [
                                                {
                                                    "Compression": "RAW",
                                                    "CompressionExtParams": [
                                                        {
                                                            "BlockSize": "32768",
                                                            "CodecName": "lz4",
                                                            "LearnSize": "0"
                                                        }
                                                    ],
                                                    "CompressionParams": [{"Algorithm": "LZ4", "Level": "4294967295"}],
                                                    "EnumHashSize": "8",
                                                    "EnumPackEnabled": "0",
                                                    "MaxUndersizedPartsCount": "0",
                                                    "PartSizeDeviation": "0.2",
                                                    "PartSizeLimit": "1099511627776",
                                                    "PopulationRate": "0.9",
                                                    "ReadContextDataAccessType": "DIRECT_FILE",
                                                    "WritableThredsCount": "1",
                                                    "WriteContextDataAccessType": "DIRECT_FILE"
                                                }
                                            ],
                                            "TimestampControlEnabled": "1",
                                            "TokenizeUrl": "0",
                                            "UseHTML5Parser": "0",
                                            "UseSlowUpdate": "0",
                                            "XmlParserConfigFile": "",
                                            "ZonesToProperties": [None]
                                        }
                                    ],
                                    "Disk": [
                                        {
                                            "AllowSameUrls": "1",
                                            "CloseThreads": "1",
                                            "ConnectionTimeout": "100",
                                            "DbgMaxDocumentsTotal": "0",
                                            "DocumentsQueueSize": "10000",
                                            "MaxDocuments": "1000000",
                                            "MaxSegments": "0",
                                            "PhaseToLiveSec": "-1",
                                            "PortionDocCount": "900",
                                            "PreparatesMode": "0",
                                            "SearchEnabled": "0",
                                            "SearchObjectsDirectory": "",
                                            "Threads": "2",
                                            "TimeToLiveSec": "1200",
                                            "WaitCloseForMerger": "0"
                                        }
                                    ],
                                    "Memory": [
                                        {
                                            "AllowSameUrls": "1",
                                            "ConnectionTimeout": "100",
                                            "DbgMaxDocumentsTotal": "0",
                                            "DocumentsLimit": "0",
                                            "DocumentsQueueSize": "10000",
                                            "Enabled": "0",
                                            "GarbageCollectionTime": "10",
                                            "MaxDocuments": "4000000",
                                            "MaxDocumentsReserveCapacityCoeff": "4",
                                            "PhaseToLiveSec": "-1",
                                            "PreparatesMode": "0",
                                            "RealTimeExternalFilesPath": "",
                                            "RealTimeFeatureConfig": "",
                                            "RealTimeLoadC2P": "",
                                            "RealTimeOxygenOptionsFile": "",
                                            "Threads": "1",
                                            "TimeToLiveSec": "20"
                                        }
                                    ]
                                }
                            ],
                            "IsPrefixedIndex": "1",
                            "IsReadOnly": "0",
                            "IsSecondaryMetaServiceComponent": "0",
                            "Logger": [{"JournalDir": ""}],
                            "Merger": [
                                {
                                    "ClearRemoved": "1",
                                    "DocIdGenerator": "DEFAULT",
                                    "Enabled": "1",
                                    "IndexSwitchSystemLockFile": "",
                                    "LockPolicy": "OnSwitch",
                                    "MaxDeadlineDocs": "0",
                                    "MaxDocumentsToMerge": "30000000",
                                    "MaxSegments": "1",
                                    "MergerCheckPolicy": "TIME",
                                    "NewIndexDefermentSec": "0",
                                    "SegmentsSort": "SIZE",
                                    "Threads": "4",
                                    "TimingCheckIntervalMilliseconds": "300000",
                                    "WriteSpeedBytes": "31457280"
                                }
                            ],
                            "ModulesConfig": [
                                {
                                    "DOCFETCHER": [
                                        {
                                            "EnableSearchOnStart": "0",
                                            "Enabled": "1",
                                            "LogFile": "/logs//current-docfetcher-base",
                                            "SearchOpenDocAgeSec": "-1",
                                            "Stream": [
                                                {
                                                    "AsyncStart": "0",
                                                    "ConsumeMode": "replace",
                                                    "FetchMaxAttempts": "3",
                                                    "FetchRetryPause": "100",
                                                    "FilterShardKpsShift": "0",
                                                    "FilterShardMax": "0",
                                                    "FilterShardMin": "0",
                                                    "FilterShardType": "url_hash",
                                                    "FixedTimestampUnix": "0",
                                                    "IterationsPause": "600",
                                                    "Mode": "OnSchedule",
                                                    "Name": "Snapshot",
                                                    "NumShards": "1",
                                                    "ResourceFetchConfig": [
                                                        {
                                                            "YTFetch": [
                                                                {
                                                                    "LockMaxAttempts": "10",
                                                                    "MinutesOfWaitingForLock": "15",
                                                                    "PathToLock": "",
                                                                    "Proxy": "hahn",
                                                                    "Token": "",
                                                                    "WriteBytesPerSec": "10485760",
                                                                    "YTHosts": ""
                                                                }
                                                            ]
                                                        }
                                                    ],
                                                    "Shard": "0",
                                                    "ShardMax": "65533",
                                                    "ShardMin": "0",
                                                    "SnapshotManager": "yt",
                                                    "SnapshotPath": "//home/saas/ferryman-stable/entity_lists/dishes",
                                                    "SnapshotServer": "hahn",
                                                    "SnapshotToken": "",
                                                    "StreamId": "0",
                                                    "StreamType": "Snapshot",
                                                    "UseIndexTimestamp": "1",
                                                    "YTHosts": ""
                                                }
                                            ],
                                            "SysLogFile": "",
                                            "WatchdogOptionsFile": ""
                                        }
                                    ],
                                    "Synchronizer": [
                                        {
                                            "ClearPreviousResults": "1",
                                            "DetachPath": "/data/detach",
                                            "DetachedSegmentSize": "10000000",
                                            "DetachedSegmentSizeDeviation": "0.15",
                                            "EnableSearchOnStart": "0",
                                            "PersistentSearchBanFile": "persistent_search_ban",
                                            "PersistentSearchBanOnEmptyIndex": "0",
                                            "SlotInfoFile": "description",
                                            "ThreadsDetach": "16",
                                            "ThreadsSync": "16"
                                        }
                                    ]
                                }
                            ],
                            "Monitoring": [
                                {
                                    "CheckIntervalSeconds": "10",
                                    "CrashCodes": "",
                                    "CrashRetries": "5",
                                    "Enabled": "0",
                                    "LimitCheckFail": "10"
                                }
                            ],
                            "MorphologyLanguages": "",
                            "NoMorphology": "0",
                            "PreferedMorphologyLanguages": "",
                            "PreserveModulesOrder": "0",
                            "PruneAttrSort": "formula:pruning",
                            "Repair": [{"Enabled": "0", "NormalizerThreads": "1", "Threads": "4"}],
                            "ResourceFetchConfig": [None],
                            "Searcher": [
                                {
                                    "AccessLog": "/logs//current-loadlog-rtyserver",
                                    "AdditionalLockedFiles": "",
                                    "AdditionalPrefetchedFiles": "",
                                    "AllowEmptyNehReplier": "0",
                                    "ArchiveCacheSizeMb": "0",
                                    "ArchivePolicy": "INMEM",
                                    "ArchiveType": "AT_FLAT",
                                    "AsyncSearch": "1",
                                    "AutoStartServer": "1",
                                    "BroadcastFetch": "0",
                                    "CustomBaseSearch": "",
                                    "CustomSearcher": "default",
                                    "DefaultBaseSearchConfig": "",
                                    "DefaultMemorySearchConfig": "",
                                    "DefaultMetaSearchConfig": "",
                                    "DelegateRequestOptimization": "0",
                                    "EnableCacheForDbgRlv": "0",
                                    "EnableUrlHash": "0",
                                    "Enabled": "1",
                                    "EventLog": "",
                                    "ExceptionOnSearch": "0",
                                    "ExternalSearch": "rty_relevance",
                                    "FactorsInfo": "/place/db/iss3/instances/iw5y7w5xqelirokm_saas_yp_cloud_entity_lists_8qodrnYCk4O/configs/relev.conf-entity_lists",
                                    "FiltrationModel": "WEIGHT",
                                    "GroupPruningCoefficient": "1000",
                                    "HttpOptions": [
                                        {
                                            "BindAddress": "",
                                            "ClientTimeout": "200",
                                            "CompressionEnabled": "0",
                                            "ExpirationTimeoutMs": "0",
                                            "FThreads": "1",
                                            "FactorQueueSize": "0",
                                            "FactorThreads": "0",
                                            "FetchQueueSize": "0",
                                            "FetchThreads": "0",
                                            "Host": "",
                                            "InfoQueueSize": "0",
                                            "InfoThreads": "0",
                                            "KeepAliveEnabled": "1",
                                            "ListenBacklog": "128",
                                            "LongReqsQueueSize": "0",
                                            "LongReqsThreads": "0",
                                            "MainQueueSize": "5",
                                            "MainThreads": "3",
                                            "MaxConnections": "0",
                                            "MaxFQueueSize": "0",
                                            "MaxQueueSize": "5",
                                            "OutputBufferSize": "0",
                                            "PollTimeoutMs": "0",
                                            "Port": "80",
                                            "RejectExcessConnections": "0",
                                            "ReuseAddress": "1",
                                            "ReusePort": "0",
                                            "Threads": "3"
                                        }
                                    ],
                                    "KeepAllDocuments": "0",
                                    "Limits": "",
                                    "LoadLogBase": "",
                                    "LoadLogMeta": "",
                                    "LocalHostName": "",
                                    "LocalSearcherHost": "127.0.0.1",
                                    "LockIndexFiles": "1",
                                    "LockUrl2DocId": "1",
                                    "PageScanSize": "10000",
                                    "PassageLogBase": "",
                                    "PassageLogMeta": "",
                                    "PrefetchSizeBytes": "0",
                                    "QueryCache": [
                                        {
                                            "Arenas": "1",
                                            "BlockSize": "0",
                                            "CacheLifeTime": "600s",
                                            "ClearOnStart": "0",
                                            "CompressionLevel": "",
                                            "Dir": "/dev/shm/entity_lists",
                                            "FileCacherSize": "1073741824",
                                            "MemoryLimit": "0",
                                            "MinCost": "0",
                                            "NonBlockingCacherEnabled": "0",
                                            "NonBlockingCacherMaxLoaderStreams": "40",
                                            "NonBlockingCacherStorerQueueSize": "40",
                                            "NonBlockingCacherStorerThreads": "5"
                                        }
                                    ],
                                    "QueryLanguage": [
                                        {
                                            "i_": "ATTR_INTEGER, doc, template",
                                            "iz_": "ATTR_INTEGER, zone, template",
                                            "s_": "ATTR_LITERAL, doc, template",
                                            "sz_": "ATTR_LITERAL, zone, template",
                                            "z_": "ZONE, doc, template"
                                        }
                                    ],
                                    "RawPassages": "0",
                                    "ReArrangeOptions": "",
                                    "ReAskBaseSearches": "0",
                                    "RedirectSearchErrorLogToGlobal1": "",
                                    "ReportMethod": "",
                                    "ReportModule": "",
                                    "RequestLimits": "",
                                    "RetryStartMetasearch": "0",
                                    "ScatterTimeout": "60000000",
                                    "SearchPath": ",yandsearch",
                                    "SkipSameDocids": "0",
                                    "SnippetsDeniedZones": "z_performer_zone,z_title_zone",
                                    "TierMinSize": "15000000",
                                    "TiersCount": "1",
                                    "TiersOverlapPercent": "0",
                                    "TwoStepQuery": "1",
                                    "UseRTYExtensions": "1",
                                    "WarmupQueries": "",
                                    "WildcardSearch": "infix"
                                }
                            ],
                            "SearchersCountLimit": "3",
                            "ShardMax": "65533",
                            "ShardMin": "0",
                            "ShardsNumber": "1(0)",
                            "StoragesConfig": [None],
                            "UrlToDocIdManager": "Url2DocId",
                            "UseExtendedProcessors": "0",
                            "VerificationPolicy": "Release",
                            "WatchdogOptionsFile": ""
                        }
                    ]
                },
                "controller_status": "Active",
                "controller_uptime": 264943,
                "cpu_count": 32,
                "cpu_load_system": 0,
                "cpu_load_user": 4,
                "default_kps": 0,
                "delete_docs": 0,
                "disk_index_rps": {"1": 0, "10": 0, "3": 0, "30": 0},
                "docfetcher_search_ban": False,
                "docs_in_disk_indexers": 0,
                "docs_in_final_indexes": 1145728,
                "docs_in_memory_indexes": 0,
                "factors_info": {
                    "dynamic_factors": {
                        "AbsolutePLM": 3,
                        "Bclm": 4,
                        "Bclm2": 7,
                        "DocLen": 6,
                        "InvWordCount": 1,
                        "LongQuery": 0,
                        "MatrixNet": 40,
                        "TLen": 2,
                        "TextBM25": 12,
                        "TextBM25_Fm_W1": 10,
                        "TextBM25_Sy_W1": 13,
                        "TitleTrigramsTitle": 9,
                        "Tocm": 8,
                        "TxtBm25Ex": 11,
                        "TxtBm25Sy": 5,
                        "TxtBreakEx": 19,
                        "TxtBreakSy": 18,
                        "TxtHead": 17,
                        "TxtHeadEx": 16,
                        "TxtHeadSy": 14,
                        "YmwFull2": 15
                    },
                    "formulas": {
                        "catboost_20190319_rc_gpu": {
                            "matrixnet": "catboost_20190319_rc_gpu.info",
                            "polynom": "910H10E308A0OD0G11010000000VJN923KE0000GV5000O780000403"
                        },
                        "catboost_20190319_rtx_300_gpu": {
                            "matrixnet": "catboost_20190319_rtx_300_gpu.info",
                            "polynom": "910H10E308A0OD0G11010000000VJN923KE0000GV5000O780000403"
                        },
                        "catboost_20190319_rtx_32_gpu": {
                            "matrixnet": "catboost_20190319_rtx_32_gpu.info",
                            "polynom": "910H10E308A0OD0G11010000000VJN923KE0000GV5000O780000403"
                        },
                        "default": {
                            "matrixnet": "catboost_20190218_v0.info",
                            "polynom": "910H10E308A0OD0G11010000000VJN923KE0000GV5000O780000403"
                        },
                        "display_rating": {"polynom": "U00H1020000000U7FJ468T0"},
                        "filmseries": {"polynom": "Q00H1020000000SNFJ468T0"},
                        "kp_rating": {"polynom": "U00H1020000000U7FJ468T0"},
                        "pruning": {"polynom": "M00F10230S600B0GR00E10E30C8080000000OV00000VJN923KE0000GV50000UN0000V01000G0C"},
                        "release_date": {"polynom": "Q00H1020000000U7CDHFIR0"}
                    },
                    "static_factors": {
                        "f_avod": {"default_value": 0, "index": 46},
                        "f_avod_start_date": {"default_value": 0, "index": 41},
                        "f_display_rating": {"default_value": 0, "index": 29},
                        "f_est_start_date": {"default_value": 0, "index": 44},
                        "f_exclusive": {"default_value": 0, "index": 26},
                        "f_has_license": {"default_value": 0, "index": 45},
                        "f_kinopoisk_formula_quantile": {"default_value": 0, "index": 35},
                        "f_kinopoisk_formula_rating": {"default_value": 0, "index": 21},
                        "f_kinopoisk_formula_rating_fixed": {"default_value": 0, "index": 30},
                        "f_kinopoisk_rating": {"default_value": 0, "index": 20},
                        "f_kinopoisk_votes": {"default_value": 0, "index": 24},
                        "f_kp_basic": {"default_value": 0, "index": 49},
                        "f_native_shows_30": {"default_value": 0, "index": 28},
                        "f_no_avod": {"default_value": 0, "index": 51},
                        "f_popularity": {"default_value": 0, "index": 22},
                        "f_porno": {"default_value": 0, "index": 32},
                        "f_random": {"default_value": 0, "index": 23},
                        "f_release_date": {"default_value": 0, "index": 25},
                        "f_shows_30": {"default_value": 0, "index": 27},
                        "f_svod_start_date": {"default_value": 0, "index": 42},
                        "f_tvod_start_date": {"default_value": 0, "index": 43},
                        "f_tvshow": {"default_value": 0, "index": 33},
                        "f_uids_30": {"default_value": 0, "index": 31},
                        "f_ya_plus": {"default_value": 0, "index": 47},
                        "f_ya_plus_3m": {"default_value": 0, "index": 48},
                        "f_ya_premium": {"default_value": 0, "index": 50},
                        "f_yandex_score": {"default_value": 0, "index": 34}
                    },
                    "user_factors": {
                        "f_match_svod": {"default_value": 0, "index": 56},
                        "f_not_match_svod": {"default_value": 0, "index": 57},
                        "f_u_kp_basic": {"default_value": 0, "index": 54},
                        "f_u_ya_plus": {"default_value": 0, "index": 52},
                        "f_u_ya_plus_3m": {"default_value": 0, "index": 53},
                        "f_u_ya_premium": {"default_value": 0, "index": 55}
                    }
                },
                "files_size": {
                    "__COUNT": 6,
                    "__SUM": 883611092,
                    "files_info": 27881,
                    "index.docurl": 16373298,
                    "index.keys.bloom": 33,
                    "index.keys.bloom.md5": 32,
                    "index.keys.inv": 56,
                    "index.keys.key": 0,
                    "index.meta": 109,
                    "index_makeup.docs": 139888964,
                    "index_makeup.hdr": 101,
                    "indexarc": 243138560,
                    "indexddk.rty": 43537664,
                    "indexddk.rty.hdr": 225,
                    "indexdir": 9165824,
                    "indexerf.rty": 123738624,
                    "indexerf.rty.hdr": 661,
                    "indexfrq": 2291456,
                    "indexfullarc.base.fat": 9165824,
                    "indexfullarc.base.meta": 72,
                    "indexfullarc.base.part": 11945943,
                    "indexfullarc.base.part.hdr": 4582912,
                    "indexfullarc.base.part.hdr_count": 2,
                    "indexfullarc.base.part.meta": 92,
                    "indexfullarc.base.part.meta_count": 2,
                    "indexfullarc.base.part_count": 2,
                    "indexinv": 197989574,
                    "indexkey": 47466720,
                    "indexlen": 1145736,
                    "indexsent": 10235784,
                    "kpsinfo": 210,
                    "timestamp": 165,
                    "url2docid": 22914572
                },
                "indexer_clients": {"disk-82_req": 0},
                "indexes": {
                    "index_0000000000_0000000295": {
                        "count": 0,
                        "count_searchable": 0,
                        "count_withnodel": 0,
                        "dir": "/data/index/project//index_0000000000_0000000295",
                        "has_searcher": 0,
                        "mlocked_size": 0,
                        "realm": "Persistent",
                        "size": 0,
                        "timestamp": 0,
                        "type": "DISK"
                    },
                    "index_0000000000_0000000308": {
                        "cache": 1,
                        "count": 1145728,
                        "count_searchable": 1145728,
                        "count_withnodel": 1145728,
                        "dir": "/data/index/project/index_0000000000_0000000308",
                        "has_searcher": 1,
                        "mlocked_size": 444801024,
                        "realm": "Persistent",
                        "size": 883611092,
                        "timestamp": 1566292140,
                        "type": "FINAL"
                    }
                },
                "indexing_enabled": True,
                "load_average": 6.04,
                "locked_memory_size": 444801024,
                "mem_size_real": "1915.676",
                "mem_size_virtual": "5530.359",
                "memory_index_rps": {"1": 0, "10": 0, "3": 0, "30": 0},
                "memory_searchers_count": 0,
                "merger_status": "Active",
                "merger_tasks": 0,
                "merger_tasks_info": [],
                "nprofile_enabled": False,
                "persistent_search_ban": False,
                "product": "rtyserver",
                "queue_disk": 0,
                "queue_mem": 0,
                "repair_tasks": 0,
                "rocksdb_docs": 0,
                "sandbox_task_id": "0",
                "search_enabled": True,
                "search_rps_failed": {"1": 0, "10": 0, "3": 0, "30": 0},
                "search_rps_http": {"1": 0, "10": 0, "3": 0, "30": 0},
                "search_rps_neh": {"1": 11.37241173, "10": 14.95662117, "3": 16.57432938, "30": 15.49956512},
                "search_rps_neh_base": {"1": 9.315459251, "10": 17.97017097, "3": 18.95111084, "30": 18.60723305},
                "search_sources_count": 1,
                "searchable_docs": 1145728,
                "server_status": "Active",
                "server_status_global": {"info": "", "state": "OK", "timestamp": "1566299593187994"},
                "server_uptime": "264940.594890s",
                "slot": {
                    "@class": "ru.yandex.iss.Instance",
                    "configurationId": "saas_yp_cloud_entity_lists#saas_yp_cloud_entity_lists-1566033575438",
                    "container": {
                        "constraints": {
                            "iss_hook_install.enable_porto": "isolate",
                            "iss_hook_install.net": "inherited",
                            "iss_hook_install.oom_is_fatal": "false",
                            "iss_hook_notify.enable_porto": "false",
                            "iss_hook_notify.net": "inherited",
                            "iss_hook_reopenlogs.enable_porto": "false",
                            "iss_hook_reopenlogs.net": "inherited",
                            "iss_hook_start.capabilities_ambient": "NET_BIND_SERVICE",
                            "iss_hook_start.enable_porto": "isolate",
                            "iss_hook_start.net": "inherited",
                            "iss_hook_start.oom_is_fatal": "false",
                            "iss_hook_status.enable_porto": "false",
                            "iss_hook_status.net": "inherited",
                            "iss_hook_stop.enable_porto": "false",
                            "iss_hook_stop.net": "inherited",
                            "iss_hook_uninstall.enable_porto": "isolate",
                            "iss_hook_uninstall.net": "inherited",
                            "iss_hook_uninstall.oom_is_fatal": "false",
                            "iss_hook_validate.enable_porto": "false",
                            "iss_hook_validate.net": "inherited",
                            "meta.enable_porto": "isolate",
                            "meta.ulimit": "memlock: 549755813888 549755813888;",
                            "ulimit": "memlock: 549755813888 549755813888;"
                        },
                        "podBindings": ["/place/db/iss3/pod_api /run/iss ro"],
                        "podContainerName": "iw5y7w5xqelirokm",
                        "withDynamicProperties": False
                    },
                    "dynamicProperties": {
                        "HBF_NAT": "disabled",
                        "NANNY_SNAPSHOT_ID": "66e98995701fa7e007cf8e235b15c34865035181",
                        "SKYNET_SSH": "enabled",
                        "nanny_container_access_url": "http://nanny.yandex-team.ru/api/repo/CheckContainerAccess/"
                    },
                    "properties": {
                        "BACKBONE_IP_ADDRESS": "2a02:6b8:c13:3beb:100:0:ebe3:0",
                        "DEPLOY_ENGINE": "YP_LITE",
                        "HOSTNAME": "iw5y7w5xqelirokm.man.yp-c.yandex.net",
                        "HOST_SKYNET": "enabled",
                        "HQ_INSTANCE_ID": "iw5y7w5xqelirokm@saas_yp_cloud_entity_lists",
                        "HQ_INSTANCE_SPEC_HASH": "3102d5aeb5865a93371058bcb7c36d1f",
                        "INSTANCE_TAG_CTYPE": "prod",
                        "INSTANCE_TAG_ITYPE": "rtyserver",
                        "INSTANCE_TAG_PRJ": "saas-entity-lists",
                        "NANNY_SERVICE_ID": "saas_yp_cloud_entity_lists",
                        "monitoringYasmagentEndpoint": "http://[2a02:6b8:c13:3beb:100:0:ebe3:0]:11003/",
                        "tags": "a_geo_man a_dc_man a_itype_rtyserver a_ctype_prod a_prj_saas-entity-lists a_metaprj_unknown a_tier_none enable_hq_report enable_hq_poll",
                        "yasmInstanceFallbackPort": "80",
                        "yasmUnistatFallbackPort": "80"
                    },
                    "resources": {
                        "dict.dict": self.info_server_resource(0, False),
                        "gdb_toolkit.tgz": self.info_server_resource(0, False),
                        "instancectl": self.info_server_resource(0, False),
                        "iss_hook_install": self.info_server_resource(0, False),
                        "iss_hook_notify": self.info_server_resource(0, False),
                        "iss_hook_reopenlogs": self.info_server_resource(0, False),
                        "iss_hook_start": self.info_server_resource(0, False),
                        "iss_hook_status": self.info_server_resource(0, False),
                        "iss_hook_stop": self.info_server_resource(0, False),
                        "iss_hook_uninstall": self.info_server_resource(0, False),
                        "logrotate": self.info_server_resource(0, False),
                        "logrotate.conf.tmpl": self.info_server_resource(0, False),
                        "loop.conf": self.info_server_resource(0, False),
                        "loop_data": self.info_server_resource(0, False),
                        "push-client": self.info_server_resource(0, False),
                        "rtyserver": self.info_server_resource(0, False)
                    },
                    "slot": "iw5y7w5xqelirokm@man2-2630.search.yandex.net",
                    "storage": "/place",
                    "targetState": "ACTIVE",
                    "timeLimits": {
                        "iss_hook_install": {
                            "maxExecutionTimeMs": 1800000,
                            "maxRestartPeriodMs": 3153600000000,
                            "minRestartPeriodMs": 180000,
                            "restartPeriodBackoff": 0,
                            "restartPeriodScaleMs": 0
                        },
                        "iss_hook_notify": {
                            "maxExecutionTimeMs": 1800000,
                            "maxRestartPeriodMs": 3153600000000,
                            "minRestartPeriodMs": 1000,
                            "restartPeriodBackoff": 0,
                            "restartPeriodScaleMs": 0
                        },
                        "iss_hook_start": {
                            "maxExecutionTimeMs": 3153600000000,
                            "maxRestartPeriodMs": 3153600000000,
                            "minRestartPeriodMs": 1000,
                            "restartPeriodBackoff": 0,
                            "restartPeriodScaleMs": 0
                        },
                        "iss_hook_status": {
                            "maxExecutionTimeMs": 1800000,
                            "maxRestartPeriodMs": 60000,
                            "minRestartPeriodMs": 30000,
                            "restartPeriodBackoff": 2,
                            "restartPeriodScaleMs": 1000
                        },
                        "iss_hook_stop": {
                            "maxExecutionTimeMs": 30000,
                            "maxRestartPeriodMs": 3153600000000,
                            "minRestartPeriodMs": 1000,
                            "restartPeriodBackoff": 0,
                            "restartPeriodScaleMs": 0
                        },
                        "iss_hook_uninstall": {
                            "maxExecutionTimeMs": 30000,
                            "maxRestartPeriodMs": 3153600000000,
                            "minRestartPeriodMs": 1000,
                            "restartPeriodBackoff": 0,
                            "restartPeriodScaleMs": 0
                        },
                        "iss_hook_validate": {
                            "maxExecutionTimeMs": 1800000,
                            "maxRestartPeriodMs": 3153600000000,
                            "minRestartPeriodMs": 1000,
                            "restartPeriodBackoff": 0,
                            "restartPeriodScaleMs": 0
                        }
                    },
                    "transitionTimestamp": 0,
                    "volumes": self.info_server_volumes()
                },
                "slot_info_search_ban": search_ban,
                "status": {
                    "CapAmb": "0000000000000400",
                    "CapBnd": "00000000a84c75fb",
                    "CapEff": "0000000000000400",
                    "CapInh": "0000000000000400",
                    "CapPrm": "0000000000000400",
                    "Cpus_allowed": "ffffffff",
                    "Cpus_allowed_list": "0-31",
                    "FDSize": 256,
                    "Gid": "1049 1049 1049 1049",
                    "Groups": "300 457 1049 1055 1333",
                    "HugetlbPages": 0,
                    "Mems_allowed": "00000000,00000003",
                    "Mems_allowed_list": "0-1",
                    "NSpgid": 120,
                    "NSpid": 120,
                    "NSsid": 120,
                    "NStgid": 120,
                    "Name": "rtyserver",
                    "Ngid": 0,
                    "PPid": 119,
                    "Pid": 120,
                    "RssAnon": 637538304,
                    "RssFile": 1371193344,
                    "RssShmem": 0,
                    "Seccomp": 0,
                    "ShdPnd": "0000000000000000",
                    "SigBlk": "0000000000005203",
                    "SigCgt": "0000000180005203",
                    "SigIgn": "0000000000000000",
                    "SigPnd": "0000000000000000",
                    "SigQ": "0/1031575",
                    "Speculation_Store_Bypass": "vulnerable",
                    "State": "S (sleeping)",
                    "Tgid": 120,
                    "Threads": 107,
                    "TracerPid": 0,
                    "Uid": "1049 1049 1049 1049",
                    "VmData": 2378162176,
                    "VmExe": 615395328,
                    "VmHWM": 2478366720,
                    "VmLck": 1227522048,
                    "VmLib": 3420160,
                    "VmPMD": 36864,
                    "VmPTE": 5103616,
                    "VmPeak": 6869864448,
                    "VmPin": 0,
                    "VmRSS": 2008731648,
                    "VmSize": 5799002112,
                    "VmStk": 135168,
                    "VmSwap": 0,
                    "nonvoluntary_ctxt_switches": 6,
                    "voluntary_ctxt_switches": 311
                },
                "timestamp": int(time.time()),
                "total_mem_size": 270458224640,
                "update_docs": 0,
                "uptime": "264942.215617s",
                "used_factors": {}
            },
            "task_status": "FINISHED"
        }
        if physical_host is not None:
            info_server['result']['slot']['properties']['NODE_NAME'] = physical_host

        return info_server

    def rtyserver_task_id(self, date_time=None):
        date_time = date_time if date_time is not None else datetime.today()
        return '{}_{}'.format(datetime.today().strftime('cc_%Y-%m-%dT%H:%M:%S.%fZ'), self.numerify('###############'))

    def help(self, additional_commands=None):
        tid = self.rtyserver_task_id()
        additional_commands = additional_commands if additional_commands is not None else []
        common_commands = [
            'abort', 'check_nprofile', 'clear_data_status', 'delete_file', 'disable_nprofile', 'download_configs_from_dm', 'enable_nprofile', 'execute_script',
            'get_async_command_info', 'get_config', 'get_configs_hashes', 'get_file', 'get_info_server', 'get_metric', 'get_must_be_alive', 'get_queue_perf',
            'get_status', 'help', 'no_file_operations', 'put_file', 'reopenlog', 'reset', 'restart', 'set_config', 'set_queue_opts', 'shutdown', 'stop', 'take_file'
        ]
        this_server_commands = [
            'ban_search', 'check_indexing', 'check_rtsearch', 'check_search', 'clear_index', 'continue_docfetcher', 'create_merger_tasks', 'disable_indexing',
            'disable_rtsearch', 'disable_search', 'do_all_merger_tasks', 'enable_indexing', 'enable_rtsearch', 'enable_search', 'get_cache_policy',
            'get_doc_info', 'get_docfetcher_status', 'get_final_indexes', 'get_histogram', 'get_searchable_timestamp', 'get_state', 'get_timestamp',
            'is_final_index', 'is_repairing', 'pause_docfetcher', 'reduce_control', 'remove_persistent_search_ban', 'reopen_indexes', 'save_config',
            'set_docfetcher_datacenters_status', 'set_docfetcher_timestamp', 'set_its_override', 'set_slot_info', 'set_timestamp', 'synchronizer',
            'wait_empty_indexing_queues'
        ]
        this_server_commands.extend(additional_commands)
        return {
            "command": "help",
            "common commands": ', '.join(common_commands),
            "description": "arguments: item (not required)",
            "id": tid,
            "task_status": "FINISHED",
            "this server commands": ', '.join(this_server_commands)
        }

    def command_result(self, command, status='FINISHED'):
        command_arguments = {
            'help': ['item (not required)', ],
            'shutdown': ['sleep=time', ],
            'restart': ['reread_config=boolean', 'sleep=time', 'stop_timeout=time', ],
            'download_configs_from_dm': ['dm_options=string (json config)', 'slot_info=string (json config)', 'config_version=string', ],
            'get_info_server': ['filter=string (regex are supported)', ]
        }
        if command in command_arguments.keys():
            description = 'arguments: {}'.format(', '.join(command_arguments[command]))
        else:
            description = fake.sentence()
        return {
            'command': command,
            'description': description,
            'id': self.rtyserver_task_id(),
            'task_status': 'FINISHED'
        }
