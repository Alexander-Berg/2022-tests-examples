import contextlib
import json
import tarfile
import typing as tp

import pytest

import yatest.common

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, parse_traits
from maps.garden.sdk.module_traits.validation import validate_modules_dependency_graph, \
    validate_modules_tracked_ancestors, validate_modules_tracked_ancestors_graph, validate_required_fields


@pytest.fixture(scope="module")
def all_module_traits() -> tp.Dict[str, ModuleTraits]:
    traits_tar = yatest.common.binary_path(
        "maps/garden/sdk/module_traits/tests/module_traits_bundle/module_traits.tar")
    with contextlib.closing(tarfile.open(traits_tar, "r")) as tar:
        all_traits = {}
        for file_info in tar.getmembers():
            f = tar.extractfile(file_info)
            module_name = file_info.name
            try:
                unparsed_traits = json.load(f)
                all_traits.update(parse_traits([unparsed_traits]))
            except Exception as ex:
                pytest.fail(f"Failed to parse traits for module {module_name}. Error: {ex}")
    return all_traits


def test_modules_dependency_graph(all_module_traits):
    validate_modules_dependency_graph(all_module_traits)


def test_modules_tracked_ancestors(all_module_traits):
    validate_modules_tracked_ancestors(all_module_traits)


def test_modules_tracked_ancestors_graph(all_module_traits):
    validate_modules_tracked_ancestors_graph(all_module_traits)


def test_readonly_props(all_module_traits):
    for module_name, traits in all_module_traits.items():
        assert not traits.capabilities, f"'capabilities' is a read-only field. " \
                                        f"Got {traits.capabilities} in the traits for module '{module_name}'"


def test_raise_error():
    with pytest.raises(ValueError) as ex:
        validate_required_fields({
            "test_module": ModuleTraits.parse_obj({
                "name": "test_module",
                "type": "source"
            }),
            "test_module_2": ModuleTraits.parse_obj({
                "name": "test_module_2",
                "type": "source"
            }),
        })

    assert len(ex.value.args[0].split("\n")) == 2
    assert "test_module" in ex.value.args[0] and "test_module_2" in ex.value.args[0]
    assert "sort_options" in ex.value.args[0]
