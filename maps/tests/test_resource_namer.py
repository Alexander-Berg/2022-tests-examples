from maps.garden.sdk import core as garden
from maps.garden.sdk.extensions import resource_namer


def test_resource_name():
    namer = resource_namer.ResourceNamer(module_name="my_module")
    assert namer.resource_name("my_table") == "my_module_my_table"

    namer = resource_namer.StageResourceNamer(module_name="my_module", stage_name="my_stage")
    assert namer.resource_name("my_table") == "my_module_my_stage_my_table"


def test_yt_path():
    namer = resource_namer.ResourceNamer(
        module_name="my_module",
        yt_path_prefix_template="{shipping_date}/{region}_{vendor}/")

    assert namer.make_yt_path("my_table") == "{shipping_date}/{region}_{vendor}/my_table"


def test_make_arguments():
    namer = resource_namer.StageResourceNamer(module_name="my_module", stage_name="my_stage")

    input_demands = namer.make_demands("table1", "table2")
    assert input_demands == garden.Demands(
        table1="my_module_my_stage_table1", table2="my_module_my_stage_table2")

    input_demands = namer.make_input_demands("table1", "table2")
    assert input_demands == garden.Demands(
        table1_in="my_module_my_stage_table1", table2_in="my_module_my_stage_table2")

    output_demands = namer.make_output_demands("table1", "table2")
    assert output_demands == garden.Demands(
        table1_out="my_module_my_stage_table1", table2_out="my_module_my_stage_table2")

    creates = namer.make_creates("table1", "table2")
    assert creates == garden.Creates(
        table1="my_module_my_stage_table1", table2="my_module_my_stage_table2")

    creates = namer.make_output_creates("table1", "table2")
    assert creates == garden.Creates(
        table1_out="my_module_my_stage_table1", table2_out="my_module_my_stage_table2")


def test_extract_arguments():
    resources = {
        "my_table_in": "foo",
        "my_table_out": "bar"
    }

    assert resource_namer.extract_input_resources(resources) == {"my_table": "foo"}
    assert resource_namer.extract_output_resources(resources) == {"my_table": "bar"}
