import os.path
import pytest

import yatest.common

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf, ymapsdf_schema

from maps.garden.modules.extra_poi_bundle.lib import graph as extra_poi_bundle
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_ft_experiment, custom_transformations
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.ymapsdf_load import ymapsdf_load
from maps.garden.modules.ymapsdf.lib import common

from maps.libs.ymapsdf.py.schema import YmapsdfSchema


YT_SERVER = "plato"


def _get_simple_yql_param(regions=None):
    return custom_transformations.TransformationDescription(
        custom_transformations.PostprocessTask(attach_file="simple.yql", exp_id="postprocess_simple_disp_plus_1", displayed_name="FtExperimentSimple"),
        regions or [ymapsdf.TEST_REGION]
    )


POSTPROCESSING_CASES = {
    "no_yql_postprocessing":  {},
    "simple": {"simple": _get_simple_yql_param()},
    "simple_is_prod": {custom_transformations.PRODUCTION: _get_simple_yql_param()},
    "simple_is_prod_other_region": {custom_transformations.PRODUCTION: _get_simple_yql_param(regions=["cis2"])}
}


def _make_extra_poi_experiments(cook, experiment_data_dir=None):
    """
    :param experiment_data_dir: the directory with source experiments data.
        If that is absent then the resource will be created as empty.
    """
    schema = [
        {
            "name": "id",
            "type": "int64",
            "required": True
        },
        {
            "name": "experiment",
            "type": "string",
            "required": True
        },
        {
            "name": "disp_class",
            "type": "int64",
            "required": True
        },
        {
            "name": "disp_class_tweak",
            "type": "double",
            "required": True
        },
        {
            "name": "disp_class_navi",
            "type": "int64",
            "required": True
        },
        {
            "name": "disp_class_tweak_navi",
            "type": "double",
            "required": True
        },
    ]

    if experiment_data_dir:
        filepath = os.path.join(experiment_data_dir, "extra_poi_experiments.jsonl")
    else:
        filepath = None

    resource = data_utils.create_yt_resource(
        cook,
        resource_name="extra_poi_experiments",
        properties={"release_name": "2020-02-18"},
        filepath=filepath,
        schema=schema)

    if not experiment_data_dir:
        resource.properties["is_empty"] = True

    return resource


def _make_ft_merged_tmp(cook):
    return data_utils.create_yt_resource(
        cook,
        resource_name=common.MERGE_POI_STAGE.resource_name("ft_merged_tmp", test_utils.ymapsdf.TEST_REGION,
                                                           test_utils.ymapsdf.TEST_VENDOR),
        properties=test_utils.ymapsdf.TEST_PROPERTIES,
        filepath=yatest.common.test_source_path("data/input/merge_poi/ft_merged_tmp.jsonl"),
        schema=YmapsdfSchema.for_table("ft").make_yt_schema())


def _make_extra_poi_update_tmp(cook):
    schema = [
        {
            "name": "ft_id",
            "type": "int64",
            "required": True
        },
        {
            "name": "disp_class",
            "type": "int64",
            "required": True
        },
        {
            "name": "disp_class_tweak",
            "type": "double",
            "required": True
        },
        {
            "name": "disp_class_navi",
            "type": "int64",
            "required": True
        },
        {
            "name": "disp_class_tweak_navi",
            "type": "double",
            "required": True
        },
    ]

    return data_utils.create_yt_resource(
        cook,
        resource_name=common.MERGE_POI_STAGE.resource_name("extra_poi_update_tmp", test_utils.ymapsdf.TEST_REGION,
                                                           test_utils.ymapsdf.TEST_VENDOR),
        properties=test_utils.ymapsdf.TEST_PROPERTIES,
        filepath=yatest.common.test_source_path("data/input/merge_poi/extra_poi_update_tmp.jsonl"),
        schema=schema)


def _make_ft_altay_companies_tmp(cook):
    schema = [
        {
            "name": "ft_id",
            "type": "int64",
            "required": True
        },
        {
            "name": "ft_type_id",
            "type": "int64",
            "required": True
        },
        {
            "name": "icon_class",
            "type": "string",
            "required": False
        },
        {
            "name": "indoor_level_ft_type_id",
            "type": "int64",
            "required": False
        },
        {
            "name": "is_closed",
            "type": "boolean",
            "required": False
        },
        {
            "name": "is_exported_from_extra_poi",
            "type": "boolean",
            "required": True
        },
        {
            "name": "is_indoor_poi",
            "type": "boolean",
            "required": True
        },
        {
            "name": "is_parking_poi",
            "type": "boolean",
            "required": True
        },
        {
            "name": "is_protected_poi",
            "type": "boolean",
            "required": True
        },
        {
            "name": "is_unknown_company",
            "type": "boolean",
            "required": True
        },
        {
            "name": "rubric_id",
            "type": "string",
            "required": False
        },
        {
            "name": "shape",
            "type": "string",
            "required": False
        },
        {
            "name": "source_id",
            "type": "string",
            "required": False
        },
        {
            "name": "x",
            "type": "double",
            "required": False
        },
        {
            "name": "y",
            "type": "double",
            "required": False
        },
    ]

    return data_utils.create_yt_resource(
        cook,
        resource_name=common.MERGE_POI_STAGE.resource_name("ft_altay_companies_tmp", test_utils.ymapsdf.TEST_REGION,
                                                           test_utils.ymapsdf.TEST_VENDOR),
        properties=test_utils.ymapsdf.TEST_PROPERTIES,
        filepath=yatest.common.test_source_path("data/input/merge_poi/ft_altay_companies_tmp.jsonl"),
        schema=schema)


def has_postprocessing_for_region(postprocessing, region):
    return any([region in t.regions for t in postprocessing.values()])


def _create_extra_tables_for_postprocessing_experiments(cook, postprocessing):
    # empty tables for experimental yql
    if has_postprocessing_for_region(postprocessing, ymapsdf.TEST_REGION):
        ymapsdf.create_resources(
            cook,
            stage=common.MERGE_POI_STAGE,
            table_names=["ft_center", "node"]
        )

        _make_extra_poi_update_tmp(cook)
        _make_ft_altay_companies_tmp(cook)


def _create_resources(environment_settings):
    cook = test_utils.GraphCook(environment_settings)
    merge_poi.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    ymapsdf_load.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    extra_poi_bundle.fill_graph(cook.input_builder())
    merge_ft_experiment.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_resources(
        cook,
        stage=common.ALPHA_STAGE,
        data_dir=yatest.common.test_source_path("data/input/alpha"),
        table_names=["ft", "ft_poi_attr", "ft_experiment"])

    ymapsdf.create_resources(
        cook,
        stage=common.MERGE_POI_STAGE,
        data_dir=yatest.common.test_source_path("data/input/merge_poi"),
        table_names=["ft_source"])

    _make_ft_merged_tmp(cook)
    return cook


@pytest.mark.parametrize("postprocessing", POSTPROCESSING_CASES.values(), ids=POSTPROCESSING_CASES.keys())
@pytest.mark.use_local_yt_yql
def test_ft_experiment_merging(environment_settings, monkeypatch, postprocessing, request):
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", postprocessing)
    cook = _create_resources(environment_settings)
    _make_extra_poi_experiments(cook, yatest.common.test_source_path("data/input"))
    _create_extra_tables_for_postprocessing_experiments(cook, postprocessing)
    test_utils.execute(cook)

    data_utils.validate_data(
        environment_settings,
        YT_SERVER,
        common.MERGE_POI_STAGE,
        ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path(os.path.join("data", "test_ft_experiment_merging", f"output_{request.node.callspec.id}")),
        ["ft_experiment"]
    )

    ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        common.MERGE_POI_STAGE,
        ymapsdf.TEST_PROPERTIES,
        ["ft_experiment"]
    )


@pytest.mark.parametrize("postprocessing", POSTPROCESSING_CASES.values(), ids=POSTPROCESSING_CASES.keys())
@pytest.mark.use_local_yt_yql
def test_no_extra_poi_experiment_table(environment_settings, monkeypatch, postprocessing, request):
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", postprocessing)
    cook = _create_resources(environment_settings)
    _make_extra_poi_experiments(cook)
    _create_extra_tables_for_postprocessing_experiments(cook, postprocessing)

    # Run the test
    test_utils.execute(cook)

    no_relevant_exps = not has_postprocessing_for_region(postprocessing, ymapsdf.TEST_REGION)
    if not postprocessing or no_relevant_exps:
        # There is no `ft_experiment` table
        pass
    else:
        data_utils.validate_data(
            environment_settings,
            YT_SERVER,
            common.MERGE_POI_STAGE,
            ymapsdf.TEST_PROPERTIES,
            yatest.common.test_source_path(os.path.join("data", "test_no_extra_poi_experiment_table", f"output_{request.node.callspec.id}")),
            ["ft_experiment"]
        )
