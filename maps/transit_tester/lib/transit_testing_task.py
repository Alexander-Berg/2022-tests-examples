import json
import logging
import requests
import yt.wrapper as yt

from maps.garden.sdk.core import DataValidationWarning
from maps.garden.sdk.yt import YtTaskBase
from maps.libs.config.py.config import read_config_file
from retry.api import retry
from time import sleep
from yandex.maps.proto.common2 import geo_object_pb2
from yandex.maps.proto.search import transit_pb2


DEFAULT_ATTEMPTS = 3
RETRY_INTERVAL = 1
DELAY_BETWEEN_TESTS = 0.2


class TransitRequester:
    def __init__(self, url):
        self._url = url

    @retry(tries=DEFAULT_ATTEMPTS, delay=RETRY_INTERVAL)
    def _get_and_parse_with_retries(self, params):
        response = requests.get(self._url, params=params)
        response.raise_for_status()

        geo_object = geo_object_pb2.GeoObject()
        geo_object.ParseFromString(response.content)

        routes = []
        for obj in geo_object.geo_object:
            route_metadata = obj.metadata[0].Extensions[transit_pb2.TRANSIT_ROUTE_METADATA]
            routes.append(route_metadata.route_id)
        return routes

    def get_all_routes(self, normalized_query, ll, spn, lang='ru'):
        params = {
            'text': normalized_query,
            'lang': lang,
            'll': ll,
            'spn': spn,
            'origin': 'transit-tester',
            'ms': 'pb'
        }

        try:
            routes = self._get_and_parse_with_retries(params)
        except:
            return None

        return routes


class TransitIndexTesterTask(YtTaskBase):
    def __call__(self, stand_is_ready, test_results):
        config = json.loads(read_config_file('/transit_tester.json'))
        testing_url = config['transit_url']
        stable_url = config['stable_url']
        if not testing_url or not stable_url:
            logging.warn("Service url for test is not defined, skip all tests")
            return

        testsets = config['testsets']
        failed = False
        yt_client = self.get_yt_client('hahn')

        for testset in testsets:
            logging.info(f"Testset {testset['name']}, yt table {testset['yt_table']}")
            tests_count = yt_client.row_count(testset['yt_table'])
            failed_tests = self._run_testset_yt(
                yt_client, testing_url, stable_url, testset['yt_table'])
            logging.info(f"Testset {testset['name']} - "
                         f"total tests: {tests_count}, "
                         f"failed tests: {len(failed_tests)}")
            if failed_tests:
                logging.warn('There are failed tests:\n' + '\n'.join(map(str, failed_tests)))
                if len(failed_tests) / tests_count > testset['fails_threshold']:
                    logging.error(f"Testset {testset['name']} has failed")
                    failed = True
        if failed:
            raise DataValidationWarning('Auto tests have failed')

    def _run_testset_yt(self, yt_client, testing_url, stable_url, yt_table):
        logging.info(f"Comparing testing {testing_url} with stable {stable_url}")
        testing_requester = TransitRequester(testing_url)
        stable_requester = TransitRequester(stable_url)
        failed_tests = []
        for row in yt_client.read_table(yt.TablePath(yt_table,
                                        columns=['normalized_query', 'll', 'spn'])):
            testing_routes = testing_requester.get_all_routes(**row)
            stable_routes = stable_requester.get_all_routes(**row)
            if testing_routes is None or stable_routes is None or \
                    not next(iter(testing_routes), None) == next(iter(stable_routes), None):
                failed_tests.append(row)

            sleep(DELAY_BETWEEN_TESTS)

        return failed_tests
