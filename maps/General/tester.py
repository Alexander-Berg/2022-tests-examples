from maps.garden.libs.search_data_validation.common.test_utils import tests

from . import meta_searcher


def run_tests(testset_path, cache_files, overlapped_criteria=None):
    meta_searcher.set_cache_files(cache_files)
    testset = tests.load_testset(testset_path, overlapped_criteria)

    executed_tests = [
        tests.ExecutedTest(test, meta_searcher.request(test.user_query(), *test.lonlat()))
        for test in testset
    ]

    return tests.ExecutedTestSet(testset.name(), executed_tests, testset.success_criterion())
