import pytest

from smarttv.droideka.tests.helpers import remove_part


remove_part_test_data = [
    (['a', ['b']],
     {'a': {'b': 'foo', 'foo': 'bar'}, 'b': 123},
     {'a': {'foo': 'bar'}, 'b': 123}),
    (['a', 1, ['b']],
     {'a': [{'b': 'foo0', 'foo': 'bar0'}, {'b': 'foo1', 'foo': 'bar1'}], 'b': 123},
     {'a': [{'b': 'foo0', 'foo': 'bar0'}, {'foo': 'bar1'}], 'b': 123}),
    (['a', 1, ['b', 'foo']],
     {'a': [{'b': 'foo0', 'foo': 'bar0'}, {'b': 'foo1', 'foo': 'bar1', 'v': 'v'}], 'b': 123},
     {'a': [{'b': 'foo0', 'foo': 'bar0'}, {'v': 'v'}], 'b': 123}),
    (['a', [1]],
     {'a': [{'b': 'foo0', 'foo': 'bar0'}, {'b': 'foo1', 'foo': 'bar1'}], 'b': 123},
     {'a': [{'b': 'foo0', 'foo': 'bar0'}], 'b': 123}),
    (['a', [0, 1]],
     {'a': [{'b': 'foo0', 'foo': 'bar0'}, {'b': 'foo1', 'foo': 'bar1'}], 'b': 123},
     {'a': [], 'b': 123})
]


@pytest.mark.parametrize('path, input_obj, expected', remove_part_test_data)
def test_remove_part(path, input_obj, expected):
    assert remove_part(path, input_obj) == expected
