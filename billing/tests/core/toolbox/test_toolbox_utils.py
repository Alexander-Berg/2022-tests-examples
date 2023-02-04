from collections import namedtuple

from mdh.core.toolbox.utils import get_obj_id, filter_unique


def test_get_obj_id():

    assert get_obj_id(None) is None
    assert get_obj_id(1) == 1

    obj = namedtuple('some', ['id'])(id=20)
    assert get_obj_id(obj) == 20


def test_filter_unique():
    alist = [1, 3, 1, 2, 7, 2, 7]
    alist_id = id(alist)
    filter_unique(alist)
    assert alist == [1, 3, 2, 7]
    assert id(alist) == alist_id
