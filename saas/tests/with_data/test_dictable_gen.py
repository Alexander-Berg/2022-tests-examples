from test_common import run_test


def testTEST_RTYSERVER_ENGLISH_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestEnglishMorphology'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_ENGLISH_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_KAZAKHH_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestKazakhhMorphology'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_KAZAKHH_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_MULTILANG_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestMultiLangMorphology'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_MULTILANG_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_RUSSIAN_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestRussianMorphology'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_RUSSIAN_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TURKISH_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestTurkishMorphology'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TURKISH_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_TURKISH_SYNONYMS_UNIT(metrics, links):
    test_name = 'TestTurkishSynonyms'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_TURKISH_SYNONYMS_UNIT', timeout, metrics=metrics, links=links)


def testTEST_RTYSERVER_UKRAINIAN_MORPHOLOGY_UNIT(metrics, links):
    test_name = 'TestUkrainianMorphology'
    test_pars = '-o $DICT_PATH'
    timeout = 240
    run_test(test_name, test_pars, 'TEST_RTYSERVER_UKRAINIAN_MORPHOLOGY_UNIT', timeout, metrics=metrics, links=links)

