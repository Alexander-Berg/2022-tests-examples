#
# A console geocoder tests executor.
#

import json
import optparse
import sys

from maps.garden.libs.search_data_validation.geocoder_validator.lib import tester
from maps.garden.libs.search_data_validation.common.test_utils import tests


def _run_tests(testset_path, overlapped_criteria=None):
    searcher = tester.Geocoder("http://addrs-testing.search.yandex.net/geocoder/stable/maps")
    testset = tests.load_testset(testset_path, overlapped_criteria)
    result = tester.run_testset(searcher, testset)

    return result


def main():
    parser = optparse.OptionParser()

    parser.add_option("-t", "--tests", dest="testset_path", help="Input testset file or folder.")
    parser.add_option("-o", "--overlap", dest="overlapped_criteria", help="Overlapped criteria json file.", metavar="FILE")

    (options, args) = parser.parse_args()

    testset_path = options.testset_path

    if not testset_path:
        parser.print_usage()
        sys.exit(1)

    if options.overlapped_criteria:
        overlapped_criteria_name = options.overlapped_criteria
        with open(overlapped_criteria_name) as overlapped_criteria:
            overlapped_criteria = json.load(overlapped_criteria)
            result = _run_tests(testset_path, overlapped_criteria)
    else:
        result = _run_tests(testset_path)

    print(f"{result.name()}:\t Passed: {len(result.passed_tests())}\tFailed: {len(result.failed_tests())}")
    print(result.failed_tests().report_tests())

    if result.failed():
        sys.exit(1)
