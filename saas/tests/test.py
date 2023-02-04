from test_common import run_test


def testTestADD_DOCUMENT_DISK(metrics, links):
    test_name = 'TestADD_DOCUMENT_DISK'
    run_test(test_name, '', test_name+'_sample', metrics=metrics, links=links)


def testTestAllFactors(metrics):
    test_name = 'TestAllFactors'
    run_test(test_name, '', test_name+'_sample', metrics=metrics)
