from test_common import run_test


def testTEST_DEADLINE_FOR_DOCS_OFFSET_UNIT(metrics, links):
    test_name = 'TestDeadlineForDocsOffset'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_DEADLINE_FOR_DOCS_OFFSET_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DEADLINE_FOR_DOCS_UNIT(metrics, links):
    test_name = 'TestDeadlineForDocs'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_DEADLINE_FOR_DOCS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DEADLINE_FUTURE_UNIT(metrics, links):
    test_name = 'TestFutureDeadline'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_DEADLINE_FUTURE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_DEADLINE_INDEXING_TIME_UNIT(metrics, links):
    test_name = 'TestIndexingTimeDeadline'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_DEADLINE_INDEXING_TIME_UNIT', timeout, metrics=metrics, links=links)


def testTEST_INDEX_DEAD_DOCUMENTS_UNIT(metrics, links):
    test_name = 'TestIndexDeadDocuments'
    test_pars = ''
    timeout = 540
    run_test(test_name, test_pars, 'TEST_INDEX_DEAD_DOCUMENTS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_MCL_UNIT(metrics, links):
    test_name = 'TestCacheLiveTime'
    test_pars = '-M -E CacheSupporter -L 100s'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_MCL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_ML_UNIT(metrics, links):
    test_name = 'TestCacheLiveTime'
    test_pars = '-M -L 100s'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_ML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_SUPPORT_MODIFY_MCL_UNIT(metrics, links):
    test_name = 'TestCacheLiveTimeSupportModify'
    test_pars = '-M -E CacheSupporter -L 100s'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_SUPPORT_MODIFY_MCL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_SUPPORT_MODIFY_ML_UNIT(metrics, links):
    test_name = 'TestCacheLiveTimeSupportModify'
    test_pars = '-M -L 100s'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_SUPPORT_MODIFY_ML_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_SUPPORT_MODIFY_UNIT(metrics, links):
    test_name = 'TestCacheLiveTimeSupportModify'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_SUPPORT_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CACHE_LIVETIME_UNIT(metrics, links):
    test_name = 'TestCacheLiveTime'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CACHE_LIVETIME_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_COLLECT_INFO_RPS_CUSTOM_UNIT(metrics, links):
    test_name = 'TestCollectInfoRPSCustom'
    test_pars = ''
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_COLLECT_INFO_RPS_CUSTOM_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_COLLECT_INFO_RPS_UNIT(metrics, links):
    test_name = 'TestCollectInfoRPS'
    test_pars = ''
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_COLLECT_INFO_RPS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_MERGER_UNIT(metrics, links):
    test_name = 'TestFullArchiveMerger'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MEMORY_LEAK_INDEXING_UNIT(metrics, links):
    test_name = 'TestMemoryLeakIndexing'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MEMORY_LEAK_INDEXING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WHILE_INDEXING_MODIFY_NOSEARCH_UNIT(metrics, links):
    test_name = 'TestMergeWhileIndexingModify'
    test_pars = '-R'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WHILE_INDEXING_MODIFY_NOSEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WHILE_INDEXING_MODIFY_ONE_SOURCE_UNIT(metrics, links):
    test_name = 'TestMergeWhileIndexingModifyOneSource'
    test_pars = ''
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WHILE_INDEXING_MODIFY_ONE_SOURCE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REMOVE_DOCS_PERFORMANCE_UNIT(metrics, links):
    test_name = 'TestRemoveDocsPerformance'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REMOVE_DOCS_PERFORMANCE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPAIR_STATIC_TEMPS_UNIT(metrics, links):
    test_name = 'TestRepairStaticTemps'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPAIR_STATIC_TEMPS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCHER_COUNT_LIMIT_AFTER_RESTART_UNIT(metrics, links):
    test_name = 'TestSearchersCountLimitAfterRestart'
    test_pars = ''
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCHER_COUNT_LIMIT_AFTER_RESTART_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_ARCHIVE_MERGER_UNIT(metrics, links):
    test_name = 'TestMergeSearchArchive'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_ARCHIVE_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CACHE_NO_RESPOND_BASE_LMC_UNIT(metrics, links):
    test_name = 'TestSearchCacheNoRespondBaseSearches'
    test_pars = '-L 100s -M -E CacheSupporter'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CACHE_NO_RESPOND_BASE_LMC_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_CACHE_NO_RESPOND_BASE_UNIT(metrics, links):
    test_name = 'TestSearchCacheNoRespondBaseSearches'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_CACHE_NO_RESPOND_BASE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_XML_PARSER_DISK_UNIT(metrics, links):
    test_name = 'TestXmlParserDisk'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_XML_PARSER_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_XML_PARSER_MEMORY_UNIT(metrics, links):
    test_name = 'TestXmlParserMemory'
    test_pars = 'None'
    timeout = 540
    run_test(test_name, test_pars, 'TEST_RTYSERVER_XML_PARSER_MEMORY_UNIT', timeout, metrics=metrics, links=links)

