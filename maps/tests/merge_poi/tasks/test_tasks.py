import copy
import pytest

import yatest.common

from maps.garden.sdk.core import Version

from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf

from maps.garden.modules.altay import defs as altay
from maps.garden.modules.ymapsdf.lib.merge_poi import constants

from maps.garden.modules.ymapsdf.lib.merge_poi import fill_ft_poi_attr_task
from maps.garden.modules.ymapsdf.lib.merge_poi import make_ft_altay_companies_task
from maps.garden.modules.ymapsdf.lib.merge_poi import make_ft_altay_names_task
from maps.garden.modules.ymapsdf.lib.merge_poi import make_ft_permalink_nmaps_task
from maps.garden.modules.ymapsdf.lib.merge_poi import make_ft_metadata_task
from maps.garden.modules.ymapsdf.lib.merge_poi import make_ft_permalink_task
from maps.garden.modules.ymapsdf.lib.merge_poi import import_extra_poi_task
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_ft_nm_task
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_ft_center_task
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_ft_source_task
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_ft_task
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_node_task

from . import helpers

_YT_SERVER = 'hahn'


class ResourcesCreator:
    def __init__(self, environment_settings):
        self._environment_settings = environment_settings
        self._cook = helpers.create_cook(environment_settings)
        self._schema_manager = ymapsdf_schema.YmapsdfSchemaManager()
        self._input_data_path = yatest.common.test_source_path("tasks_data/input")
        self._tmp_data_path = yatest.common.test_source_path("tasks_data/tmp")
        self._output_data_path = yatest.common.test_source_path("tasks_data/output")

    def make_altay_table(self, table_name):
        resource = self._cook.input_builder().make_resource(table_name)
        return self._fill_resource(resource, self._tmp_data_path, table_name)

    def make_input_table(self, table_name):
        resource_name = constants.INPUT_STAGE.resource_name(
            table_name,
            ymapsdf.TEST_REGION,
            ymapsdf.TEST_VENDOR)
        resource = self._cook.create_input_resource(resource_name)
        resource.set_schema(self._schema_manager.yt_schema_for_sorted_table(table_name))
        return self._fill_resource(resource, self._input_data_path, table_name)

    def make_temp_table(self, table_name, schema=None, schema_from=None):
        resource_name = constants.OUTPUT_STAGE.resource_name(
            table_name,
            ymapsdf.TEST_REGION,
            ymapsdf.TEST_VENDOR)
        resource = self._cook.target_builder().make_resource(resource_name)
        if schema:
            resource.set_schema(schema)
        if schema_from:
            resource.set_schema(self._schema_manager.yt_schema_for_sorted_table(schema_from))
        return self._fill_resource(resource, self._tmp_data_path, table_name)

    def make_output_table(self, table_name):
        resource_name = constants.OUTPUT_STAGE.resource_name(
            table_name,
            ymapsdf.TEST_REGION,
            ymapsdf.TEST_VENDOR)
        resource = self._cook.target_builder().make_resource(resource_name)
        resource.version = Version(properties=ymapsdf.TEST_PROPERTIES)
        resource.load_environment_settings(self._environment_settings)
        return resource

    def _fill_resource(self, resource, data_dir, table_name):
        resource.server = next(iter(self._environment_settings["yt_servers"].keys()))
        resource.version = Version(properties=ymapsdf.TEST_PROPERTIES)
        resource.load_environment_settings(self._environment_settings)
        data_utils.populate_resource_with_data(resource, data_dir, table_name)
        resource.logged_commit()
        resource.calculate_size()
        return resource

    def read_expected_output_data(self, table_name):
        return data_utils.read_table_from_file(self._output_data_path, table_name)

    def read_expected_tmp_data(self, table_name):
        return data_utils.read_table_from_file(self._tmp_data_path, table_name)


@pytest.fixture
def resources_creator(environment_settings):
    return ResourcesCreator(environment_settings)


@pytest.mark.use_local_yt_yql
def test_make_ft_permalink_nmaps_task(environment_settings, resources_creator):
    ft_permalink_nmaps_tmp = resources_creator.make_output_table(
        "ft_permalink_nmaps_tmp")

    task = make_ft_permalink_nmaps_task.MakeFtPermalinkNmapsTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_source_in=resources_creator.make_input_table("ft_source"),
        altay_duplicates=resources_creator.make_altay_table(altay.DUPLICATES),
        ft_permalink_nmaps_tmp=ft_permalink_nmaps_tmp)

    result = list(ft_permalink_nmaps_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_permalink_nmaps_tmp")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_make_ft_metadata_task(environment_settings, resources_creator):
    ft_metadata_tmp = resources_creator.make_output_table(
        "ft_metadata_tmp")

    task = make_ft_metadata_task.MakeFtMetadataTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_in=resources_creator.make_input_table("ft"),
        ft_source_in=resources_creator.make_input_table("ft_source"),
        altay_companies_unknown=resources_creator.make_altay_table(altay.COMPANIES_UNKNOWN),
        ft_metadata_tmp=ft_metadata_tmp)

    result = list(ft_metadata_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_metadata_tmp")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_make_ft_permalink_task(environment_settings, resources_creator):
    ft_permalink_tmp = resources_creator.make_output_table(
        "ft_permalink_tmp")

    task = make_ft_permalink_task.MakeFtPermalinkTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_metadata_tmp=resources_creator.make_temp_table("ft_metadata_tmp"),
        ft_permalink_nmaps_tmp=resources_creator.make_temp_table("ft_permalink_nmaps_tmp"),
        altay_references=resources_creator.make_altay_table(altay.REFERENCES),
        ft_permalink_tmp=ft_permalink_tmp)

    result = list(ft_permalink_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_permalink_tmp")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_make_ft_altay_companies_task(environment_settings, resources_creator):
    ft_altay_companies_tmp = resources_creator.make_output_table("ft_altay_companies_tmp")

    task = make_ft_altay_companies_task.MakeFtAltayCompaniesTask()
    task.load_environment_settings(environment_settings)
    task(
        altay_companies=resources_creator.make_altay_table(altay.COMPANIES),
        ft_permalink_tmp=resources_creator.make_temp_table("ft_permalink_tmp"),
        ft_altay_companies_tmp=ft_altay_companies_tmp)

    result = list(ft_altay_companies_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_altay_companies_tmp")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_make_ft_altay_names_task(environment_settings, resources_creator):
    ft_altay_names_tmp = resources_creator.make_output_table("ft_altay_names_tmp")

    task = make_ft_altay_names_task.MakeFtAltayNamesTask()
    task.load_environment_settings(environment_settings)
    task(
        altay_names=resources_creator.make_altay_table(altay.NAMES),
        ft_permalink_tmp=resources_creator.make_temp_table("ft_permalink_tmp"),
        ft_altay_names_tmp=ft_altay_names_tmp)

    result = list(ft_altay_names_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_altay_names_tmp")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_prepare_extra_pois_task(environment_settings, resources_creator):
    extra_poi_with_altay_heads = resources_creator.make_output_table(
        "extra_poi_with_altay_heads")

    task = import_extra_poi_task.PrepareExtraPoiTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_type_in=resources_creator.make_input_table("ft_type"),
        altay_duplicates=resources_creator.make_altay_table(altay.DUPLICATES),
        altay_references=resources_creator.make_altay_table(altay.REFERENCES),
        extra_poi_filtered_by_region=resources_creator.make_temp_table("extra_poi_filtered_by_region"),
        extra_poi_with_altay_heads=extra_poi_with_altay_heads)

    result = list(extra_poi_with_altay_heads.read_table())
    expected = resources_creator.read_expected_tmp_data("extra_poi_with_altay_heads")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_import_extra_pois_task(environment_settings, resources_creator):
    extra_poi_tmp = resources_creator.make_output_table(
        "extra_poi_tmp")
    extra_poi_update_tmp = resources_creator.make_output_table(
        "extra_poi_update_tmp")
    extra_poi_names_tmp = resources_creator.make_output_table(
        "extra_poi_names_tmp")
    extra_poi_geoproduct_tmp = resources_creator.make_output_table(
        "extra_poi_geoproduct_tmp")

    task = import_extra_poi_task.ImportExtraPoisTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_in=resources_creator.make_input_table("ft"),
        node_in=resources_creator.make_input_table("node"),
        ft_permalink_tmp=resources_creator.make_temp_table("ft_permalink_tmp"),
        extra_poi_with_altay_heads=resources_creator.make_temp_table("extra_poi_with_altay_heads"),
        extra_poi_tmp=extra_poi_tmp,
        extra_poi_update_tmp=extra_poi_update_tmp,
        extra_poi_names_tmp=extra_poi_names_tmp,
        extra_poi_geoproduct_tmp=extra_poi_geoproduct_tmp,
    )

    result = list(extra_poi_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("extra_poi_tmp")
    assert result == expected

    result = sorted(extra_poi_update_tmp.read_table(), key=lambda row: row["ft_id"])
    expected = resources_creator.read_expected_tmp_data("extra_poi_update_tmp")
    assert result == expected

    result = list(extra_poi_names_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("extra_poi_names_tmp")
    assert result == expected

    result = list(extra_poi_geoproduct_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("extra_poi_geoproduct_tmp")

    def sort_func(d):
        return d["ft_id"], d["source_id"]

    # `extra_poi_geoproduct_tmp` is produced unsorted
    assert sorted(result, key=sort_func) == sorted(expected, key=sort_func)


@pytest.mark.use_local_yt_yql
def test_transform_extra_poi_to_ft_task(environment_settings, resources_creator):
    extra_poi_ft = resources_creator.make_output_table(
        "extra_poi_ft")

    task = merge_ft_task.TransformExtraPoiToFtTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_in=resources_creator.make_input_table("ft"),
        altay_companies=resources_creator.make_altay_table(altay.COMPANIES),
        extra_poi_tmp=resources_creator.make_temp_table("extra_poi_tmp"),
        extra_poi_ft=extra_poi_ft)

    result = list(extra_poi_ft.read_table())
    expected = resources_creator.read_expected_tmp_data("extra_poi_ft")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_transform_extra_poi_to_ft_nm_task(environment_settings, resources_creator):
    extra_poi_ft_nm = resources_creator.make_output_table(
        "extra_poi_ft_nm")

    task = merge_ft_nm_task.TransformExtraPoiNamesToFtNmTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_nm_in=resources_creator.make_input_table("ft_nm"),
        extra_poi_names_tmp=resources_creator.make_temp_table("extra_poi_names_tmp"),
        altay_names=resources_creator.make_altay_table(altay.NAMES),
        extra_poi_ft_nm=extra_poi_ft_nm)

    result = list(extra_poi_ft_nm.read_table())
    expected = resources_creator.read_expected_tmp_data("extra_poi_ft_nm")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_merge_node_task(environment_settings, resources_creator):
    helpers.create_ft_position_from_nmaps_table(environment_settings, fill_table=True)
    node_out = resources_creator.make_output_table("node")

    task = merge_node_task.MergeNodeUsingExtraPOIs()
    task.load_environment_settings(environment_settings)
    task(
        node_in=resources_creator.make_input_table("node"),
        ft_center_in=resources_creator.make_input_table("ft_center"),
        ft_altay_companies_tmp=resources_creator.make_temp_table("ft_altay_companies_tmp"),
        extra_poi_tmp=resources_creator.make_temp_table("extra_poi_tmp"),
        node_out=node_out)

    result = list(node_out.read_table())
    expected = resources_creator.read_expected_output_data("node")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_merge_ft_with_sprav_task(environment_settings, resources_creator):
    ft_with_sprav_tmp = resources_creator.make_output_table("ft_with_sprav_tmp")

    extra_poi_update_schema = copy.deepcopy(constants.EXTRA_POI_UPDATE_TMP_SCHEMA)
    extra_poi_update_schema[0]["sort_order"] = "ascending"

    task = merge_ft_task.MergeFtWithSprav()
    task.load_environment_settings(environment_settings)
    task(
        ft_in=resources_creator.make_input_table("ft"),
        ft_poi_attr_in=resources_creator.make_input_table("ft_poi_attr"),
        ft_altay_companies_tmp=resources_creator.make_temp_table("ft_altay_companies_tmp"),
        extra_poi_update_tmp=resources_creator.make_temp_table("extra_poi_update_tmp", schema=extra_poi_update_schema),
        ft_with_sprav_tmp=ft_with_sprav_tmp)

    result = list(ft_with_sprav_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_with_sprav_tmp")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_merge_ft_with_extra_poi_task(environment_settings, resources_creator):
    ft_out = resources_creator.make_output_table("ft")

    task = merge_ft_task.MergeFtWithExtraPoiTask()
    task.load_environment_settings(environment_settings)
    task(
        extra_poi_ft=resources_creator.make_temp_table("extra_poi_ft", schema_from="ft"),
        ft_with_sprav_tmp=resources_creator.make_temp_table("ft_with_sprav_tmp", schema_from="ft"),
        ft_merged_tmp_out=ft_out)

    result = list(ft_out.read_table())
    expected = resources_creator.read_expected_output_data("ft")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_merge_ft_source_task(environment_settings, resources_creator):
    ft_source_out = resources_creator.make_output_table("ft_source")

    properties = copy.copy(ymapsdf.TEST_PROPERTIES)
    properties["extra_poi_release_name"] = helpers.EXTRA_POI_BUNDLE_RELEASE_NAME

    extra_poi_tmp = resources_creator.make_temp_table("extra_poi_tmp")
    extra_poi_tmp.version = Version(properties=properties)

    extra_poi_geoproduct_tmp = resources_creator.make_temp_table("extra_poi_geoproduct_tmp")
    extra_poi_geoproduct_tmp.version = Version(properties=properties)

    task = merge_ft_source_task.MergeFtSourceUsingExtraPOIs()
    task.load_environment_settings(environment_settings)
    task(
        ft_source_in=resources_creator.make_input_table("ft_source"),
        ft_altay_companies_tmp=resources_creator.make_temp_table("ft_altay_companies_tmp"),
        extra_poi_tmp=extra_poi_tmp,
        extra_poi_geoproduct_tmp=extra_poi_geoproduct_tmp,
        ft_source_out=ft_source_out)

    ft_source_out.logged_commit()

    result = list(ft_source_out.read_table())
    expected = resources_creator.read_expected_output_data("ft_source")
    assert result == expected


@pytest.mark.use_local_yt_yql
def test_merge_ft_center_task(environment_settings, resources_creator):
    ft_center_out = resources_creator.make_output_table("ft_center")

    task = merge_ft_center_task.MergeFtCenterUsingExtraPOIs()
    task.load_environment_settings(environment_settings)
    task(
        ft_center_in=resources_creator.make_input_table("ft_center"),
        ft_altay_companies_tmp=resources_creator.make_temp_table("ft_altay_companies_tmp"),
        extra_poi_tmp=resources_creator.make_temp_table("extra_poi_tmp"),
        ft_center_out=ft_center_out
    )

    result = list(ft_center_out.read_table())
    expected = resources_creator.read_expected_output_data("ft_center")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_merge_ft_nm_with_sprav_task(environment_settings, resources_creator):
    ft_nm_with_sprav_tmp = resources_creator.make_output_table("ft_nm_with_sprav_tmp")

    task = merge_ft_nm_task.MergeFtNmWithSpravTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_nm_in=resources_creator.make_input_table("ft_nm"),
        ft_altay_companies_tmp=resources_creator.make_temp_table("ft_altay_companies_tmp"),
        ft_altay_names_tmp=resources_creator.make_temp_table("ft_altay_names_tmp"),
        ft_nm_with_sprav_tmp=ft_nm_with_sprav_tmp)

    result = list(ft_nm_with_sprav_tmp.read_table())
    expected = resources_creator.read_expected_tmp_data("ft_nm_with_sprav_tmp")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_merge_ft_nm_with_extra_poi_task(environment_settings, resources_creator):
    ft_nm_out = resources_creator.make_output_table("ft_nm")

    task = merge_ft_nm_task.MergeFtNmWithExtraPoiTask()
    task.load_environment_settings(environment_settings)
    task(
        extra_poi_ft_nm=resources_creator.make_temp_table("extra_poi_ft_nm", schema_from="ft_nm"),
        ft_nm_with_sprav_tmp=resources_creator.make_temp_table("ft_nm_with_sprav_tmp", schema_from="ft_nm"),
        ft_nm_out=ft_nm_out)

    result = list(ft_nm_out.read_table())
    expected = resources_creator.read_expected_output_data("ft_nm")
    assert result == expected


@pytest.mark.use_local_yt(_YT_SERVER)
def test_fill_ft_poi_attr_task(environment_settings, resources_creator):
    ft_poi_attr_out = resources_creator.make_output_table("ft_poi_attr")

    task = fill_ft_poi_attr_task.FillFtPoiAttrTask()
    task.load_environment_settings(environment_settings)
    task(
        ft_poi_attr_in=resources_creator.make_input_table("ft_poi_attr"),
        ft_altay_companies_tmp=resources_creator.make_temp_table("ft_altay_companies_tmp"),
        extra_poi_tmp=resources_creator.make_temp_table("extra_poi_tmp"),
        altay_companies=resources_creator.make_altay_table(altay.COMPANIES),
        altay_rubrics=resources_creator.make_altay_table(altay.RUBRICS),
        ft_poi_attr_out=ft_poi_attr_out)

    result = list(ft_poi_attr_out.read_table())
    expected = resources_creator.read_expected_output_data("ft_poi_attr")
    assert result == expected
