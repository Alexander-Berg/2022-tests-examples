from test import run_cluster_test


def testTEST_DISABLE_INDEXING_UNIT_C(metrics, links):
    test_name = 'TestDisableIndexing'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_DISABLE_INDEXING_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_EMPTY_INDEXERS_CLOSING_UNIT_C(metrics, links):
    test_name = 'TestEMPTY_INDEXERS_CLOSING'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_EMPTY_INDEXERS_CLOSING_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FA_GROUPING_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveGrouping'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FA_GROUPING_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_COMPRESSED_REPORT_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveCompressedReport'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_COMPRESSED_REPORT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_CUSTOM_METASERACH_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveCustomMetasearch'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_CUSTOM_METASERACH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_DELEGATE_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveDelegate'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_DELEGATE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_EMPTY_KEY_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveEmptyKey'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_EMPTY_KEY_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_EMPTY_TEXT_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveEmptyText'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_EMPTY_TEXT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_GENERATE_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveGenerate'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_GENERATE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_GTA_ALL_DOC_INFOS_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveGtaAllDocInfos'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_GTA_ALL_DOC_INFOS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_MODIFY_DOC_REALTIME_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveRtModify'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_MODIFY_DOC_REALTIME_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_MULTI_KPS_SEARCH_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveMultiKpsSearch'
    test_pars = '-k on -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_MULTI_KPS_SEARCH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_NORMAL_REPORT_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveNormalReport'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_NORMAL_REPORT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_NO_TEXT_SPLIT_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveNoTextSplit'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_NO_TEXT_SPLIT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_REALTIME_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveRealtime'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_REALTIME_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_REINDEXING_CHECK_METRICS_PERSISTENT_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveReindexingCheckMetricsPersistent'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_REINDEXING_CHECK_METRICS_PERSISTENT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_INCORRECT_BROADCAST_FETCH_UNIT_C(metrics, links):
    test_name = 'TestIncorrectBroadcastFetch'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_INCORRECT_BROADCAST_FETCH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_JSON_PROPERTIES_XML_ENCODING_UNIT_C(metrics, links):
    test_name = 'TestJsonPropertiesXMLEncoding'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_JSON_PROPERTIES_XML_ENCODING_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_LOGS_UNIT_C(metrics, links):
    test_name = 'TestLogs'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_LOGS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_LONG_PROPERTIES_UNIT_C(metrics, links):
    test_name = 'TestLongProperties'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_LONG_PROPERTIES_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_MERGER_POLICY_CONTINUOUS_UNIT_C(metrics, links):
    test_name = 'TestMergerPolicyContinuous'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_MERGER_POLICY_CONTINUOUS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_MERGER_POLICY_NEW_INDEX_UNIT_C(metrics, links):
    test_name = 'TestMergerPolicyNewIndex'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_MERGER_POLICY_NEW_INDEX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_MERGER_SAME_URLS_UNIT_C(metrics, links):
    test_name = 'TestMergerSameUrls'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_MERGER_SAME_URLS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_MERGE_ZONES_MAKEUP_UNIT_C(metrics, links):
    test_name = 'TestMergeZonesMakeup'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_MERGE_ZONES_MAKEUP_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC_UNIT_C(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RESTART_BAD_SERVER_UNIT_C(metrics, links):
    test_name = 'TestRestartBadServer'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RESTART_BAD_SERVER_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_BIG_KEY_PREFIX_UNIT_C(metrics, links):
    test_name = 'TestBigKeyPrefix'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_BIG_KEY_PREFIX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_BIG_RUID_UNIT_C(metrics, links):
    test_name = 'TestBigRuid'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_BIG_RUID_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_FAST_UPDATES_WITH_ZERO_TS_UNIT_C(metrics, links):
    test_name = 'TestDDKPatchDocsFastUpdatesWithZeroTs'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_FAST_UPDATES_WITH_ZERO_TS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_UNIT_C(metrics, links):
    test_name = 'TestDDKPatchDocsSlowUpdates'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_WITH_ZERO_TS_UNIT_C(metrics, links):
    test_name = 'TestDDKPatchDocsSlowUpdatesWithZeroTs'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_WITH_ZERO_TS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DEFERRED_INDEXATION_BIG_STEP_EMULATOR_UNIT_C(metrics, links):
    test_name = 'TestDeferredIndexationBigStepEmulator'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_DEFERRED_INDEXATION_BIG_STEP_EMULATOR_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELAY_SEARCH_UNIT_C(metrics, links):
    test_name = 'TestDelaySearch'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_DELAY_SEARCH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EMPTY_SEARCH_INDEX_UNIT_C(metrics, links):
    test_name = 'TestEmptySearchIndex'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_EMPTY_SEARCH_INDEX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_DIFF_KEY_TYPES_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveDifferentKeyTypes'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FA_DIFF_KEY_TYPES_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_EMPTY_KEYS_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveEmptyKeys'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FA_EMPTY_KEYS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_NO_KEYS_UNIT_C(metrics, links):
    test_name = 'TestFullArchiveNoKeys'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FA_NO_KEYS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_MERGE_UNIT_C(metrics, links):
    test_name = 'TestFullArcCompressedExtTwoIndexes'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_MERGE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_SINGLE_INDEX_UNIT_C(metrics, links):
    test_name = 'TestFullArcCompressedExtSingleIndex'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_SINGLE_INDEX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_TWO_INDEXES_UNIT_C(metrics, links):
    test_name = 'TestFullArcCompressedExtTwoIndexes'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_TWO_INDEXES_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_MEDIUM_SIZE_DATA_UNIT_C(metrics, links):
    test_name = 'TestFullArcCompressedExtZstdMediumSizeData'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_MEDIUM_SIZE_DATA_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_SMALL_DATA_UNIT_C(metrics, links):
    test_name = 'TestFullArcCompressedExtZstdSmallData'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_SMALL_DATA_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_WITH_PRUNE_UNIT_C(metrics, links):
    test_name = 'TestGROUPPING_WITH_PRUNING'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_WITH_PRUNE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_INCORRECT_TIMEOUT_UNIT_C(metrics, links):
    test_name = 'TestIncorrectTimeout'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_INCORRECT_TIMEOUT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MIX_SHARDING_REMOVE_AND_CHECK_UNIT_C(metrics, links):
    test_name = 'TestRemoveAndCheck'
    test_pars = '-k on -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_MIX_SHARDING_REMOVE_AND_CHECK_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NEGATIVE_KEY_PREFIX_UNIT_C(metrics, links):
    test_name = 'TestNegativeKeyPrefix'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_NEGATIVE_KEY_PREFIX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NGRAMMS_SEARCH_UNIT_C(metrics, links):
    test_name = 'TestNGrammsSearch'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_NGRAMMS_SEARCH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_QTREE_UNIT_C(metrics, links):
    test_name = 'TestQTree'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_QTREE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REARRANGE_SIM_RANK_THRESHOLD_UNIT_C(metrics, links):
    test_name = 'TestRearrangeSimRankThreshold'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_REARRANGE_SIM_RANK_THRESHOLD_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTR_NAME_CAMEL_CASE_UNIT_C(metrics, links):
    test_name = 'TestSearchAttrNameCamelCase'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTR_NAME_CAMEL_CASE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CAMEL_CASE_ZONE_NAME_XML_UNIT_C(metrics, links):
    test_name = 'TestSearchCamelCaseZoneNameXML'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CAMEL_CASE_ZONE_NAME_XML_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_IN_SENT_UNIT_C(metrics, links):
    test_name = 'TestSearchDistanceForAttrsInSent'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_IN_SENT_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_UNIT_C(metrics, links):
    test_name = 'TestSearchDistanceForAttrs'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_FREAK_TREE_UNIT_C(metrics, links):
    test_name = 'TestSearchSyntaxFreakTree'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_FREAK_TREE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SIMULTANEOUS_DETACH_UNIT_C(metrics, links):
    test_name = 'TestSimultaneousDetach'
    test_pars = '-k off -g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_SIMULTANEOUS_DETACH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UNICODE_SYMBOLS_ZONES_CLASH_UNIT_C(metrics, links):
    test_name = 'TestUnicodeSymbolsZonesClash'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_RTYSERVER_UNICODE_SYMBOLS_ZONES_CLASH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_ALL_MEMORY_UNIT_C(metrics, links):
    test_name = 'TestTrieRemoveAllMemory'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_REMOVE_ALL_MEMORY_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_DOCS_DISK_DISK_UNIT_C(metrics, links):
    test_name = 'TestTrieRemoveDocsDiskDisk'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_REMOVE_DOCS_DISK_DISK_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_DOCS_DISK_UNIT_C(metrics, links):
    test_name = 'TestTrieRemoveDocsDisk'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_REMOVE_DOCS_DISK_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_DOCS_MEMORY_UNIT_C(metrics, links):
    test_name = 'TestTrieRemoveDocsMemory'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_REMOVE_DOCS_MEMORY_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_KPS_MEMORY_UNIT_C(metrics, links):
    test_name = 'TestTrieRemoveKpsMemory'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_REMOVE_KPS_MEMORY_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_KEY_PACKED_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskComplexKeyPacked'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_KEY_PACKED_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_2_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskComplexLastDimensionUnique2'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_2_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskComplexLastDimensionUnique'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskComplex'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_UNSORTED_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskComplexUnsorted'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_UNSORTED_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_PARALLEL_SEARCH_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskParallelSearch'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_PARALLEL_SEARCH_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_SIMPLE_2_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskSimple2'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_SIMPLE_2_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_SIMPLE_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchDiskSimple'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_SIMPLE_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MAX_DOCS_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchMaxDocs'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MAX_DOCS_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MEMORY_COMPLEX_KEY_PACKED_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchMemoryComplexKeyPacked'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MEMORY_COMPLEX_KEY_PACKED_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MEMORY_COMPLEX_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchMemoryComplex'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MEMORY_COMPLEX_UNIT_C', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_META_TWO_DISKS_UNIT_C(metrics, links):
    test_name = 'TestTrieSearchMetaTwoDisks'
    test_pars = '-g $CONF_PATH/cluster/cluster_nodm_1be.cfg'
    timeout = 240
    run_cluster_test(test_name, test_pars, 'TEST_TRIE_SEARCH_META_TWO_DISKS_UNIT_C', timeout, metrics=metrics, links=links)

