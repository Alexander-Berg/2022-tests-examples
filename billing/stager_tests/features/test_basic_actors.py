from tests.base import BalanceTest
from yt.wrapper.mappings import FrozenDict
from balance.processors.stager.actors import (
    Aggregator)


class AggregatorTest(BalanceTest):
    def test_preprocessor(self):
        aggregator = Aggregator(
            source_tables=('dummy',),
            destination_table='dummy',
            group_fields=('a', 'b'))

        aggregator.add_preprocessor('c', lambda row: row['c'] * 2)

        test_data = [
            {
                'a': 1,
                'b': 2,
                'c': 3,
                '@table_index': 1
            },
            {
                'a': 1,
                'b': 2,
                'c': 5,
                '@table_index': 1
            }
        ]

        expected = [
            {'a': 1, 'c': 6, 'b': 2, 'table_index': 1},
            {'a': 1, 'c': 10, 'b': 2, 'table_index': 1}
        ]

        mapper = aggregator.get_mapper()

        result = []
        for row in test_data:
            for mapped_row in mapper(row):
                result.append(mapped_row)

        self.assertEqual(result, expected)

    def test_counter(self):
        aggregator = Aggregator(
            source_tables=('dummy',),
            destination_table='dummy',
            group_fields=('a', 'b'))

        aggregator.add_counter('c', lambda row: row['c'] * 2, int)

        key = FrozenDict(a=1, b=2)
        rows = [{'a': 1, 'b': 2, 'c': 3},
                {'a': 1, 'b': 2, 'c': 5}]
        expected = [{'a': 1, 'b': 2, 'c': 16}]

        reducer = aggregator.get_reducer()
        result = [a for a in reducer(key, rows)]
        self.assertEqual(result, expected)

    def test_postprocessor(self):
        aggregator = Aggregator(
            source_tables=('dummy',),
            destination_table='dummy',
            group_fields=('a', 'b'),
            columns=('a', 'b', 'c'))

        aggregator.add_postprocessor('a', lambda row: row['a'] * 5)
        aggregator.add_postprocessor('b', lambda row: row['a'] * 10)

        key = FrozenDict(a=1, b=2)
        rows = [{'a': 1, 'b': 2, 'c': 3},
                {'a': 1, 'b': 2, 'c': 6}]
        expected = [
            {'a': 5, 'b': 50},
        ]

        reducer = aggregator.get_reducer()
        result = [a for a in reducer(key, rows)]
        self.assertEqual(result, expected)


if __name__ == '__main__':
    AggregatorTest._call_test('test_postprocessor')
    AggregatorTest._call_test('test_preprocessor')
    AggregatorTest._call_test('test_counter')
