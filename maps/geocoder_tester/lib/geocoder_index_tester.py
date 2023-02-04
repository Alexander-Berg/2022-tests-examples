"""Tasks and resources for geocoder index testing.
Input resource: indexer_index_gzipped:
This package:
1. Runs tests.
2. Reports test results via mail.
3. Stores deploy permission to a geocoder_index_stand_tested resource.
Output resource: geocoder_index_stand_tested
"""

from maps.garden.sdk.core import DataValidationWarning, Task, Demands, Creates
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.extensions import mutagen

from maps.garden.libs.search_data_validation.common.test_utils.tests import load_testset_from_string
from maps.garden.libs.search_data_validation.geocoder_validator.lib import tester
from maps.garden.libs.search_data_validation.modules_lib import constants
from maps.garden.libs.search_data_validation.modules_lib.common import run_tester

from maps.garden.libs.geocoder.common.constants import INDEX_DEPLOYED

GEOCODER_INDEX_TESTED_RESOURCE = "geocoder_index_tested"
SUBJECT_MAIL_TYPE = "index"
RESPONSE_WAITING_TIME = 60 * 15  # seconds == 15 minutes to survive possible network problems


class TestingGeocoder:
    def __init__(self, testing_geocoder):
        self.testing_geocoder = testing_geocoder

        self.subject_mail_type = SUBJECT_MAIL_TYPE

    def run_tests(self, testset_file_name, testset_string):
        overlapped_criteria = None

        testset = load_testset_from_string(testset_string, testset_file_name, overlapped_criteria)
        return tester.run_testset(self.testing_geocoder, testset)


class GeocoderIndexTesterTask(Task):
    '''
    Test index.
    '''
    def __call__(self, stand_is_ready, test_results):
        if constants.ENVIRONMENT == "development":
            test_results.value = constants.TESTS_SUCCEED
            return

        testing_geocoder = tester.VersionedGeocoder(self._test_geocoder_url,
                                                    expected_index_version=stand_is_ready.properties['release'],
                                                    wait_time=RESPONSE_WAITING_TIME)

        searcher = TestingGeocoder(testing_geocoder)

        stable_geocoder = tester.Geocoder(self._stable_geocoder_url, wait_time=RESPONSE_WAITING_TIME)

        success_flag, report, short_report = run_tester(
            searcher,
            stable_geocoder,
            stand_is_ready.properties['release'],
            mail=constants.REPORT_EMAIL
        )

        test_results.value = constants.TESTS_SUCCEED if success_flag else constants.TESTS_FAILED

        if not success_flag:
            raise DataValidationWarning('Auto tests have failed\n' + short_report)

    def load_environment_settings(self, environment_settings):
        self._test_geocoder_url = environment_settings['geocoder_indexer']['validation_stage']['geocoder_url'] + '/maps'
        self._stable_geocoder_url = environment_settings['geocoder_indexer']['release_stage']['geocoder_url'] + '/maps'


@mutagen.propagate_properties("release")
def fill_graph(graph_builder, regions=None):
    # Resources
    graph_builder.add_resource(
        PythonResource(GEOCODER_INDEX_TESTED_RESOURCE,
                       doc="All geocoder tests passed."))

    # Tasks
    graph_builder.add_task(
        Demands(stand_is_ready=INDEX_DEPLOYED.format(stage="validation_stage")),
        Creates(test_results=GEOCODER_INDEX_TESTED_RESOURCE),
        GeocoderIndexTesterTask())
