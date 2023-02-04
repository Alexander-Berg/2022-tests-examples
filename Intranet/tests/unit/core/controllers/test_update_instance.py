import pytest

from intranet.femida.src.core.controllers import update_instance

from intranet.femida.tests.models import Table, WFTable


def create_test_instance(**params):
    return Table.objects.create(**params)


def test_empty_data_and_empty_update_fields(django_assert_num_queries):
    instance = create_test_instance()
    with django_assert_num_queries(0):
        update_instance(instance, data={})


def test_extra_update_fields(django_assert_num_queries):
    instance = create_test_instance()
    modified = instance.modified
    with django_assert_num_queries(1):
        update_instance(instance, data={}, extra_update_fields=('char_field',))
        assert modified != instance.modified


def test_some_data():
    char_field = 'char1'
    int_field = 0
    bool_field = False
    instance = create_test_instance(
        char_field=char_field,
        int_field=int_field,
        bool_field=bool_field,
    )
    instance = update_instance(
        instance=instance,
        data={
            'char_field': 'char2',
            'int_field': 1,
            'bool_field': True,
        },
        extra_update_fields=('char_field',),
    )
    assert char_field != instance.char_field
    assert int_field != instance.int_field
    assert bool_field != instance.bool_field


def test_wrong_data():
    with pytest.raises(AttributeError):
        update_instance(create_test_instance(), data={'wrong_field': 1})


def test_wrong_extra_update_fields():
    with pytest.raises(ValueError):
        update_instance(create_test_instance(), data={}, extra_update_fields=('wrong_field',))


def test_update_wf_fields():
    wf_field = 'this'
    instance = WFTable.objects.create(
        wf_field=wf_field,
    )
    formatted_wf_field = instance.formatted_wf_field

    instance = update_instance(
        instance=instance,
        data={
            'wf_field': 'that',
        },
    )
    assert instance.wf_field != wf_field
    assert instance.formatted_wf_field != formatted_wf_field
