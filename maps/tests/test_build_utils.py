from dataclasses import dataclass
from copy import deepcopy
from collections.abc import Callable
from maps.garden.libs_server.build import build_utils
from maps.garden.sdk.module_traits.module_traits import SortOption


def is_sorted(arr: list, key: Callable):
    return all(
        key(arr[i]) <= key(arr[i+1])
        for i in range(len(arr) - 1)
    )


def test_objects_sorting_by_property():
    @dataclass
    class ValueHolder:
        properties: dict

    ref_objs = [ValueHolder(properties={"value": i}) for i in range(3, 0, -1)]
    for sort_option in [
        SortOption(sort_by_property="value"),
        SortOption(key_pattern="{0.properties[value]}")
    ]:
        sort_option.reverse = False
        objs = deepcopy(ref_objs)
        build_utils.sort_using_options(objs, [sort_option])
        assert is_sorted(objs, lambda x: x.properties["value"])

        assert not is_sorted(ref_objs, lambda x: x.properties["value"])

        sort_option.reverse = True
        build_utils.sort_using_options(objs, [sort_option])
        assert is_sorted(objs, lambda x: -x.properties["value"])


def test_objects_sorting_by_build_id_simple():
    @dataclass
    class SimpleId:
        build_id: int

    ref_objs = [SimpleId(build_id=i) for i in range(3, 0, -1)]
    sort_option = SortOption(sort_by_build_id=True)
    sort_option.reverse = False
    objs = deepcopy(ref_objs)
    build_utils.sort_using_options(objs, [sort_option])
    assert is_sorted(objs, lambda x: x.build_id)

    assert not is_sorted(ref_objs, lambda x: x.build_id)

    sort_option.reverse = True
    build_utils.sort_using_options(objs, [sort_option])
    assert is_sorted(objs, lambda x: -x.build_id)


def test_objects_sorting_by_build_id_version():
    @dataclass
    class Version:
        build_id: int

    @dataclass
    class SimpleId:
        version: Version

    ref_objs = [SimpleId(version=Version(build_id=i)) for i in range(3, 0, -1)]
    sort_option = SortOption(sort_by_build_id=True)
    sort_option.reverse = False
    objs = deepcopy(ref_objs)
    build_utils.sort_using_options(objs, [sort_option])
    assert is_sorted(objs, lambda x: x.version.build_id)

    assert not is_sorted(ref_objs, lambda x: x.version.build_id)

    sort_option.reverse = True
    build_utils.sort_using_options(objs, [sort_option])
    assert is_sorted(objs, lambda x: -x.version.build_id)
