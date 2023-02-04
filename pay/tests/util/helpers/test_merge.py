from paysys.sre.tools.monitorings.lib.util.helpers import merge


class TestMerge:
    def test_empty_merge(self):
        assert merge() == {}

    def test_one_object_merge(self):
        assert merge({'a': 123}) == {'a': 123}

    def test_two_objects_merge(self):
        assert merge({'a': 123}, {'b': 234}) == {'a': 123, 'b': 234}

    def test_merge_with_replace(self):
        assert merge({'a': 123}, {'a': "Replaced"}) == {'a': "Replaced"}

    def test_chain_merge_with_replace(self):
        assert merge(
            {'a': 123}, {'a': 312}, {'b': "New key"}, {'a': "Replaced"}
        ) == {'a': "Replaced", 'b': "New key"}

    def test_merge_two_complicant_objects(self):
        assert merge(
            {'a': {'b': {'c': [1, 2]}}},
            {'a': {'b': {'c': [1, 3]}}},
            {'b': {'b': {'c': [1, 2, 3, 4]}},
             'c': None,
             'd': set()
             },
            {'a': {'b': {'d': True}}}
        ) == {
            'a': {'b': {
                'c': [2, 1, 3],
                'd': True
                }
            },
            'b': {'b': {'c': [1, 2, 3, 4]}},
            'c': None,
            'd': set()
        }
