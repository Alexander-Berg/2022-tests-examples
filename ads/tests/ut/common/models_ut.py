from ads.watchman.timeline.api.lib.common import models as co_models


class DataObjectChildOne(co_models.DataObject):
    def __init__(self, a, b):
        self.a = a
        self.b = b


class DataObjectChildTwo(co_models.DataObject):
    def __init__(self, a, b):
        self.a = a
        self.b = b


def test_that_data_object_equals_to_itself():
    data_object = DataObjectChildOne(1, 2)
    assert data_object == data_object


def test_that_data_object_equals_to_object_with_same_data():
    data_object_first = DataObjectChildOne(1, 2)
    data_object_second = DataObjectChildOne(1, 2)
    assert data_object_first == data_object_second


def test_that_data_object_not_equals_to_object_with_other_data():
    data_object_first = DataObjectChildOne(1, 2)
    data_object_second = DataObjectChildOne(1, 3)
    assert data_object_first != data_object_second


def test_that_data_object_not_equals_to_object_with_other_type():
    data_object_first = DataObjectChildOne(1, 2)
    data_object_second = DataObjectChildTwo(1, 2)
    assert data_object_first != data_object_second


def test_that_data_object_has_nice_repr():
    data_object_first = DataObjectChildOne(1, 2)
    assert repr(data_object_first) in {"DataObjectChildOne(a=1, b=2)", "DataObjectChildOne(b=2, a=1)"}
