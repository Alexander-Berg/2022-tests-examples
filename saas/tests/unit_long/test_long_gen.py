from test_common import run_test


def testTEST_DEFAULT_DEADLINE_UNIT(metrics, links):
    test_name = 'TestDefaultDeadline'
    test_pars = 'None'
    timeout = 1200
    run_test(test_name, test_pars, 'TEST_DEFAULT_DEADLINE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_LOGS_UNIT(metrics, links):
    test_name = 'TestLogs'
    test_pars = '-k off'
    timeout = 1200
    run_test(test_name, test_pars, 'TEST_LOGS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_DOCFETCHER_SYNCHONIZATION_UNIT(metrics, links):
    test_name = 'TestSynchronization'
    test_pars = '-k off'
    timeout = 1200
    run_test(test_name, test_pars, 'TEST_RTYSERVER_DOCFETCHER_SYNCHONIZATION_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WHILE_INDEXING_UPDATE_NOSEARCH_UNIT(metrics, links):
    test_name = 'TestMergeWhileIndexingFastUpdate'
    test_pars = '-R'
    timeout = 1200
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WHILE_INDEXING_UPDATE_NOSEARCH_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MERGE_WHILE_INDEXING_UPDATE_UNIT(metrics, links):
    test_name = 'TestMergeWhileIndexingFastUpdate'
    test_pars = ''
    timeout = 1200
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MERGE_WHILE_INDEXING_UPDATE_UNIT', timeout, metrics=metrics, links=links)


def testTEST_STB_UNIT(metrics, links):
    test_name = 'TestStb'
    test_pars = ''
    timeout = 1200
    run_test(test_name, test_pars, 'TEST_STB_UNIT', timeout, metrics=metrics, links=links)
