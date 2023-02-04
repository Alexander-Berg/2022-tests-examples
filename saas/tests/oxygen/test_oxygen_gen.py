from test_common import run_test


def testTEST_OXYGEN_BROKEN_FULLARC_REPAIR_UNIT(metrics, links):
    test_name = 'TestOxygenBrokenFullarcRepair'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_BROKEN_FULLARC_REPAIR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_1000_UNIT(metrics, links):
    test_name = 'TestOxygenDocs1000'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_1000_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_CHECK_AND_FIX_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCheckAndFix'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_CHECK_AND_FIX_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_CORRECT_DOCIDS_FULLDATA_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1PartialTuplesUpdatesCorrectDocidsFullData'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_CORRECT_DOCIDS_FULLDATA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_CORRECT_DOCIDS_PARTICULAR_DATA_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1PartialTuplesUpdatesCorrectDocidsPartialData'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_CORRECT_DOCIDS_PARTICULAR_DATA_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_FAIL_AT_FIRST_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1PartialTuplesUpdatesFailAtFirst'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_FAIL_AT_FIRST_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_FAIL_ONLY_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1PartialTuplesUpdatesFailOnly'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_FAIL_ONLY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1PartialTuplesUpdates'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_PARTIAL_TUPLES_UPDATES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_TUPLES_DOUBLE_DISK_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1TuplesDoubleDisk'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_TUPLES_DOUBLE_DISK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_TUPLES_DOUBLE_FINAL_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1TuplesDoubleFinal'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_TUPLES_DOUBLE_FINAL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE1_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompare1'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE1_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_COMPARE_KEYINV_REFRESH2017_UNIT(metrics, links):
    test_name = 'TestOxygenDocsCompareKeyInvRefresh2017'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_COMPARE_KEYINV_REFRESH2017_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_MANY_UNIT(metrics, links):
    test_name = 'TestOxygenDocsMany'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_MANY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_MERGE1_DEADLINE10_UNIT(metrics, links):
    test_name = 'TestOxygenDocsMerge1Deadline10'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_MERGE1_DEADLINE10_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_MERGE1_UNIT(metrics, links):
    test_name = 'TestOxygenDocsMerge1'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_MERGE1_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_MERGE_UNIT(metrics, links):
    test_name = 'TestOxygenDocsMerge'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_MERGE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_MERGE_WHILE_INDEXING_MODIFY_UNIT(metrics, links):
    test_name = 'TestOxygenDocsMergeWhileIndexingModify'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_MERGE_WHILE_INDEXING_MODIFY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_NO_TUPLES_UNIT(metrics, links):
    test_name = 'TestOxygenDocsNoTuples'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_NO_TUPLES_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_REFRESH2017_FUTURE_UNIT(metrics, links):
    test_name = 'TestOxygenDocsRefresh2017Future'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_REFRESH2017_FUTURE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_REFRESH2017_LQUICK_UNIT(metrics, links):
    test_name = 'TestOxygenDocsRefresh2017Legacy'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_REFRESH2017_LQUICK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_REFRESH2017_SAMOVAR_UNIT(metrics, links):
    test_name = 'TestOxygenDocsRefresh2017Samovar'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_REFRESH2017_SAMOVAR_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_REFRESH2017_SMOKEFACT_UNIT(metrics, links):
    test_name = 'TestOxygenDocsRefresh2017_SmokeFactors'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_REFRESH2017_SMOKEFACT_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_REFRESH2017_UNIT(metrics, links):
    test_name = 'TestOxygenDocsRefresh2017'
    test_pars = '-k off -d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_REFRESH2017_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_DOCS_SIMPLE_UNIT(metrics, links):
    test_name = 'TestOxygenDocsSimple'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_DOCS_SIMPLE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_FAKE_DOC_UNIT(metrics, links):
    test_name = 'TestOxygenFakeDoc'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_FAKE_DOC_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_INCORRECT_DOC_INDEX_FAIL_UNIT(metrics, links):
    test_name = 'TestOxygenIncorrectDocIndexFAIL'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_INCORRECT_DOC_INDEX_FAIL_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_INCORRECT_DOC_INDEX_OK_UNIT(metrics, links):
    test_name = 'TestOxygenIncorrectDocIndexOK'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_INCORRECT_DOC_INDEX_OK_UNIT', timeout, metrics=metrics, links=links)


def testTEST_OXYGEN_MULTIZONE_SIMPLE_UNIT(metrics, links):
    test_name = 'TestOxygenMultizoneSimple'
    test_pars = '-d $TEST_DATA_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_OXYGEN_MULTIZONE_SIMPLE_UNIT', timeout, metrics=metrics, links=links)

