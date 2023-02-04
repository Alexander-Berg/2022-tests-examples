import pytest

from maps_adv.billing_proxy.lib.core.cpm_filters.base import BaseCpmFilter
from maps_adv.billing_proxy.lib.core.cpm_filters.registry import FiltersRegistry


class CpmFilterForTests(BaseCpmFilter):
    def __call__(self):
        return 15


@pytest.mark.parametrize("filters", [None, {}, {"str": object()}])
def test_creating(filters):
    FiltersRegistry(filters)


def test_getitem():
    cpm_filter = CpmFilterForTests()

    registry = FiltersRegistry({"key": cpm_filter})

    assert registry["key"] is cpm_filter


def test_contains():
    cpm_filter1 = CpmFilterForTests()
    cpm_filter2 = CpmFilterForTests()

    registry = FiltersRegistry({"key1": cpm_filter1, "key2": cpm_filter2})

    assert "key1" in registry
    assert "key0" not in registry


@pytest.mark.parametrize(
    ("filters", "expected_len"),
    [
        (None, 0),
        ({}, 0),
        ({"key1": CpmFilterForTests()}, 1),
        ({"key1": CpmFilterForTests(), "key2": CpmFilterForTests()}, 2),
    ],
)
def test_len(filters, expected_len):
    registry = FiltersRegistry(filters)

    assert len(registry) == expected_len


def test_iter():
    cpm_filter1 = CpmFilterForTests()
    cpm_filter2 = CpmFilterForTests()

    registry = FiltersRegistry({"key1": cpm_filter1, "key2": cpm_filter2})

    assert list(registry) == ["key1", "key2"]


def test_keys():
    cpm_filter1 = CpmFilterForTests()
    cpm_filter2 = CpmFilterForTests()

    registry = FiltersRegistry({"key1": cpm_filter1, "key2": cpm_filter2})

    assert list(registry.keys()) == ["key1", "key2"]


def test_values():
    cpm_filter1 = CpmFilterForTests()
    cpm_filter2 = CpmFilterForTests()

    registry = FiltersRegistry({"key1": cpm_filter1, "key2": cpm_filter2})

    assert list(registry.values()) == [cpm_filter1, cpm_filter2]


def test_items():
    cpm_filter1 = CpmFilterForTests()
    cpm_filter2 = CpmFilterForTests()

    registry = FiltersRegistry({"key1": cpm_filter1, "key2": cpm_filter2})

    assert list(registry.items()) == [("key1", cpm_filter1), ("key2", cpm_filter2)]
