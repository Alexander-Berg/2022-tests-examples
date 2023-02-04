from test_common import run_test


def testTEST_ALL_FACTORS_UNIT(metrics, links):
    test_name = 'TestAllFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ALL_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_BAD_SNIPPETS_UNIT(metrics, links):
    test_name = 'TestBadSnippets'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_BAD_SNIPPETS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_CACHE_DELETE_QUERY_UNIT(metrics, links):
    test_name = 'TestCacheDeleteQuery'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_CACHE_DELETE_QUERY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_CACHE_SUPPORTER_USAGE_UNIT(metrics, links):
    test_name = 'TestCacheSupporterUsage'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_CACHE_SUPPORTER_USAGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DEADLINE_FOR_DOCS_COUNT_DL_UNIT(metrics, links):
    test_name = 'TestDeadlineForDocsCountByDeadline'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_DEADLINE_FOR_DOCS_COUNT_DL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DEADLINE_FOR_DOCS_COUNT_ONLY_UNIT(metrics, links):
    test_name = 'TestDeadlineForDocsCountOnly'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_DEADLINE_FOR_DOCS_COUNT_ONLY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DEADLINE_FOR_DOCS_COUNT_PR_UNIT(metrics, links):
    test_name = 'TestDeadlineForDocsCountByPruningRank'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_DEADLINE_FOR_DOCS_COUNT_PR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DELEGATE_FUCKUP_UNIT(metrics, links):
    test_name = 'TestDelegateFuckup'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_DELEGATE_FUCKUP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DISABLE_INDEXING_UNIT(metrics, links):
    test_name = 'TestDisableIndexing'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_DISABLE_INDEXING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_EMPTY_INDEXERS_CLOSING_UNIT(metrics, links):
    test_name = 'TestEMPTY_INDEXERS_CLOSING'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_EMPTY_INDEXERS_CLOSING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FA_GROUPING_UNIT(metrics, links):
    test_name = 'TestFullArchiveGrouping'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FA_GROUPING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FREAKS_XML_UNIT(metrics, links):
    test_name = 'TestFreaksML'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FREAKS_XML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_COMPRESSED_REPORT_UNIT(metrics, links):
    test_name = 'TestFullArchiveCompressedReport'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_COMPRESSED_REPORT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_CUSTOM_METASERACH_UNIT(metrics, links):
    test_name = 'TestFullArchiveCustomMetasearch'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_CUSTOM_METASERACH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_DELEGATE_UNIT(metrics, links):
    test_name = 'TestFullArchiveDelegate'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_DELEGATE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_DETACH_UNIT(metrics, links):
    test_name = 'TestFullArchiveDetach'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_DETACH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_EMPTY_KEY_UNIT(metrics, links):
    test_name = 'TestFullArchiveEmptyKey'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_EMPTY_KEY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_EMPTY_TEXT_UNIT(metrics, links):
    test_name = 'TestFullArchiveEmptyText'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_EMPTY_TEXT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_GENERATE_REPLY_CODES_UNIT(metrics, links):
    test_name = 'TestFullArchiveGenerateReplyCodes'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_GENERATE_REPLY_CODES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_GENERATE_UNIT(metrics, links):
    test_name = 'TestFullArchiveGenerate'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_GENERATE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_GTA_ALL_DOC_INFOS_UNIT(metrics, links):
    test_name = 'TestFullArchiveGtaAllDocInfos'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_GTA_ALL_DOC_INFOS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_MODIFY_DOC_REALTIME_UNIT(metrics, links):
    test_name = 'TestFullArchiveRtModify'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_MODIFY_DOC_REALTIME_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_MULTI_KPS_SEARCH_UNIT(metrics, links):
    test_name = 'TestFullArchiveMultiKpsSearch'
    test_pars = '-k on'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_MULTI_KPS_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_NORMAL_REPORT_UNIT(metrics, links):
    test_name = 'TestFullArchiveNormalReport'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_NORMAL_REPORT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_NO_TEXT_SPLIT_UNIT(metrics, links):
    test_name = 'TestFullArchiveNoTextSplit'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_NO_TEXT_SPLIT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_REALTIME_UNIT(metrics, links):
    test_name = 'TestFullArchiveRealtime'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_REALTIME_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FULL_ARCHIVE_REINDEXING_CHECK_METRICS_UNIT(metrics, links):
    test_name = 'TestFullArchiveReindexingCheckMetrics'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FULL_ARCHIVE_REINDEXING_CHECK_METRICS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_GROUP_SORT_BY_COUNT_UNIT(metrics, links):
    test_name = 'TestGROUP_SORT_BY_COUNT'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_GROUP_SORT_BY_COUNT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INCORRECT_BROADCAST_FETCH_UNIT(metrics, links):
    test_name = 'TestIncorrectBroadcastFetch'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INCORRECT_BROADCAST_FETCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEXERS_CLOSING_ONE_BASE_UNIT(metrics, links):
    test_name = 'TestINDEXERS_CLOSING_ONE_BASE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEXERS_CLOSING_ONE_BASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEXER_CONTENT_AFTER_CLOSE_ONE_BASE_UNIT(metrics, links):
    test_name = 'TestINDEXER_CONTENT_AFTER_CLOSE_ONE_BASE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEXER_CONTENT_AFTER_CLOSE_ONE_BASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEXER_DOCS_DBG_LIMIT_UNIT(metrics, links):
    test_name = 'TestIndexerDocsDbgLimit'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEXER_DOCS_DBG_LIMIT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEXER_DOCS_VERSION_UNIT(metrics, links):
    test_name = 'TestINDEXER_DOCS_VERSION'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEXER_DOCS_VERSION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEXER_ORDER_AFTER_CLOSE_ONE_BASE_UNIT(metrics, links):
    test_name = 'TestINDEXER_ORDER_AFTER_CLOSE_ONE_BASE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEXER_ORDER_AFTER_CLOSE_ONE_BASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEX_EMPTY_TEXT_UNIT(metrics, links):
    test_name = 'TestIndexEmptyText'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEX_EMPTY_TEXT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEX_MIN_DATA_CONTENT_UNIT(metrics, links):
    test_name = 'IndexMinDataContent'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEX_MIN_DATA_CONTENT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEX_MIN_DATA_NONCONTENT_UNIT(metrics, links):
    test_name = 'IndexMinDataNonContent'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEX_MIN_DATA_NONCONTENT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEX_STAT_KPS_UNIT(metrics, links):
    test_name = 'TestIndexStatKps'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEX_STAT_KPS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEX_STAT_UNIT(metrics, links):
    test_name = 'TestIndexStat'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INDEX_STAT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INFO_REQUEST_UNIT(metrics, links):
    test_name = 'TestInfoRequest'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_INFO_REQUEST_UNIT', timeout, metrics=metrics, links=links)


def testTEST_JSON_PROPERTIES_XML_ENCODING_UNIT(metrics, links):
    test_name = 'TestJsonPropertiesXMLEncoding'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_JSON_PROPERTIES_XML_ENCODING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_LONG_PROPERTIES_UNIT(metrics, links):
    test_name = 'TestLongProperties'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_LONG_PROPERTIES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MAX_SEGMENTS_UNIT(metrics, links):
    test_name = 'TestMaxSegments'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MAX_SEGMENTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MEMORY_SEARCH_URL_HASH_UNIT(metrics, links):
    test_name = 'TestMemorySearchUrlHash'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MEMORY_SEARCH_URL_HASH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_DOUBLES_SEGMENTS_UNIT(metrics, links):
    test_name = 'TestMergerDoublesOlderSegment'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_DOUBLES_SEGMENTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_DOUBLES_UNIT(metrics, links):
    test_name = 'TestMergerDoubles'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_DOUBLES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_ONE_BASE_UNIT(metrics, links):
    test_name = 'TestMerger_ONE_BASE'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_ONE_BASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_POLICY_CONTINUOUS_UNIT(metrics, links):
    test_name = 'TestMergerPolicyContinuous'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_POLICY_CONTINUOUS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_POLICY_NEW_INDEX_UNIT(metrics, links):
    test_name = 'TestMergerPolicyNewIndex'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_POLICY_NEW_INDEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_SAME_URLS_UNIT(metrics, links):
    test_name = 'TestMergerSameUrls'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_SAME_URLS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGE_MULTIPART_TEXT_ARCHIVE_UNIT(metrics, links):
    test_name = 'TestMergeMultipartTextArchive'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGE_MULTIPART_TEXT_ARCHIVE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGE_ZONES_MAKEUP_UNIT(metrics, links):
    test_name = 'TestMergeZonesMakeup'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGE_ZONES_MAKEUP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_METASEARCH_ZERO_DOCS_IN_GROUP_UNIT(metrics, links):
    test_name = 'TestMetaSearchZeroDocsInGroups'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_METASEARCH_ZERO_DOCS_IN_GROUP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_DEL_MIX_ONE_DOCUMENT_UNIT(metrics, links):
    test_name = 'TestMODIFY_DEL_MIX_ONE_DOCUMENT'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_DEL_MIX_ONE_DOCUMENT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH_MERGER_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH_MERGER'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGED'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_TIMESTAMP_ADD_AFTER_MERGE_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_TIMESTAMP_ADD_AFTER_MERGE'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_TIMESTAMP_ADD_AFTER_MERGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MODIFY_ONE_DOCUMENT_TIMESTAMP_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT_TIMESTAMP'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MODIFY_ONE_DOCUMENT_TIMESTAMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MULTIPART_REPAIR_HDR_UNIT(metrics, links):
    test_name = 'TestMultipartRepairHdr'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MULTIPART_REPAIR_HDR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MULTIPART_REPAIR_META_UNIT(metrics, links):
    test_name = 'TestMultipartRepairMeta'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MULTIPART_REPAIR_META_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MULTIPART_REPAIR_STAGE_UNIT(metrics, links):
    test_name = 'TestMultipartRepairStage'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MULTIPART_REPAIR_STAGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MULTI_KEY_PREFIX_IN_ONE_TOUCH_UNIT(metrics, links):
    test_name = 'TestMultiKeyPrefixInOneTouch'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MULTI_KEY_PREFIX_IN_ONE_TOUCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MULTI_RESTART_UNIT(metrics, links):
    test_name = 'TestMultiRestart'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MULTI_RESTART_UNIT', timeout, metrics=metrics, links=links)


def testTEST_PRUNING_WITHNO_ATTRS_LATER_UNIT(metrics, links):
    test_name = 'TestPruningWithnoAttrsLater'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_PRUNING_WITHNO_ATTRS_LATER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_PRUNING_WITH_FREAK_ATTRS_UNIT(metrics, links):
    test_name = 'TestPruningWithFreakAttrs'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_PRUNING_WITH_FREAK_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_REFINE_RTY_DYNAMIC_FACTORS_UNIT(metrics, links):
    test_name = 'TestRefineRtyDynamicFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_REFINE_RTY_DYNAMIC_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_REJECT_INDEX_LIMIT_EXCEEDED_UNIT(metrics, links):
    test_name = 'TestRejectForFullIndex'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_REJECT_INDEX_LIMIT_EXCEEDED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_REPAIR_DECREMENT_MAX_DOCUMENTS_UNIT(metrics, links):
    test_name = 'TestRepairDecrementMaxDocument'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_REPAIR_DECREMENT_MAX_DOCUMENTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RESTART_BAD_SERVER_UNIT(metrics, links):
    test_name = 'TestRestartBadServer'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RESTART_BAD_SERVER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RESTART_CLEAR_INDEX_UNIT(metrics, links):
    test_name = 'TestClearIndex'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RESTART_CLEAR_INDEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RESTORE_IDENTIFIERS_UNIT(metrics, links):
    test_name = 'TestRestoreIdentifiers'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RESTORE_IDENTIFIERS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_1000_MEM_DOCS_UNIT(metrics, links):
    test_name = 'Test1000MemDocs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_1000_MEM_DOCS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ADD_DOCUMENT_CLOSE_MEMORY_UNIT(metrics, links):
    test_name = 'TestADD_DOCUMENT_CLOSE_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ADD_DOCUMENT_CLOSE_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ADD_DOCUMENT_DISK_UNIT(metrics, links):
    test_name = 'TestADD_DOCUMENT_DISK'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ADD_DOCUMENT_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ADD_DOCUMENT_MEMORY_UNIT(metrics, links):
    test_name = 'TestADD_DOCUMENT_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ADD_DOCUMENT_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ADD_DOCUMENT_REMOVE_FROM_MEMORY_UNIT(metrics, links):
    test_name = 'TestADD_DOCUMENT_REMOVE_FROM_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ADD_DOCUMENT_REMOVE_FROM_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_AUTO_DELEGATION_UNIT(metrics, links):
    test_name = 'TestAutoDelegation'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_AUTO_DELEGATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_BIG_KEY_PREFIX_UNIT(metrics, links):
    test_name = 'TestBigKeyPrefix'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_BIG_KEY_PREFIX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_BIG_RUID_UNIT(metrics, links):
    test_name = 'TestBigRuid'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_BIG_RUID_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_BROKEN_URL_UNIT(metrics, links):
    test_name = 'TestBrokenUrl'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_BROKEN_URL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_AFTER_RESTART_UNIT(metrics, links):
    test_name = 'TestCacheAfterRestart'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_AFTER_RESTART_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_MODIFY_MULTIINDEXES_MCL_UNIT(metrics, links):
    test_name = 'TestCacheLiveTimeSupportModifyMultiIndexes'
    test_pars = '-M -E CacheSupporter -L 100s'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_MODIFY_MULTIINDEXES_MCL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_MODIFY_MULTIINDEXES_ML_UNIT(metrics, links):
    test_name = 'TestCacheLiveTimeSupportModifyMultiIndexes'
    test_pars = '-M -L 100s'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_MODIFY_MULTIINDEXES_ML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_MODIFY_MULTIINDEXES_UNIT(metrics, links):
    test_name = 'TestCacheLiveTimeSupportModifyMultiIndexes'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_MODIFY_MULTIINDEXES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_COLLECT_INFO_STAT_FIELDS_UNIT(metrics, links):
    test_name = 'TestCollectInfoStatFields'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_COLLECT_INFO_STAT_FIELDS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CUSTOM_ZONE_REPAIR_UNIT(metrics, links):
    test_name = 'TestCustomZoneRepair'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CUSTOM_ZONE_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CUSTOM_ZONE_UNIT(metrics, links):
    test_name = 'TestCustomZone'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CUSTOM_ZONE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_FAST_UPDATES_UNIT(metrics, links):
    test_name = 'TestDDKPatchDocsFastUpdates'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_FAST_UPDATES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_FAST_UPDATES_WITH_ZERO_TS_UNIT(metrics, links):
    test_name = 'TestDDKPatchDocsFastUpdatesWithZeroTs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_FAST_UPDATES_WITH_ZERO_TS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_UNIT(metrics, links):
    test_name = 'TestDDKPatchDocsSlowUpdates'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_WITH_ZERO_TS_UNIT(metrics, links):
    test_name = 'TestDDKPatchDocsSlowUpdatesWithZeroTs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DDK_PATCH_DOCS_SLOW_UPDATES_WITH_ZERO_TS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DEFAULT_KPS_UNIT(metrics, links):
    test_name = 'TestDefaultKps'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DEFAULT_KPS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DEFERRED_INDEXATION_BIG_STEP_EMULATOR_UNIT(metrics, links):
    test_name = 'TestDeferredIndexationBigStepEmulator'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DEFERRED_INDEXATION_BIG_STEP_EMULATOR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DEFERRED_INDEXATION_INCREMENT_VERSION_EMULATOR_UNIT(metrics, links):
    test_name = 'TestDeferredIndexationIncrementEmulator'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DEFERRED_INDEXATION_INCREMENT_VERSION_EMULATOR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELAY_SEARCH_UNIT(metrics, links):
    test_name = 'TestDelaySearch'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELAY_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_DISK_F_UNIT(metrics, links):
    test_name = 'TestDeletingDisk'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_DISK_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_DISK_M_UNIT(metrics, links):
    test_name = 'TestDeletingDisk'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_DISK_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_DISK_UNIT(metrics, links):
    test_name = 'TestDeletingDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_KPS_COUNT_UNIT(metrics, links):
    test_name = 'TestDeletingKpsCount'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_KPS_COUNT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_MEMORY_F_UNIT(metrics, links):
    test_name = 'TestDeletingMemory'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_MEMORY_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_MEMORY_M_UNIT(metrics, links):
    test_name = 'TestDeletingMemory'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_MEMORY_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_MEMORY_UNIT(metrics, links):
    test_name = 'TestDeletingMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_QUERY_COUNT_UNIT(metrics, links):
    test_name = 'TestDeletingQueryDelCount'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_QUERY_COUNT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_QUERY_TEMP_UNIT(metrics, links):
    test_name = 'TestDeletingQueryDelTemp'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_QUERY_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_TEMP_F_UNIT(metrics, links):
    test_name = 'TestDeletingTemp'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_TEMP_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_TEMP_M_UNIT(metrics, links):
    test_name = 'TestDeletingTemp'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_TEMP_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DELETING_TEMP_UNIT(metrics, links):
    test_name = 'TestDeletingTemp'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DELETING_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DISABLE_MEMORY_SEARCH_UNIT(metrics, links):
    test_name = 'TestDisableRTSearch'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DISABLE_MEMORY_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DISK_INDEXER_SEARCH_AFTER_CLOSE_UNIT(metrics, links):
    test_name = 'TestDiskIndexerSearchAfterClose'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DISK_INDEXER_SEARCH_AFTER_CLOSE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DISK_INDEXER_SEARCH_MODIFY_UNIT(metrics, links):
    test_name = 'TestDiskIndexerSearchMODIFY'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DISK_INDEXER_SEARCH_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DOC_INFO_ASAMO_UNIT(metrics, links):
    test_name = 'TestDocInfoSourcesLimit'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DOC_INFO_ASAMO_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EMPTY_PROPERTY_UNIT(metrics, links):
    test_name = 'TestEmptyProperty'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_EMPTY_PROPERTY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EMPTY_SEARCH_INDEX_UNIT(metrics, links):
    test_name = 'TestEmptySearchIndex'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_EMPTY_SEARCH_INDEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EXACTMATCHING_DISK_UNIT(metrics, links):
    test_name = 'TestExactMatching_Disk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_EXACTMATCHING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EXACTMATCHING_MEMORY_UNIT(metrics, links):
    test_name = 'TestExactMatching_Memory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_EXACTMATCHING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EXTENDED_FACETS_UNIT(metrics, links):
    test_name = 'TestExtendedFacets'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_EXTENDED_FACETS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FACETS_ALL_UNIT(metrics, links):
    test_name = 'TestFacetsAll'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FACETS_ALL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FACETS_INTERLEAVING_UNIT(metrics, links):
    test_name = 'TestFacetsInterleaving'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FACETS_INTERLEAVING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FACETS_SEMICOLON_UNIT(metrics, links):
    test_name = 'TestFacetsSemicolon'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FACETS_SEMICOLON_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FACET_AND_BAN_CACHE_UNIT(metrics, links):
    test_name = 'TestFacetAndBanCache'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FACET_AND_BAN_CACHE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FACET_AND_BAN_UNIT(metrics, links):
    test_name = 'TestFacetAndBan'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FACET_AND_BAN_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FAST_ARCHIVE_FSGTA_UNIT(metrics, links):
    test_name = 'TestFastArchiveFsgta'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FAST_ARCHIVE_FSGTA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FAST_ARCHIVE_MERGER_UNIT(metrics, links):
    test_name = 'TestFastArchiveMerger'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FAST_ARCHIVE_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_DIFF_KEY_TYPES_UNIT(metrics, links):
    test_name = 'TestFullArchiveDifferentKeyTypes'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FA_DIFF_KEY_TYPES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_EMPTY_KEYS_UNIT(metrics, links):
    test_name = 'TestFullArchiveEmptyKeys'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FA_EMPTY_KEYS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_NO_KEYS_UNIT(metrics, links):
    test_name = 'TestFullArchiveNoKeys'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FA_NO_KEYS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FA_OPTIMIZE_ON_START_UNIT(metrics, links):
    test_name = 'TestFullArchiveOptimizeOnStart'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FA_OPTIMIZE_ON_START_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FILTER_FACTORS_UNIT(metrics, links):
    test_name = 'TestFilterFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FILTER_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FINAL_INDEX_NORMALIZER_UNIT(metrics, links):
    test_name = 'TestFinalIndexNormalizer'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FINAL_INDEX_NORMALIZER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FIND_LONGTEXT_UNIT(metrics, links):
    test_name = 'TestFindLongText'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FIND_LONGTEXT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FIND_RESULTS_INTERNAL_ORDER_GROUPING_DISK_UNIT(metrics, links):
    test_name = 'TestFindResultsInternalOrderGroupingDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FIND_RESULTS_INTERNAL_ORDER_GROUPING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FIND_RESULTS_INTERNAL_ORDER_GROUPING_MEMORY_UNIT(metrics, links):
    test_name = 'TestFindResultsInternalOrderGroupingMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FIND_RESULTS_INTERNAL_ORDER_GROUPING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FIND_RESULTS_ORDER_UNIT(metrics, links):
    test_name = 'TestFindResultsOrder'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FIND_RESULTS_ORDER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FIND_RESULTS_RLV_ORDER_GROUPING_DISK_UNIT(metrics, links):
    test_name = 'TestFindResultsRlvOrderGroupingDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FIND_RESULTS_RLV_ORDER_GROUPING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FIND_RESULTS_RLV_ORDER_GROUPING_MEMORY_UNIT(metrics, links):
    test_name = 'TestFindResultsRlvOrderGroupingMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FIND_RESULTS_RLV_ORDER_GROUPING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FSGTA_FACTORS_UNIT(metrics, links):
    test_name = 'TestFsgtaFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FSGTA_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_DOC_INFO_UNIT(metrics, links):
    test_name = 'TestFullArchiveDocInfo'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_DOC_INFO_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_LOCK_FILES_UNIT(metrics, links):
    test_name = 'TestFullArchiveLockIndexFiles'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_LOCK_FILES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_MERGE_UNIT(metrics, links):
    test_name = 'TestFullArcCompressedExtTwoIndexes'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_MERGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_SINGLE_INDEX_UNIT(metrics, links):
    test_name = 'TestFullArcCompressedExtSingleIndex'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_SINGLE_INDEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_TWO_INDEXES_UNIT(metrics, links):
    test_name = 'TestFullArcCompressedExtTwoIndexes'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_TWO_INDEXES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_MEDIUM_SIZE_DATA_UNIT(metrics, links):
    test_name = 'TestFullArcCompressedExtZstdMediumSizeData'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_MEDIUM_SIZE_DATA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_SMALL_DATA_UNIT(metrics, links):
    test_name = 'TestFullArcCompressedExtZstdSmallData'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARC_COMPRESSED_EXT_ZSTD_SMALL_DATA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GRATTR_FACETS_UNIT(metrics, links):
    test_name = 'TestGroupAttrsFacets'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GRATTR_FACETS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GRATTR_LITERAL_FACETS_UNIT(metrics, links):
    test_name = 'TestGroupAttrsLiteralFacets'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GRATTR_LITERAL_FACETS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_C2N_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_C2N'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_C2N_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_DIFFKEYS_WITH_PRUNE_UNIT(metrics, links):
    test_name = 'TestGROUPPING_DIFFKEYS_WITH_PRUNING'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_DIFFKEYS_WITH_PRUNE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_DISK_UNIT(metrics, links):
    test_name = 'TestGROUPING_DISK'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_MEMORY_UNIT(metrics, links):
    test_name = 'TestGROUPING_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_WITH_PRUNE_UNIT(metrics, links):
    test_name = 'TestGROUPPING_WITH_PRUNING'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_WITH_PRUNE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUPING_ZERO_NUMDOCS_UNIT(metrics, links):
    test_name = 'TestGROUPING_ZERO_NUMDOCS'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUPING_ZERO_NUMDOCS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_DUBLICATE_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_DUPLICATE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_DUBLICATE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_INCORRECT_DISK_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_INCORRECT_DISK'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_INCORRECT_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_INCORRECT_MEMORY_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_INCORRECT_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_INCORRECT_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_INTERVALS_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_INTERVALS'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_INTERVALS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_MERGER_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_MERGER'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_PARTIAL_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_PARTIAL'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_PARTIAL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_WITHNO_DECLARATION_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_WITHNO_DECLARATION'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_WITHNO_DECLARATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTRS_WITH_CACHE_UNIT(metrics, links):
    test_name = 'TestGroupAttrsWithCache'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTRS_WITH_CACHE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_ATTR_ASC_SORT_UNIT(metrics, links):
    test_name = 'TestGROUP_ATTRS_ASC_SORT'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_ATTR_ASC_SORT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_NO_DISK_UNIT(metrics, links):
    test_name = 'TestGROUP_NO_DISK'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_NO_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GROUP_NO_MEMORY_UNIT(metrics, links):
    test_name = 'TestGROUP_NO_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GROUP_NO_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_GTA_UNIT(metrics, links):
    test_name = 'TestGta'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_GTA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_HTML_MULTI_LONG_TITLE_UNIT(metrics, links):
    test_name = 'TestHtmlMultiLongTitle'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_HTML_MULTI_LONG_TITLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_HTML_PARSER_DISK_UNIT(metrics, links):
    test_name = 'TestHtmlParserDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_HTML_PARSER_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_HTML_PARSER_EXTRACTS_ZONES_UNIT(metrics, links):
    test_name = 'TestHtmlParserExtractZones'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_HTML_PARSER_EXTRACTS_ZONES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_HTML_PARSER_GROUP_ATTRS_DISK_UNIT(metrics, links):
    test_name = 'TestHtmlParserGroupAttrsDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_HTML_PARSER_GROUP_ATTRS_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_HTML_PARSER_GROUP_ATTRS_MEMORY_UNIT(metrics, links):
    test_name = 'TestHtmlParserGroupAttrsMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_HTML_PARSER_GROUP_ATTRS_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_HTML_PARSER_MEMORY_UNIT(metrics, links):
    test_name = 'TestHtmlParserMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_HTML_PARSER_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_INCORRECT_FACTORS_UNIT(metrics, links):
    test_name = 'TestIncorrectFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_INCORRECT_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_INCORRECT_TIMEOUT_UNIT(metrics, links):
    test_name = 'TestIncorrectTimeout'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_INCORRECT_TIMEOUT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_INDEXERS_CLOSING_UNIT(metrics, links):
    test_name = 'TestINDEXERS_CLOSING'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_INDEXERS_CLOSING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_INTERNAL_BROADCAST_FETCH_UNIT(metrics, links):
    test_name = 'TestInternalBroadcastFetch'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_INTERNAL_BROADCAST_FETCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_LOAD_OLD_INDEXES_UNIT(metrics, links):
    test_name = 'TestLoadOldIndexes'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_LOAD_OLD_INDEXES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_LONG_URL_UNIT(metrics, links):
    test_name = 'TestLongUrl'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_LONG_URL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGER_MODIFY_F_UNIT(metrics, links):
    test_name = 'TestMergerMODIFY'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGER_MODIFY_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGER_MODIFY_UNIT(metrics, links):
    test_name = 'TestMergerMODIFY'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGER_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGER_REMOVE_ALL_M_UNIT(metrics, links):
    test_name = 'TestMergerREMOVE_ALL'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGER_REMOVE_ALL_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGER_REMOVE_F_UNIT(metrics, links):
    test_name = 'TestMergerREMOVE'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGER_REMOVE_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGER_REMOVE_M_UNIT(metrics, links):
    test_name = 'TestMergerREMOVE'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGER_REMOVE_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGER_REMOVE_UNIT(metrics, links):
    test_name = 'TestMergerREMOVE'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGER_REMOVE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_MANY_SEGMENTS_UNIT(metrics, links):
    test_name = 'TestMergeManySegments'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_MANY_SEGMENTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WHILE_INDEXING_REOPEN_NOSEARCH_UNIT(metrics, links):
    test_name = 'TestMergeWhileIndexingReopen'
    test_pars = '-R'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WHILE_INDEXING_REOPEN_NOSEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WHILE_INDEXING_REOPEN_UNIT(metrics, links):
    test_name = 'TestMergeWhileIndexingReopen'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WHILE_INDEXING_REOPEN_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WITHOUT_ATTRS_UNIT(metrics, links):
    test_name = 'TestMergeWithoutAttrs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WITHOUT_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WITH_ATTRS_UNIT(metrics, links):
    test_name = 'TestMergeWithAttrs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WITH_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WITH_DIFF_ATTRSSET_UNIT(metrics, links):
    test_name = 'TestMergeWithDifferentAttrsSet'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WITH_DIFF_ATTRSSET_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MIX_COMPONENTS_SEARCH_UNIT(metrics, links):
    test_name = 'TestMixComponentsSearch'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MIX_COMPONENTS_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MIX_SHARDING_REMOVE_AND_CHECK_UNIT(metrics, links):
    test_name = 'TestRemoveAndCheck'
    test_pars = '-k on'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MIX_SHARDING_REMOVE_AND_CHECK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MIX_SHARDING_SYNCHRONIZATION_UNIT(metrics, links):
    test_name = 'TestDetachAndSynch'
    test_pars = '-k on'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MIX_SHARDING_SYNCHRONIZATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MODIFY_DOCUMENT_F_UNIT(metrics, links):
    test_name = 'TestMODIFY_DOCUMENT'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MODIFY_DOCUMENT_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MODIFY_DOCUMENT_M_UNIT(metrics, links):
    test_name = 'TestMODIFY_DOCUMENT'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MODIFY_DOCUMENT_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MODIFY_DOCUMENT_UNIT(metrics, links):
    test_name = 'TestMODIFY_DOCUMENT'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MODIFY_DOCUMENT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MODIFY_ONE_DOCUMENT_F_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MODIFY_ONE_DOCUMENT_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MODIFY_ONE_DOCUMENT_M_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MODIFY_ONE_DOCUMENT_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MODIFY_ONE_DOCUMENT_UNIT(metrics, links):
    test_name = 'TestMODIFY_ONE_DOCUMENT'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MODIFY_ONE_DOCUMENT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MONEY_URL_UNIT(metrics, links):
    test_name = 'TestMoneyUrl'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MONEY_URL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULITYTHREAD_DISK_UNIT(metrics, links):
    test_name = 'TestExactMatching_Disk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULITYTHREAD_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTIKEY_PREFIX_LMC_UNIT(metrics, links):
    test_name = 'TestMultiKeyPrefix'
    test_pars = '-L 100s -M -E CacheSupporter'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTIKEY_PREFIX_LMC_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTIKEY_PREFIX_UNIT(metrics, links):
    test_name = 'TestMultiKeyPrefix'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTIKEY_PREFIX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTIKEY_PREFIX_ZONES_AND_ATTRS_UNIT(metrics, links):
    test_name = 'TestMultiKeyPrefixZonesAndAttrs'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTIKEY_PREFIX_ZONES_AND_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTIPLE_ZONE_UNIT(metrics, links):
    test_name = 'TestMultipleZoneValues'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTIPLE_ZONE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTITOKEN_UNIT(metrics, links):
    test_name = 'TestMultitoken'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTITOKEN_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTI_DELETING_DISK_UNIT(metrics, links):
    test_name = 'TestMultiDeletingDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTI_DELETING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTI_DELETING_MEMORY_UNIT(metrics, links):
    test_name = 'TestMultiDeletingMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTI_DELETING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTI_DELETING_TEMP_UNIT(metrics, links):
    test_name = 'TestMultiDeletingTemp'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTI_DELETING_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NEGATIVE_GROUP_ATTRS_UNIT(metrics, links):
    test_name = 'TestNegativeGroupAttrs'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NEGATIVE_GROUP_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NEGATIVE_KEY_PREFIX_UNIT(metrics, links):
    test_name = 'TestNegativeKeyPrefix'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NEGATIVE_KEY_PREFIX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NGRAMMS_SEARCH_UNIT(metrics, links):
    test_name = 'TestNGrammsSearch'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NGRAMMS_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NO_MORPHOLOGY_F_UNIT(metrics, links):
    test_name = 'TestNoMorphology'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NO_MORPHOLOGY_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NO_MORPHOLOGY_M_UNIT(metrics, links):
    test_name = 'TestNoMorphology'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NO_MORPHOLOGY_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NO_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestNoMorphology'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NO_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NUMBERS_UNIT(metrics, links):
    test_name = 'TestNUMBERS'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NUMBERS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NUMBER_OF_FOUND_UNIT(metrics, links):
    test_name = 'TestNumberOfFound'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NUMBER_OF_FOUND_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_OLD_DOC_COUNT_SIGNAL_UNIT(metrics, links):
    test_name = 'TestOldDocCountSignal'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_OLD_DOC_COUNT_SIGNAL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PROPERTIES_FILTER_ONLY_UNIT(metrics, links):
    test_name = 'TestPROPERTIES_FILTER_ONLY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PROPERTIES_FILTER_ONLY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PROPERTIES_MULTIVALUE_NO_TEXT_UNIT(metrics, links):
    test_name = 'TestPROPERTIES_MULTIVALUE_NO_TEXT'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PROPERTIES_MULTIVALUE_NO_TEXT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PROPERTIES_MULTIVALUE_UNIT(metrics, links):
    test_name = 'TestPROPERTIES_MULTIVALUE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PROPERTIES_MULTIVALUE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_MERGE_PORTIONS_UNIT(metrics, links):
    test_name = 'TestPruningMergePortions'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_MERGE_PORTIONS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_PREPARE_WITHNO_MERGE_UNIT(metrics, links):
    test_name = 'TestPRUNING_PREPARE_WITHNO_MERGE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_PREPARE_WITHNO_MERGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_PREPARE_WITH_MERGE_UNIT(metrics, links):
    test_name = 'TestPRUNING_PREPARE_WITH_MERGE'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_PREPARE_WITH_MERGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_SEARCH_MERGE_RESULTS_UNIT(metrics, links):
    test_name = 'TestPruningSearchMergeResults'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_SEARCH_MERGE_RESULTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_QTREE_UNIT(metrics, links):
    test_name = 'TestQTree'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_QTREE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RARE_INDEXING_DISK_UNIT(metrics, links):
    test_name = 'TestRareIndexingDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RARE_INDEXING_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RARE_INDEXING_MEMORY_UNIT(metrics, links):
    test_name = 'TestRareIndexingMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RARE_INDEXING_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REALTIME_DOCUMENTS_LIMIT_UNIT(metrics, links):
    test_name = 'TestRealtimeDocumentsLimit'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REALTIME_DOCUMENTS_LIMIT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REARRANGE_SIM_RANK_THRESHOLD_UNIT(metrics, links):
    test_name = 'TestRearrangeSimRankThreshold'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REARRANGE_SIM_RANK_THRESHOLD_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REFINE_DYNAMIC_FACTOR_UNIT(metrics, links):
    test_name = 'TestRefineDynamicFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REFINE_DYNAMIC_FACTOR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REFINE_STATIC_FACTOR_UNIT(metrics, links):
    test_name = 'TestRefineStaticFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REFINE_STATIC_FACTOR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REMOVE_DOCIDS_BY_NEXT_VERSION_SOURCE_UNIT(metrics, links):
    test_name = 'TestRemoveDocIdsByNextVersionSource'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REMOVE_DOCIDS_BY_NEXT_VERSION_SOURCE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPAIR_DELETING_UNIT(metrics, links):
    test_name = 'TestRepairDeleting'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPAIR_DELETING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPAIR_DISABLED_UNIT(metrics, links):
    test_name = 'TestRepairDisabled'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPAIR_DISABLED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPAIR_STATIC_TEMPS_UNIT(metrics, links):
    test_name = 'TestRepairStaticTemps'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPAIR_STATIC_TEMPS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPEAT_BLOCK_DISC_UNIT(metrics, links):
    test_name = 'TestRepeatBlockDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPEAT_BLOCK_DISC_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPEAT_BLOCK_MEMORY_UNIT(metrics, links):
    test_name = 'TestRepeatBlockMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPEAT_BLOCK_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RESTORE_FAST_ARCHIVE_UNIT(metrics, links):
    test_name = 'TestRestoreFastArchive'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RESTORE_FAST_ARCHIVE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RESTORE_ZONES_UNIT(metrics, links):
    test_name = 'TestRestoreZones'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RESTORE_ZONES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RIGID_STOP_INDEXER_UNIT(metrics, links):
    test_name = 'TestRigidStopIndexer'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RIGID_STOP_INDEXER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RIGIT_STOP_SLOW_UNIT(metrics, links):
    test_name = 'TestRigidStopSlow'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RIGIT_STOP_SLOW_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RIGIT_STOP_UNIT(metrics, links):
    test_name = 'TestRigidStop'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RIGIT_STOP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RUS_TO_ENG_UNIT(metrics, links):
    test_name = 'TestRusToEng'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RUS_TO_ENG_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SAME_URL_DIFF_KPS_DISK_UNIT(metrics, links):
    test_name = 'TestSameUrlDiffKpsDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SAME_URL_DIFF_KPS_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SAME_URL_DIFF_KPS_MEMORY_UNIT(metrics, links):
    test_name = 'TestSameUrlDiffKpsMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SAME_URL_DIFF_KPS_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SAME_URL_DIFF_KPS_TEMP_UNIT(metrics, links):
    test_name = 'TestSameUrlDiffKpsTemp'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SAME_URL_DIFF_KPS_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SAME_URL_DISK_UNIT(metrics, links):
    test_name = 'TestSameUrlDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SAME_URL_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SAME_URL_MEMORY_UNIT(metrics, links):
    test_name = 'TestSameUrlMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SAME_URL_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCHER_COUNT_LIMIT_AFTER_RESTART_UNIT(metrics, links):
    test_name = 'TestSearchersCountLimitAfterRestart'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCHER_COUNT_LIMIT_AFTER_RESTART_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTRS_MIXED_UNIT(metrics, links):
    test_name = 'TestSearchAttrsMixed'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTRS_MIXED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTR_BIG_UNIT(metrics, links):
    test_name = 'TestSearchAttrBig'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTR_BIG_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTR_MULTI_TOKENS_UNIT(metrics, links):
    test_name = 'TestSearchAttrMultiTokens'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTR_MULTI_TOKENS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTR_NAME_CAMEL_CASE_UNIT(metrics, links):
    test_name = 'TestSearchAttrNameCamelCase'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTR_NAME_CAMEL_CASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTR_NEGATIVE_UNIT(metrics, links):
    test_name = 'TestSearchAttrNegative'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTR_NEGATIVE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ATTR_UNIT(metrics, links):
    test_name = 'TestSearchAttr'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ATTR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CACHE_CORRECT_F_UNIT(metrics, links):
    test_name = 'TestSearchCacheCorrect'
    test_pars = '-F $CACHE_DIR'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CACHE_CORRECT_F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CACHE_CORRECT_M_UNIT(metrics, links):
    test_name = 'TestSearchCacheCorrect'
    test_pars = '-M'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CACHE_CORRECT_M_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CACHE_CORRECT_UNIT(metrics, links):
    test_name = 'TestSearchCacheCorrect'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CACHE_CORRECT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CACHE_NO_UNIT(metrics, links):
    test_name = 'TestSearchCacheNo'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CACHE_NO_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CAMEL_CASE_ZONE_NAME_XML_UNIT(metrics, links):
    test_name = 'TestSearchCamelCaseZoneNameXML'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CAMEL_CASE_ZONE_NAME_XML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_IN_SENT_UNIT(metrics, links):
    test_name = 'TestSearchDistanceForAttrsInSent'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_IN_SENT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_UNIT(metrics, links):
    test_name = 'TestSearchDistanceForAttrs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_DISTANCE_FOR_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_FACTORS_EXISTS_UNIT(metrics, links):
    test_name = 'TestSearchFactorsExists'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_FACTORS_EXISTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_FACTORS_PASSAGES_UNIT(metrics, links):
    test_name = 'TestSearchFactorsPassages'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_FACTORS_PASSAGES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_FACTORS_UNIT(metrics, links):
    test_name = 'TestSearchFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_INTERVAL_UNIT(metrics, links):
    test_name = 'TestSearchInterval'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_INTERVAL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_REFINE_UNIT(metrics, links):
    test_name = 'TestSearchRefine'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_REFINE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_ATTRS_UNIT(metrics, links):
    test_name = 'TestSearchSyntaxAttrs'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_ATTRS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_FILTERS_EXACT_UNIT(metrics, links):
    test_name = 'TestSearchSyntaxFiltersExact'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_FILTERS_EXACT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_FREAK_TREE_UNIT(metrics, links):
    test_name = 'TestSearchSyntaxFreakTree'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_FREAK_TREE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_OR_AND_UNIT(metrics, links):
    test_name = 'TestSearchSyntaxOrAnd'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_OR_AND_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_ZONES_MODIFY_UNIT(metrics, links):
    test_name = 'TestSearchSyntaxZonesModify'
    test_pars = '-S'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_ZONES_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_SYNTAX_ZONES_UNIT(metrics, links):
    test_name = 'TestSearchSyntaxZones'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_SYNTAX_ZONES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_TIMEOUT_UNIT(metrics, links):
    test_name = 'TestSearchTimeout'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_TIMEOUT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_TREE_HITS_MARKERS_DISK_ONLY_UNIT(metrics, links):
    test_name = 'TestSearchTreeHitsMarkersDiskOnly'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_TREE_HITS_MARKERS_DISK_ONLY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_TREE_HITS_MARKERS_UNIT(metrics, links):
    test_name = 'TestSearchTreeHitsMarkers'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_TREE_HITS_MARKERS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_UTF8_ATTR_UNIT(metrics, links):
    test_name = 'TestSearchUTF8Attr'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_UTF8_ATTR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SELECT_FORMULA_UNIT(metrics, links):
    test_name = 'TestSelectFormula'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SELECT_FORMULA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SET_SLOT_INFO_UNIT(metrics, links):
    test_name = 'TestSetSlotInfo'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SET_SLOT_INFO_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SIMULTANEOUS_DETACH_UNIT(metrics, links):
    test_name = 'TestSimultaneousDetach'
    test_pars = '-k off'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SIMULTANEOUS_DETACH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SOLID_MULTITOKEN_UNIT(metrics, links):
    test_name = 'TestSolidMultitoken'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SOLID_MULTITOKEN_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SYNONYMS_UNIT(metrics, links):
    test_name = 'TestSynonyms'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SYNONYMS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TAB_IN_PROPERTY_UNIT(metrics, links):
    test_name = 'TestTabInProperty'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TAB_IN_PROPERTY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TERF_TEST_DEFAULT_VALUE_UNIT(metrics, links):
    test_name = 'TErfTestDefaultValues'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TERF_TEST_DEFAULT_VALUE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TERF_TEST_DISK_UNIT(metrics, links):
    test_name = 'TErfTestDisk'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TERF_TEST_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TERF_TEST_MEMORY_UNIT(metrics, links):
    test_name = 'TErfTestMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TERF_TEST_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TERF_TEST_REPAIR_UNIT(metrics, links):
    test_name = 'TErfTestRepair'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TERF_TEST_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TEST_DASH_UNIT(metrics, links):
    test_name = 'TestDash'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TEST_DASH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TIMEOUT_INDEXER_CONNECT_UNIT(metrics, links):
    test_name = 'TestTimeoutIndexerConnect'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TIMEOUT_INDEXER_CONNECT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TIMESTAMP_UNIT(metrics, links):
    test_name = 'TestTimestampSimple'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TIMESTAMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TITLE_CASE_UNIT(metrics, links):
    test_name = 'TestTitleCase'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TITLE_CASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TWO_QUEUES_UNIT(metrics, links):
    test_name = 'TestTwoQueues'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TWO_QUEUES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UNICODE_SYMBOLS_ZONES_CLASH_UNIT(metrics, links):
    test_name = 'TestUnicodeSymbolsZonesClash'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UNICODE_SYMBOLS_ZONES_CLASH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UNICODE_SYMBOLS_ZONES_RECOVERY_CLASH_UNIT(metrics, links):
    test_name = 'TestUnicodeSymbolsZonesRecoveryClash'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UNICODE_SYMBOLS_ZONES_RECOVERY_CLASH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_ARCHIVE_UNIT(metrics, links):
    test_name = 'TestUpdateArchive'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_ARCHIVE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_ADV_REPAIR_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_ADV_REPAIR'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_ADV_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_ADV_TEMP_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_ADV_TEMP'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_ADV_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_DISK_CHANGE_CONFIG_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_DISK_CHANGE_CONFIG'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_DISK_CHANGE_CONFIG_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_DISK_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_DISK'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_MEMORY_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_MEMORY'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_REPAIR_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_REPAIR'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_DOCUMENT_TEMP_UNIT(metrics, links):
    test_name = 'TestUPDATE_DOCUMENT_TEMP'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_DOCUMENT_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_ERF_DISK_UNIT(metrics, links):
    test_name = 'TestUpdateErfFinal'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_ERF_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_ERF_MEMORY_UNIT(metrics, links):
    test_name = 'TestUpdateErfMemory'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_ERF_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_ERF_TEMP_UNIT(metrics, links):
    test_name = 'TestUpdateErfTemp'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_ERF_TEMP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_VERSIONED_DELETE_REALTIME_UNIT(metrics, links):
    test_name = 'TestVersionedDeleteRealtime'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_VERSIONED_DELETE_REALTIME_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_VERSIONING_UNIT(metrics, links):
    test_name = 'TestVersioning'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_VERSIONING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_WILDCARD_UNIT(metrics, links):
    test_name = 'TestWILDCARD'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_WILDCARD_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_WORDSFREQS_UNIT(metrics, links):
    test_name = 'TestWordsFreqs'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_WORDSFREQS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_XML_MULTI_LONG_TITLE_UNIT(metrics, links):
    test_name = 'TestXmlMultiLongTitle'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_XML_MULTI_LONG_TITLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_XML_PARSER_EXTRACTS_ZONES_UNIT(metrics, links):
    test_name = 'TestXmlParserExtractZones'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_XML_PARSER_EXTRACTS_ZONES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_XML_UNIT(metrics, links):
    test_name = 'TestXml'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_XML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SEARCHARC_MULTIPART_NORMALIZATION_UNIT(metrics, links):
    test_name = 'TestSearchArchiveMultipartNormalization'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SEARCHARC_MULTIPART_NORMALIZATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SEARCH_ATTR_FREAKS_UNIT(metrics, links):
    test_name = 'TestSearchAttrFreaks'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SEARCH_ATTR_FREAKS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SEARCH_ROYKSOPP_UNIT(metrics, links):
    test_name = 'TestSearchRoyksopp'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SEARCH_ROYKSOPP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SNIPPETS_UNIT(metrics, links):
    test_name = 'TestSnippets'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SNIPPETS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SYNTAX_EXCEPTION_UNIT(metrics, links):
    test_name = 'TestSyntaxException'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SYNTAX_EXCEPTION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SYNTAX_EXCLUSION_CASES_UNIT(metrics, links):
    test_name = 'TestSyntaxExclusionCases'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SYNTAX_EXCLUSION_CASES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SYNTAX_EXCLUSION_SIMPLE_UNIT(metrics, links):
    test_name = 'TestSyntaxExclusion'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SYNTAX_EXCLUSION_SIMPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SYNTAX_EXCLUSION_XML_UNIT(metrics, links):
    test_name = 'TestSyntaxExclusionXml'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SYNTAX_EXCLUSION_XML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_ALL_DISK_UNIT(metrics, links):
    test_name = 'TestTrieRemoveAllDisk'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_ALL_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_ALL_MEMORY_UNIT(metrics, links):
    test_name = 'TestTrieRemoveAllMemory'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_ALL_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_DOCS_DISK_DISK_UNIT(metrics, links):
    test_name = 'TestTrieRemoveDocsDiskDisk'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_DOCS_DISK_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_DOCS_DISK_UNIT(metrics, links):
    test_name = 'TestTrieRemoveDocsDisk'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_DOCS_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_DOCS_MEMORY_UNIT(metrics, links):
    test_name = 'TestTrieRemoveDocsMemory'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_DOCS_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_KPS_DISK_UNIT(metrics, links):
    test_name = 'TestTrieRemoveKpsDisk'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_KPS_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_REMOVE_KPS_MEMORY_UNIT(metrics, links):
    test_name = 'TestTrieRemoveKpsMemory'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_REMOVE_KPS_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_KEY_PACKED_2_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskComplexKeyPacked2'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_KEY_PACKED_2_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_KEY_PACKED_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskComplexKeyPacked'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_KEY_PACKED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_2_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskComplexLastDimensionUnique2'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_2_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskComplexLastDimensionUnique'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_LAST_DIMENSION_UNIQUE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskComplex'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_COMPLEX_UNSORTED_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskComplexUnsorted'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_COMPLEX_UNSORTED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_PARALLEL_SEARCH_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskParallelSearch'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_PARALLEL_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_SIMPLE_2_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskSimple2'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_SIMPLE_2_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_DISK_SIMPLE_UNIT(metrics, links):
    test_name = 'TestTrieSearchDiskSimple'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_DISK_SIMPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MAX_DOCS_UNIT(metrics, links):
    test_name = 'TestTrieSearchMaxDocs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MAX_DOCS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MEMORY_COMPLEX_KEY_PACKED_UNIT(metrics, links):
    test_name = 'TestTrieSearchMemoryComplexKeyPacked'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MEMORY_COMPLEX_KEY_PACKED_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MEMORY_COMPLEX_UNIT(metrics, links):
    test_name = 'TestTrieSearchMemoryComplex'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MEMORY_COMPLEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_MEMORY_SIMPLE_UNIT(metrics, links):
    test_name = 'TestTrieSearchMemorySimple'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_MEMORY_SIMPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_META_DISK_MEMORY_UNIT(metrics, links):
    test_name = 'TestTrieSearchMetaDiskMemory'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_META_DISK_MEMORY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_META_MAX_DOCS_2_UNIT(metrics, links):
    test_name = 'TestTrieSearchMetaMaxDocs2'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_META_MAX_DOCS_2_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_META_MAX_DOCS_UNIT(metrics, links):
    test_name = 'TestTrieSearchMetaMaxDocs'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_META_MAX_DOCS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TRIE_SEARCH_META_TWO_DISKS_UNIT(metrics, links):
    test_name = 'TestTrieSearchMetaTwoDisks'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TRIE_SEARCH_META_TWO_DISKS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_UNUSED_ZONES_CLEANUP_UNIT(metrics, links):
    test_name = 'TestUnusedZonesCleanup'
    test_pars = ''
    timeout = 240
    run_test(test_name, test_pars, 'TEST_UNUSED_ZONES_CLEANUP_UNIT', timeout, metrics=metrics, links=links)


def testTEST_USER_FACTORS_UNIT(metrics, links):
    test_name = 'TestUserFactors'
    test_pars = 'None'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_USER_FACTORS_UNIT', timeout, metrics=metrics, links=links)

