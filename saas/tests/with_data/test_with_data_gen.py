from test_common import run_test


def testTEST_ADVQ_FORMULA_UNIT(metrics, links):
    test_name = 'TestAdvqFormula'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ADVQ_FORMULA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_CONVERT_DATA_FORMAT_UNIT(metrics, links):
    test_name = 'TestAnnConvertDataFormat'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_CONVERT_DATA_FORMAT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_DEFAULT_LANG_UNIT(metrics, links):
    test_name = 'TestAnnDefaultLang'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_DEFAULT_LANG_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_DOC_INFOS_UNIT(metrics, links):
    test_name = 'TestAnnDocInfos'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_DOC_INFOS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_NORMALIZER_UNIT(metrics, links):
    test_name = 'TestAnnNormalizer'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_NORMALIZER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_RECOGNIZER_UNIT(metrics, links):
    test_name = 'TestAnnRecognizer'
    test_pars = '-d $TEST_DATA_PATH -o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_RECOGNIZER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_REGIONS_UNIT(metrics, links):
    test_name = 'TestAnnDifferentRegions'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_REGIONS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_SKIP_UNKNOWN_STREAMS_UNIT(metrics, links):
    test_name = 'TestAnnSkipUnknownStreams'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_SKIP_UNKNOWN_STREAMS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_ANN_TWO_INDEXES_UNIT(metrics, links):
    test_name = 'TestTwoAnnIndexes'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_ANN_TWO_INDEXES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_CTR_ANN_FACTORS_MERGER_UNIT(metrics, links):
    test_name = 'TestAnnFactorsMerge'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_CTR_ANN_FACTORS_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_CTR_ANN_FACTORS_UNIT(metrics, links):
    test_name = 'TestCtrAnnFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_CTR_ANN_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_CTR_ANN_REINDEXING_UNIT(metrics, links):
    test_name = 'TestAnnReindexDoc'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_CTR_ANN_REINDEXING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_EMPTY_ANNOTATIONS_UNIT(metrics, links):
    test_name = 'TestEmptyAnnotations'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_EMPTY_ANNOTATIONS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_FAST_RANK_UNIT(metrics, links):
    test_name = 'TestFastRank'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_FAST_RANK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MERGER_BAD_INDEX_UNIT(metrics, links):
    test_name = 'TestMergerBadIndex'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MERGER_BAD_INDEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_MIX_FACTORS_WIDTH_UNIT(metrics, links):
    test_name = 'TestMixFactorsWidth'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_MIX_FACTORS_WIDTH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ADVANCED_DYNAMIC_FACTORS_UNIT(metrics, links):
    test_name = 'TestAdvancedDynamicFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ADVANCED_DYNAMIC_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CS_FACTORS_BAD_UNIT(metrics, links):
    test_name = 'TestCSBadFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CS_FACTORS_BAD_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CS_FACTORS_SAME_MERGE_UNIT(metrics, links):
    test_name = 'TestCSFactorsSameMerge'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CS_FACTORS_SAME_MERGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_CS_FACTORS_UNIT(metrics, links):
    test_name = 'TestCSFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_CS_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DOC_INFO_UNIT(metrics, links):
    test_name = 'TestDocInfo'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DOC_INFO_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DOUBLE_SYNCHRONIZATION_UNIT(metrics, links):
    test_name = 'TestDoubleSynchronization'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DOUBLE_SYNCHRONIZATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DYNAMIC_FACTORS_UNIT(metrics, links):
    test_name = 'TestDynamicFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DYNAMIC_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_EMPTY_SEARCH_SUGGEST_UNIT(metrics, links):
    test_name = 'TestEmptySearchSuggest'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_EMPTY_SEARCH_SUGGEST_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ERF_GTA_UNIT(metrics, links):
    test_name = 'TestErfGta'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ERF_GTA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FRESHNESS_UNIT(metrics, links):
    test_name = 'TestFreshFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FRESHNESS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_REPAIR_UNIT(metrics, links):
    test_name = 'TestFullArchiveRepair'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_REPAIR_WITH_EMPTY_PART_UNIT(metrics, links):
    test_name = 'TestFullArchiveRepairArchiveWithEmptyPart'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_REPAIR_WITH_EMPTY_PART_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_SIMPLE_UNIT(metrics, links):
    test_name = 'TestFullArchiveSimple'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_SIMPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_FULL_ARCHIVE_VERSION_CHANGE_UNIT(metrics, links):
    test_name = 'TestFullArchiveVersionChanges'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_FULL_ARCHIVE_VERSION_CHANGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MANY_ZONES_FACTORS_BM25F_UNIT(metrics, links):
    test_name = 'TestManyZonesFactorsBM25F'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MANY_ZONES_FACTORS_BM25F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MIX_FACTORS_REPAIR_UNIT(metrics, links):
    test_name = 'TestMixFactorsRepair'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MIX_FACTORS_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MIX_FACTORS_UNIT(metrics, links):
    test_name = 'TestMixFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MIX_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_NORMALIZE_QS_UNIT(metrics, links):
    test_name = 'TestQSFactorsNormalize'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_NORMALIZE_QS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_SEARCH_TM_UNIT(metrics, links):
    test_name = 'TestPruningSearchTm'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_SEARCH_TM_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_SEARCH_UNIT(metrics, links):
    test_name = 'TestPruningSearch'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_STAT_FACTORS_DEFERRED_UPDATE_UNIT(metrics, links):
    test_name = 'TestPruningByStatFormulaDeferredUpdate'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_STAT_FACTORS_DEFERRED_UPDATE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_PRUNING_STAT_FACTORS_UNIT(metrics, links):
    test_name = 'TestPruningByStatFormula'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_PRUNING_STAT_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_QS_FACTORS_QSCGI_UNIT(metrics, links):
    test_name = 'TestQSFactorsQSCgi'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_QS_FACTORS_QSCGI_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_QS_FACTORS_UNIT(metrics, links):
    test_name = 'TestQSFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_QS_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_QUORUM_FORMULA_UNIT(metrics, links):
    test_name = 'TestSearchQuorumFormula'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_QUORUM_FORMULA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RELEV_FORMULA_CHECK_MATRIXNET_UNIT(metrics, links):
    test_name = 'TestRelevFormulaCheckMatrixNet'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RELEV_FORMULA_CHECK_MATRIXNET_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RELEV_FORMULA_WITH_MATRIXNET_UNIT(metrics, links):
    test_name = 'TestRelevFormulaWithMatrixNet'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RELEV_FORMULA_WITH_MATRIXNET_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REMOVE_SYNCHRONIZATION_UNIT(metrics, links):
    test_name = 'TestRemoveSynchronization'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REMOVE_SYNCHRONIZATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPAIR_CS_UNIT(metrics, links):
    test_name = 'TestCSFactorsRepair'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPAIR_CS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_REPAIR_QS_UNIT(metrics, links):
    test_name = 'TestQSFactorsRepair'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_REPAIR_QS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RESTORE_CS_UNIT(metrics, links):
    test_name = 'TestCSFactorsRestore'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RESTORE_CS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RESTORE_QS_UNIT(metrics, links):
    test_name = 'TestQSFactorsRestore'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RESTORE_QS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_FACTORS_MODIFY_CONFIG_UNIT(metrics, links):
    test_name = 'TestSearchFactorsModifyConfig'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_FACTORS_MODIFY_CONFIG_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SEARCH_WITH_URL_HASH_UNIT(metrics, links):
    test_name = 'TestSearchWithUrlHash'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SEARCH_WITH_URL_HASH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SET_MISSING_USER_FACTORS_UNIT(metrics, links):
    test_name = 'TestSetMissingUserFactors'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SET_MISSING_USER_FACTORS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SIMPLE_SYNCHRONIZATION_DEFAULT_SPEED_LIMIT_UNIT(metrics, links):
    test_name = 'TestSimpleSynchronizationDefaultSpeedLimit'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SIMPLE_SYNCHRONIZATION_DEFAULT_SPEED_LIMIT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SIMPLE_SYNCHRONIZATION_GLOBAL_SPEED_LIMIT_UNIT(metrics, links):
    test_name = 'TestSimpleSynchronizationGlobalSpeedLimit'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SIMPLE_SYNCHRONIZATION_GLOBAL_SPEED_LIMIT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SIMPLE_SYNCHRONIZATION_SYNC_SPEED_LIMIT_OVERRIDE_ZERO_UNIT(metrics, links):
    test_name = 'TestSimpleSynchronizationSyncSpeedLimitOverrideZero'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SIMPLE_SYNCHRONIZATION_SYNC_SPEED_LIMIT_OVERRIDE_ZERO_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SORT_BY_UNIT(metrics, links):
    test_name = 'TestSearchFactorsSortBy'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SORT_BY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_BUILD_INDEX_UNIT(metrics, links):
    test_name = 'TestSuggestBuildIndex'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_BUILD_INDEX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_CUSTOM_ZONES_UNIT(metrics, links):
    test_name = 'TestSuggestCustomZones'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_CUSTOM_ZONES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_DELETE_UNIT(metrics, links):
    test_name = 'TestSuggestDelete'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_DELETE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_DISK_SEARCH_UNIT(metrics, links):
    test_name = 'TestSuggestDiskSearch'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_DISK_SEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_FILTER_UNIT(metrics, links):
    test_name = 'TestSuggestFilter'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_FILTER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_FREAK_UNIT(metrics, links):
    test_name = 'TestSuggestFreaks'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_FREAK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_BLACKLIST_UNIT(metrics, links):
    test_name = 'TestSuggestBlacklist'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_BLACKLIST_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_LENGTH_UNIT(metrics, links):
    test_name = 'TestSuggestLength'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_LENGTH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_MANY_KPSS_RESTART_UNIT(metrics, links):
    test_name = 'TestSuggestManyKpssRestart'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_MANY_KPSS_RESTART_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_MANY_WORDS_UNIT(metrics, links):
    test_name = 'TestSuggestManyWords'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_MANY_WORDS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_MODIFY_UNIT(metrics, links):
    test_name = 'TestSuggestModify'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_MULTI_TOKEN_UNIT(metrics, links):
    test_name = 'TestSuggestMultiToken'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_MULTI_TOKEN_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_NUMBERS_UNIT(metrics, links):
    test_name = 'TestSuggestNumbers'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_NUMBERS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_RANKING_UNIT(metrics, links):
    test_name = 'TestSuggestRanking'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_RANKING_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_REPAIR_DDK_UNIT(metrics, links):
    test_name = 'TestSuggestRepairDDK'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_REPAIR_DDK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_REPAIR_ERF_UNIT(metrics, links):
    test_name = 'TestSuggestRepairErf'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_REPAIR_ERF_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_REPAIR_UNIT(metrics, links):
    test_name = 'TestSuggestRepair'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_SELF_MODIFY_UNIT(metrics, links):
    test_name = 'TestSuggestSelfModify'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_SELF_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_SIMPLE_REMOVE_UNIT(metrics, links):
    test_name = 'TestSuggestSimpleRemove'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_SIMPLE_REMOVE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_SUGGEST_SIMPLE_UNIT(metrics, links):
    test_name = 'TestSuggestSimple'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_SUGGEST_SIMPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_CS_DISK_UNIT(metrics, links):
    test_name = 'TestCSFactorsUpdateDisk'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_CS_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_CS_MEM_UNIT(metrics, links):
    test_name = 'TestCSFactorsUpdateMem'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_CS_MEM_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UPDATE_QS_UNIT(metrics, links):
    test_name = 'TestQSFactorsUpdate'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UPDATE_QS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ZONES_MAKEUP_MERGER_UNIT(metrics, links):
    test_name = 'TestZonesMakeupMerger'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ZONES_MAKEUP_MERGER_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ZONES_MAKEUP_PEOPLE_UNIT(metrics, links):
    test_name = 'TestZoneFactorsPeoples'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ZONES_MAKEUP_PEOPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ZONE_FACTORS_BM25F_UNIT(metrics, links):
    test_name = 'TestZoneFactorsBM25F'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ZONE_FACTORS_BM25F_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ZONE_FACTORS_CM_UNIT(metrics, links):
    test_name = 'TestZoneFactorsCoordinationMatch'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ZONE_FACTORS_CM_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ZONE_FACTORS_CZL_UNIT(metrics, links):
    test_name = 'TestZoneFactorsCZL'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ZONE_FACTORS_CZL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_ZONE_FACTORS_ZL_UNIT(metrics, links):
    test_name = 'TestZoneFactorsZL'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ZONE_FACTORS_ZL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SEARCH_BIG_DOCNUM_UNIT(metrics, links):
    test_name = 'TestSearchBigDocnum'
    test_pars = '-k on -s 1 -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SEARCH_BIG_DOCNUM_UNIT', timeout, metrics=metrics, links=links)


def testTEST_SUGGEST_DISK_SEARCH_BY_FLAG_ONLY_UNIT(metrics, links):
    test_name = 'TestSuggestDiskSearchByFlagOnly'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_SUGGEST_DISK_SEARCH_BY_FLAG_ONLY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_TURN_ON_ANNOTATIONS_UNIT(metrics, links):
    test_name = 'TestTurnOnAnnotations'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_TURN_ON_ANNOTATIONS_UNIT', timeout, metrics=metrics, links=links)

