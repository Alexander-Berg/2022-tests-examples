import pytest

import infra.callisto.controllers.utils.nested_obj as nested_obj
import infra.callisto.controllers.utils.const_obj as const_obj
import infra.callisto.controllers.utils.funcs as funcs


class A(nested_obj.NestedObject):
    fields = (
        ('x', nested_obj.String),
        ('y', nested_obj.Integer),
    )


class B(nested_obj.NestedObject):
    fields = (
        ('a', A),
    )


class C(nested_obj.NestedObject):
    fields = (
        ('b', B),
    )


def test_types_validation():
    with pytest.raises(TypeError):
        A(1, 2)  # type mismatch

    with pytest.raises(TypeError):
        A(1)  # not enough args

    with pytest.raises(TypeError):
        C(A(1, 2))  # type mismatch

    A(x='1', y=2)
    A('2', 3)
    B(A('3', 4))
    B(a=A('3', 4))


def test_dump_load():
    a = B(A('1', 2))
    b = B.load_json_flat(a.dump_json_flat())
    assert a == b
    assert hash(a) == hash(b)
    assert a.dump_json_flat() == b.dump_json_flat()

    b = B(A('2', 3))
    assert a != b
    assert hash(a) != hash(b)
    assert a.dump_json_flat() != b.dump_json_flat()

    a = C(B(A('1', 2)))
    b = C.load_json_flat(a.dump_json_flat())
    assert a == b
    assert hash(a) == hash(b)
    assert a.dump_json_flat() == b.dump_json_flat()


def test_immutable():
    a = B(A('1', 2))
    with pytest.raises(AttributeError):
        a.new_attr = 1

    with pytest.raises(AttributeError):
        a.a = 1

    assert len({a, a}) == 1


def test_merge_dicts():
    assert funcs.merge_dicts({'a': 1}, {'b': 2}, {'c': 3}) == {'a': 1, 'b': 2, 'c': 3}
    funcs.merge_dicts({'a': 1}, {'b': 2}, {'c': 3, 'a': 4})
    with pytest.raises(ValueError):
        funcs.merge_dicts_no_intersection({'a': 1}, {'b': 2}, {'c': 3, 'a': 4})


def test_invert_mapping():
    assert funcs.invert_mapping({}) == {}
    assert funcs.invert_mapping({'a': {'b', 'c'}}) == {'b': {'a'}, 'c': {'a'}}
    assert funcs.invert_mapping({'a': {'1', '2'}, 'b': {'2', '3'}}) == {'1': {'a'}, '2': {'a', 'b'}, '3': {'b'}}

    for example in [
        {'a': {'b', 'c'}},
        {'a': {'1', '2'}, 'b': {'2', '3'}},
        {'123': {'123'}, 'bbb': {'aaa', '123', 'bbb'}},
    ]:
        assert funcs.invert_mapping(funcs.invert_mapping(example)) == example


def test_const_obj():
    class A(const_obj.ConstObj):
        def __init__(self, x):
            self.x = x
            self.on_initialize()

    a = A(1234)
    assert a.x == 1234
    with pytest.raises(AttributeError):
        a.x = 6789

    with pytest.raises(AttributeError):
        a.y = 1234
