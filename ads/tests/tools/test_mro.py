import pytest
from ads_pytorch.tools.mro import find_longest_descendant


class A: pass
class B(A): pass
class C(A): pass
class D(C): pass
class X: pass


@pytest.mark.parametrize(
    'items',
    [
        [],
        [1, 2, 3],
        [X, '1']
    ],
    ids=['EmptyList', 'NoDescendant', 'SomeExtraCls']
)
def test_find_no_items(items):
    with pytest.raises(ValueError):
        find_longest_descendant(lst=items, base_class=A)


def test_find_self():
    res = find_longest_descendant(lst=[1, 2, '2', A, 'e'], base_class=A)
    assert res is A


@pytest.mark.parametrize('base_class', [A, C, D])
def test_find_long_descendant(base_class):
    res = find_longest_descendant(lst=[A, 1, C, D, 'e'], base_class=base_class)
    assert res is D


def test_fork_detection():
    with pytest.raises(ValueError):
        find_longest_descendant(lst=[A, B, C, D, 'e'], base_class=A)


def test_work_with_fork_other_lds():
    res = find_longest_descendant(lst=[A, B, C, D, 'e'], base_class=C)
    assert res is D
