from typing import Any, Optional

from infra.deploy_notifications_controller.lib.dict_wrapper import DictWrapper


def test_empty():
    empty_dw = DictWrapper({})
    assert empty_dw.empty()

    not_empty_dw = DictWrapper({'a': 1})
    assert not not_empty_dw.empty()


def test_constructor_copy():
    base_dict = {'a': 1, 'b': 2, 'c': {'d': 3}}
    dw = DictWrapper(base_dict)

    base_dict.pop('b')
    base_dict['e'] = 4
    base_dict['c']['d'] = 5

    expected_dict = {'a': 1, 'b': 2, 'c': {'d': 3}}
    actual_dict = dw.to_dict()

    assert expected_dict == actual_dict


def test_get_field():
    b_value_dw = DictWrapper({'c': 3})
    dw = DictWrapper(
        {
            'a': 1,
            'b': b_value_dw,
            'atomic': b_value_dw,
            'none': None,
        },
        atomic={'atomic'}
    )

    assert dw.get_field('a') == 1  # primitive

    assert dw.get_field('b') == b_value_dw  # dict wrapper
    assert not dw.get_field('b') is b_value_dw  # constructor values are deep copying

    assert dw.get_field('atomic') == b_value_dw  # Atomic is the same as usual

    assert dw.get_field('none') is None  # None is stored
    assert dw.get_field('none', -1) == -1  # None with default == default

    assert dw.get_field('c') is None  # not field
    assert dw.get_field('d', -1) == -1  # not field with default


def dict_wrapper_update_field_and_assert(
    dw: DictWrapper,
    field_name: str,
    new_value,
    expected_value: Optional[Any] = None
):
    if expected_value is None:
        expected_value = new_value

    dw.update_field(field_name, new_value)
    assert dw.get_field(field_name) is expected_value


def test_update_field():
    dw = DictWrapper(
        {
            'p': 0,
            'dw_1': DictWrapper({'x': 1}),
            'dw_2': DictWrapper({'y': 3}),
            'dw_3': DictWrapper({'z': 5}),
            'd': 55,
            'atomic_dw': DictWrapper({'a': 1}),
            'atomic_primitive': 4,
        },
        atomic={'atomic_dw', 'atomic_primitive'}
    )

    new_dw_value = DictWrapper({'b': 2})
    new_primitive = 5

    dict_wrapper_update_field_and_assert(dw, 'p', 4)  # primitive
    dict_wrapper_update_field_and_assert(dw, 'new', 1)  # new field
    dict_wrapper_update_field_and_assert(dw, 'dw_1', 4)  # dw -> primitive
    dict_wrapper_update_field_and_assert(dw, 'dw_2', {'p': 10})  # dw -> dict
    dict_wrapper_update_field_and_assert(dw, 'dw_3', DictWrapper({'qwe': 10}))  # dw -> dw
    dict_wrapper_update_field_and_assert(dw, 'd', None, 55)  # None doesn't change value
    dict_wrapper_update_field_and_assert(dw, 'atomic_dw', new_dw_value, new_dw_value)  # atomic value updated without recursion
    dict_wrapper_update_field_and_assert(dw, 'atomic_primitive', new_primitive, new_primitive)  # atomic primitive same as usual


def test_eq():
    actual_dw = DictWrapper({
        'a': 1,
        'b': DictWrapper({'c': 2, 'x': 5}),
        'd': {'e': 3},
        'f': 4,
    })

    equal_dw = DictWrapper(actual_dw)
    assert actual_dw == equal_dw

    not_equal_primitive_field_dw = DictWrapper(actual_dw)
    not_equal_primitive_field_dw.update_field('a', 0)

    assert actual_dw != not_equal_primitive_field_dw

    not_equal_dw_field_dw = DictWrapper(actual_dw)
    not_equal_dw_field_dw.get_field('b').update_field('y', 1)

    assert actual_dw != not_equal_dw_field_dw


def test_update_by():
    actual_dw = DictWrapper(
        {
            'a': 1,
            'b': DictWrapper({'c': 2, 'x': 5}),
            'd': {'e': 3},
            'f': 4,
            'atomic_dw': DictWrapper({'q': 3}),
        },
        atomic={'atomic_dw'}
    )

    new_atomic_dw = DictWrapper({'q2': 4})

    update_dw = DictWrapper({
        'a': 0,
        'b': {'c': 3, 'z': 4},
        'd': DictWrapper({'f': 5}),
        'atomic_dw': new_atomic_dw,
    })

    actual_dw.update_by(update_dw)

    expected_dw = DictWrapper({
        'a': 0,
        'b': DictWrapper({'c': 3, 'z': 4, 'x': 5}),
        'd': DictWrapper({'f': 5}),
        'f': 4,
        'atomic_dw': new_atomic_dw,
    })

    assert expected_dw == actual_dw
