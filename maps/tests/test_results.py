import maps.analyzer.pylibs.yql.results as results


class MockTable(object):
    def __init__(self, column_names, rows):
        self.column_names = column_names
        self.rows = rows

    def fetch_full_data(self):
        pass

    def as_list(self):
        return self.rows

    def as_dict(self):
        return [
            {
                name: value
                for name, value in zip(self.column_names, row)
            }
            for row in self.rows
        ]


def test_results():
    TEST_TABLES = [
        MockTable(
            ['age', 'name'],
            [
                [10, 'Petr'],
                [20, 'Fedor'],
            ],
        ),
        MockTable(
            ['id', 'price'],
            [
                [0, 100.0],
                [1, 200.0],
            ],
        ),
    ]

    AS_LIST_ETALON = [t.as_list() for t in TEST_TABLES]
    AS_DICT_ETALON = [t.as_dict() for t in TEST_TABLES]

    assert results.read_results(TEST_TABLES, as_list=True) == AS_LIST_ETALON
    assert results.read_results(TEST_TABLES, as_list=False) == AS_DICT_ETALON
