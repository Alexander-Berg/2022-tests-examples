import pytest

from smarttv.droideka.utils.chaining import ResultBuilder, DataSource, ResultBuilderExtension


def as_dict(value: str):
    return {'value': value}


class DataSource1(DataSource):
    def get_result(self):
        return {
            'key1': 'value1',
            'key2': 2
        }


class BaseTstDataSource(DataSource):
    def __init__(self, key=None, db=None):
        self.key = key
        self.db = db or {}

    def get_result(self):
        value = self.db.get(self.key)
        return as_dict(value)


class DataSource2(BaseTstDataSource):
    def __init__(self, key):
        db = {
            1: {'first': 'A'},
            2: {'second': 'B'},
            3: {'third': 'C'}
        }
        super().__init__(key, db)


class DataSource3(BaseTstDataSource):
    def __init__(self, key):
        db = {
            'value35': 'Some text 35',
            'value1': 'Some text 1',
            'value2': 'Some text 2'
        }
        super().__init__(key, db)


class DataSource4(BaseTstDataSource):
    def __init__(self, key):
        db = {
            'A': 'Some text 35',
            'B': 'Some text 1',
            'C': 'Some text 2'
        }
        super().__init__(key, db)


class DataSource5(DataSource):
    def get_result(self):
        raise ValueError("Some error")


def test_get_result():
    builder = ResultBuilder(DataSource1())
    result = builder.get_result()

    assert result['key1'] == 'value1'
    assert result['key2'] == 2


def test_result_with_one_chain():
    result = ResultBuilder(DataSource1()).extend(
        ResultBuilderExtension(
            data_source_provider=lambda res: DataSource2(res['key2']),
            name='key3'
        )
    ).get_result()

    assert result['key1'] == 'value1'
    assert result['key2'] == 2
    assert result['key3']['value']['second'] == 'B'


def test_result_with_two_chains_on_same_level():
    result = ResultBuilder(DataSource1()).extend(
        ResultBuilderExtension(
            data_source_provider=lambda res: DataSource2(res['key2']),
            name='key3'
        ),
        ResultBuilderExtension(
            data_source_provider=lambda res: DataSource3(res['key1']),
            name='expected'
        )
    ).get_result()

    assert result['key1'] == 'value1'
    assert result['key2'] == 2
    assert result['key3']['value']['second'] == 'B'
    assert result['expected']['value'] == 'Some text 1'


def test_result_raise_exception_not_raised():
    result = ResultBuilder(DataSource5()).get_result()

    assert result == {}, result


def test_extended_result_raise_exception_not_raised():
    builder = ResultBuilder(DataSource1())

    extension = ResultBuilderExtension(data_source_provider=lambda _: DataSource5(), raise_exception=False)
    builder.add_extension(extension)
    result = builder.get_result()

    assert result == DataSource1().get_result()


def test_result_raise_exception_raised():
    with pytest.raises(ValueError):
        ResultBuilder(data_source=DataSource5(), raise_exception=True).get_result()


def test_extended_result_raise_exception_raised():
    with pytest.raises(ValueError):
        ResultBuilder(DataSource1()).extend(ResultBuilderExtension(
            data_source_provider=lambda res: DataSource5(),
            raise_exception=True
        )).get_result()
