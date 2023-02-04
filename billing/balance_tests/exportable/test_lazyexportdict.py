import pytest

from tests.balance_tests.exportable.conftest import get_test_queue_of_instance


def not_fully_loaded(instance):
    return instance.exports.is_relation_loaded() is False


@pytest.mark.parametrize(
    ('lookup_func_name',),
    [
        ('__contains__',),
        ('__getitem__',),
    ]
)
def test_single_lookup_does_not_load_all(
    selected_instance_with_export_entry,
    lookup_func_name,
):
    test_queue_name = get_test_queue_of_instance(selected_instance_with_export_entry)

    lookup_func = getattr(selected_instance_with_export_entry.exports, lookup_func_name)

    assert not_fully_loaded(selected_instance_with_export_entry)
    assert lookup_func(test_queue_name)
    assert not_fully_loaded(selected_instance_with_export_entry)


def test_does_not_contain(selected_exportable_instance):
    test_queue_name = get_test_queue_of_instance(selected_exportable_instance)

    assert not_fully_loaded(selected_exportable_instance)
    with pytest.raises(KeyError):
        non_existing_obj = selected_exportable_instance.exports[test_queue_name]

    assert not_fully_loaded(selected_exportable_instance)
    assert test_queue_name not in selected_exportable_instance.exports
    assert not_fully_loaded(selected_exportable_instance)


def test_remove_from_cache(
    selected_instance_with_export_entry,
    session,
):
    instance = selected_instance_with_export_entry
    test_queue_name = get_test_queue_of_instance(instance)

    assert not_fully_loaded(selected_instance_with_export_entry)

    # load export instance into cache
    assert test_queue_name in instance.exports
    assert test_queue_name in instance.exports._cache

    del instance.exports[test_queue_name]

    # make sure after deletion old object was not retrieved
    assert test_queue_name not in instance.exports._cache
    assert test_queue_name not in instance.exports

    # assert object in relation was deleted
    assert test_queue_name not in instance.exports_rel


def test_full_cleanup(
    selected_instance_with_export_entry,
):
    instance = selected_instance_with_export_entry

    assert len(instance.exports_rel) > 0
    instance.exports = {}

    assert len(instance.exports_rel) == 0
    assert len(instance.exports._cache) == 0
    assert len(instance.exports) == 0
