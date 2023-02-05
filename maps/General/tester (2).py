from maps.search.geocoder.python.lib.geocoder import Geocoder
from maps.search.geocoder.python.lib.response import Response
from maps.garden.libs.search_data_validation.common.test_utils import tests

from time import sleep


class VersionedGeocoder(Geocoder):
    def __init__(self, url, expected_index_version, wait_time):
        super().__init__(url, wait_time=wait_time)
        self._expected_index_version = expected_index_version

    def request(self, request, attempt_count=5, retry_interval=10):
        for i in range(attempt_count):
            response = super().request(request)
            found_version = response.metadata().version

            if not found_version:
                return Response(None, False)

            if found_version == self._expected_index_version:
                return response

            if i + 1 < attempt_count:
                sleep(retry_interval)

        raise RuntimeError("Incorrect index version: {version} instead of {expected}".format(
            version=response.metadata().version,
            expected=self._expected_index_version))


def run_testset(geocoder, testset):
    executed_tests = [
        tests.ExecutedTest(test, geocoder.request(test.query()).first())
            for test in testset
    ]
    return tests.ExecutedTestSet(testset.name(), executed_tests, testset.success_criterion())
