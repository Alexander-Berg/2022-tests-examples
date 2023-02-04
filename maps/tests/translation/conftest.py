import logging
import os
import pytest

import yatest.common

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf
from maps.garden.sdk.extensions import mutagen

from maps.garden.modules.ymapsdf.lib.geometry_collector import geometry_collector
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.translation import translate as translation


YT_SERVER = "hahn"

logger = logging.getLogger('garden.tasks.translation')


class _Tester:
    def __init__(self, cook, test_name):
        self.cook = cook
        data_test_dir = os.path.join(yatest.common.test_source_path("data"), test_name)
        self.data_test_dir_input = os.path.join(data_test_dir, "input")
        self.data_test_dir_output = os.path.join(data_test_dir, "output")

    def run_test(self):
        geometry_collector.fill_graph(self.cook.input_builder(), ymapsdf.TEST_REGIONS)
        parent_finder.fill_graph(self.cook.input_builder(), ymapsdf.TEST_REGIONS)

        translation.fill_processing_graph_for_region(
            mutagen.create_region_vendor_mutagen(
                self.cook.target_builder(), ymapsdf.TEST_REGION, ymapsdf.TEST_VENDOR))

        object_table_names = ["ad", "ft", "rd"]
        nm_table_names = [base_name + "_nm" for base_name in object_table_names]

        for table_names, stage in [
            (nm_table_names, translation.constants.INPUT_STAGE),
            (object_table_names, translation.constants.OUTPUT_STAGE),
        ]:
            ymapsdf.create_resources(self.cook, stage, table_names, self.data_test_dir_input)

        test_utils.execute(self.cook)

        data_utils.validate_data(
            self.cook.environment_settings,
            YT_SERVER,
            translation.constants.OUTPUT_STAGE,
            ymapsdf.TEST_PROPERTIES,
            self.data_test_dir_output,
            nm_table_names
        )

        ymapsdf_schema.validate(
            self.cook.environment_settings,
            YT_SERVER,
            translation.constants.OUTPUT_STAGE,
            ymapsdf.TEST_PROPERTIES,
            nm_table_names
        )


@pytest.fixture
def translation_tester(cook, request):
    return _Tester(cook, request.function.__name__)
