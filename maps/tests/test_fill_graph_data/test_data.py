import os
import shutil
import pytest
from maps.garden.sdk.test_utils import ymapsdf, geometry
from maps.garden.modules.altay.lib import altay
from maps.garden.modules.carparks.lib import (fill_graph as carparks, configs,
                                              validation)
from maps.garden.modules.carparks.lib.common import SRC_ALTAY_RESOURCE
from maps.garden.modules.carparks.lib.prices import CARPARKS_PRICES_RESOURCE
from maps.garden.sdk import test_utils as garden_test_utils
from maps.garden.sdk.test_utils.ymapsdf_schema import YmapsdfSchemaManager
from maps.garden.sdk.core import Version
from yatest.common import test_source_path
from yt.wrapper.ypath import ypath_join

from . import test_utils

BASE_ALTAY_YT_PATH = '//home/altay/export'

TEST_DATA_DIR = test_source_path('test_data/')
EXPECTED_DATA_DIR = test_source_path('expected_data/')
WORK_DIR = 'genfiles/tests/'

RELEASE = '20.01.22-1'

MMS_DUMP_RESOURCE = configs.RENDERER_CONFIG.dump_dir_resource_name()
HANDLER_DUMP_RESOURCE = configs.HANDLER_CONFIG.dump_dir_resource_name()

TEST_REGIONS = [
    ('tr', 'yandex'),
    ('cis1', 'yandex')
]

INPUT_TABLES = ['ft', 'ft_geom', 'ft_type', 'source_type', 'ft_source',
                'rd_geom', 'rd_el']


def create_src_altay_out_resource(cook, yt_client, properties):
    yt_client.create('map_node', BASE_ALTAY_YT_PATH,
                     recursive=True, ignore_existing=True)
    resource = cook.create_input_resource(SRC_ALTAY_RESOURCE)
    resource.version = Version(properties=properties)
    resource.server = list(cook.environment_settings['yt_servers'].keys())[0]
    resource.load_environment_settings(cook.environment_settings)
    resource.logged_commit()
    resource.calculate_size()


def create_input_resources(cook, yt_client):
    ymapsdf.create_final_resources_for_many_regions(cook,
                                                    TEST_REGIONS,
                                                    INPUT_TABLES)
    cook.create_build_params_resource(properties={'release_name': RELEASE})
    create_src_altay_out_resource(cook,
                                  yt_client,
                                  properties={
                                      'yt_path': BASE_ALTAY_YT_PATH,
                                      'shipping_date': '20200301'
                                  })


def prepare_ymapsdf(cook, yt_client):
    schema_manager = YmapsdfSchemaManager()
    for region, vendor in TEST_REGIONS:
        for table_name in INPUT_TABLES:
            filepath = os.path.join(TEST_DATA_DIR, region,
                                    table_name + '.jsonl')
            yt_path = ymapsdf.construct_abs_yt_path(
                cook.environment_settings, table_name, region)
            schema = schema_manager.yt_schema_for_sorted_table(table_name)
            yt_client.write_table(yt_path,
                                  geometry.convert_wkt_to_wkb_in_jsonl_file(
                                      filepath,
                                      schema=schema))


def prepare_altay_company_data(yt_client):
    companies_data = test_utils.companies_data(TEST_DATA_DIR)
    yt_path = ypath_join(BASE_ALTAY_YT_PATH, 'snapshot/company')
    yt_client.create('table', yt_path, recursive=True)
    yt_client.write_table(yt_path, companies_data)


def prepare_input_resources(cook, yt_client):
    prepare_ymapsdf(cook, yt_client)
    prepare_altay_company_data(yt_client)


class TestData():

    def setup(self):
        shutil.rmtree(WORK_DIR, ignore_errors=True)
        os.makedirs(WORK_DIR)

    def verify_denormalized_data(self, result_resources):
        carparks_output_resources = {
            res.name: res
            for res in result_resources.values()
            if res and 'carparks' in res.name
            and res.name not in [MMS_DUMP_RESOURCE,
                                 HANDLER_DUMP_RESOURCE,
                                 CARPARKS_PRICES_RESOURCE]
        }
        for region, vendor in TEST_REGIONS:
            region_tables = {name: resource
                             for name, resource
                             in carparks_output_resources.items()
                             if resource.properties['region'] == region}
            test_utils.validate_several_tables(region_tables,
                                               os.path.join(EXPECTED_DATA_DIR,
                                                            region))

    def check_dump_call(self, postgres, schemas):
        test_utils.assert_files_equal(
            os.path.join(EXPECTED_DATA_DIR, 'dump_call'),
            os.path.join(WORK_DIR, 'dump_call'),
            schemas=schemas,
            **postgres)

    def check_place_shields_call(self, postgres, schemas):
        test_utils.assert_files_equal(
            os.path.join(EXPECTED_DATA_DIR, 'place_shields_call'),
            os.path.join(WORK_DIR, 'place_shields_call'),
            sort=True,
            schemas=schemas,
            **postgres)

    @pytest.mark.use_local_yt_yql
    def test(self, environment_settings, yt_client):
        cook = garden_test_utils.GraphCook(environment_settings)
        ymapsdf.fill_graph(cook.input_builder(), TEST_REGIONS)
        altay.fill_graph(cook.input_builder())
        carparks.fill_graph_data(cook.target_builder(), TEST_REGIONS)
        carparks.fill_graph_validation(cook.target_builder(), TEST_REGIONS)

        create_input_resources(cook, yt_client)
        prepare_input_resources(cook, yt_client)
        # Stub, save report to file with name equal to mail_to
        validation._DATA_MAIL_TO = 'data_validation.txt'
        validation._send_report = send_report

        result_resources = garden_test_utils.execute(cook)

        self.verify_denormalized_data(result_resources)

        non_created_resources = [name for name in result_resources
                                 if result_resources[name] is None]
        assert len(non_created_resources) == 0,\
            'Not created resources: ' + str(non_created_resources)

        self.check_send_mail_calls(RELEASE)

    def check_send_mail_calls(self, release):
        test_utils.assert_files_equal(
            os.path.join(EXPECTED_DATA_DIR, validation._DATA_MAIL_TO),
            os.path.join(WORK_DIR, validation._DATA_MAIL_TO),
            release=release)


def send_report(subject, from_mail, to_mail, html, csv):
    with open(os.path.join(WORK_DIR, to_mail), 'a') as f:
        f.write(html + '\n--\n' + csv)
