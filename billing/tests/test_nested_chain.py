from dwh.grocery.tools.more_itertools import nested_chain


class TestNestedChain:

    def test_identity(self):
        a = [1, 2, 3]
        assert set(nested_chain(a)) == set(a)

    def test_nested_lists(self):
        a = [1, [2, 3], [4, [5, 6]]]
        assert set(nested_chain(a)) == {1, 2, 3, 4, 5, 6}

    def test_nested_dicts(self):
        a = [1, {'a': 2, 'b': 3}, {'a': 4, 'b': {'a': 5, 'b': 6}}]
        assert set(nested_chain(a)) == {1, 2, 3, 4, 5, 6}

    def test_nested_mix(self):
        a = [1, [2, 3], {'a': 4, 'b': {'a': 5, 'b': 6}}]
        assert set(nested_chain(a)) == {1, 2, 3, 4, 5, 6}

    def test_cost_by_page_struct(self):
        a = {'a': 1, 'b': 2, 'c': [3, 4, 5]}
        r = set(nested_chain(a))
        assert r == {1, 2, 3, 4, 5}, r
