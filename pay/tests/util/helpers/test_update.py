from paysys.sre.tools.monitorings.lib.util.helpers import update


class TestUpdate:
    def test_update_lists_empty_empty(self):
        assert update({'a': []}, {'a': []}) == {'a': []}

    def test_update_lists_empty_not_a_list(self):
        assert update({'a': []}, {'a': 1}) == {'a': 1}

    def test_update_lists_not_a_list_empty(self):
        assert update({'a': 1}, {'a': []}) == {'a': []}

    def test_update_empty_not_empty1(self):
        assert update({'a': []}, {'a': [1]}) == {'a': [1]}

    def test_update_empty_not_empty2(self):
        assert update({'a': []}, {'a': [1, 2]}) == {'a': [1, 2]}

    def test_update_both_not_empty1(self):
        assert update({'a': [1, 2]}, {'a': [1, 2]}) == {'a': [1, 2]}

    def test_update_both_not_empty2(self):
        assert update({'a': [1]}, {'a': [1, 2]}) == {'a': [1, 2]}

    def test_update_both_not_empty3(self):
        assert update({'a': [1, 3]}, {'a': [1, 2]}) == {'a': [3, 1, 2]}

    def test_update_both_not_empty4(self):
        assert update({'a': [1, 2]}, {'a': [1, 3]}) == {'a': [2, 1, 3]}

    def test_update_nested_lists_empty_empty(self):
        assert update({'a': {'b': {'c': []}}}, {'a': {'b': {'c': []}}}) == \
            {'a': {'b': {'c': []}}}

    def test_update_nested_lists_empty_not_a_list(self):
        assert update({'a': {'b': {'c': []}}}, {'a': {'b': {'c': 1}}}) == \
            {'a': {'b': {'c': 1}}}

    def test_update_nested_lists_not_a_list_empty(self):
        assert update({'a': {'b': {'c': 1}}}, {'a': {'b': {'c': []}}}) == \
            {'a': {'b': {'c': []}}}

    def test_update_nested_empty_not_empty1(self):
        assert update({'a': {'b': {'c': []}}}, {'a': {'b': {'c': [1]}}}) == \
            {'a': {'b': {'c': [1]}}}

    def test_update_nested_empty_not_empty2(self):
        assert update({'a': {'b': {'c': []}}}, {'a': {'b': {'c': [1, 2]}}}) == \
            {'a': {'b': {'c': [1, 2]}}}

    def test_update_nested_both_not_empty1(self):
        assert update({'a': {'b': {'c': [1, 2]}}}, {'a': {'b': {'c': [1, 2]}}}) == \
            {'a': {'b': {'c': [1, 2]}}}

    def test_update_nested_both_not_empty2(self):
        assert update({'a': {'b': {'c': [1]}}}, {'a': {'b': {'c': [1, 2]}}}) == \
            {'a': {'b': {'c': [1, 2]}}}

    def test_update_nested_both_not_empty3(self):
        assert update({'a': {'b': {'c': [1, 3]}}}, {'a': {'b': {'c': [1, 2]}}}) == \
            {'a': {'b': {'c': [3, 1, 2]}}}

    def test_update_nested_both_not_empty4(self):
        assert update({'a': {'b': {'c': [1, 2]}}}, {'a': {'b': {'c': [1, 3]}}}) == \
            {'a': {'b': {'c': [2, 1, 3]}}}

    def test_update_different_types1(self):
        assert update({'a': 1}, {'a': 'a'}) == {'a': 'a'}

    def test_update_different_types2(self):
        assert update({'a': 1}, {'a': set()}) == {'a': set()}

    def test_update_different_types3(self):
        assert update({'a': set()}, {'a': 1}) == {'a': 1}

    def test_update_different_types4(self):
        assert update({'a': set()}, {'a': []}) == {'a': []}

    def test_update_different_types5(self):
        assert update({'a': 1}, {'a': []}) == {'a': []}

    def test_update_different_types6(self):
        assert update({'a': []}, {'a': 1}) == {'a': 1}

    def test_update_complex(self):
        assert update(
            {
                'a': {
                    'b': {
                        'x': set([1, 2]),
                        'y': 'test',
                        'z': [],
                    },
                    'c': [1, 2, 3],
                    'd': {},
                },
                'x': 'k',
            },
            {
                'a': {
                    'b': {
                        'x': set(),
                        'y': '',
                        'z': [1],
                    },
                    'c': [5],
                    'd': 'x',
                },
                'x': {},
            },
        ) == {
            'a': {
                'b': {
                    'x': set(),
                    'y': '',
                    'z': [1],
                },
                'c': [1, 2, 3, 5],
                'd': 'x',
            },
            'x': {},
        }
