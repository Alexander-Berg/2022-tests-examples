from mdh.launches.importers.base import WrappedValue, ValuesBag


def test_wrapped_value():
    val = WrappedValue(field='myfield', initial=1, new=2)
    assert f'{val}' == 'myfield'


def test_values_bag():
    bag = ValuesBag({'a', 'b'})
    bag.store('x', 'y')
    bag.store('a', '1')
    bag.store('a', '2')
    bag.store('a', '2')

    assert bag.bag == {'x': ['y'], 'a': ['1', '2', '2']}

    bag._counter = 199
    bag.store('b', '7')

    # Проверка устранения одинаковых значений.
    assert len(bag.bag['a']) == 2

    bag.printout()
