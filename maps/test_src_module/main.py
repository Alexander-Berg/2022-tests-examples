from maps.garden.sdk.resources import PythonResource

from maps.garden.sdk.resources.scanners.common import SourceDataset, BuildExternalResource
from maps.garden.sdk.module_rpc.module_runner import run_module_simple


def fill_graph(builder):
    builder.add_resource(PythonResource('input_resource'))


def scan_resources(environment_settings):
    yield SourceDataset(
        foreign_key={"shipping_date": "20201021"},
        resources=[BuildExternalResource(
            resource_name="input_resource",
            properties={
                "first_property": "first_value",
                "second_property": 2,
                "shipping_date": "20201021"
            }
        )]
    )


def main():
    run_module_simple('test_src_module', fill_graph, scan_resources=scan_resources)
