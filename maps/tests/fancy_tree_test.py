# import yatest
import io

import gold_data_manager
from maps.infra.sedem.lib import deprecated_fancy_tree as fancy_tree


def create_fancy_tree(init, msg):
    result = fancy_tree.FancyTree()
    result.load_dict(init, msg)
    return result


def create_fancy_tree1():
    return create_fancy_tree({
        'Entry1': 1,
        'Entry2': [
            1, 2, '33'
        ],
        '$default Ent4': 4,
        'Entry33': {}
    }, "Test dict #1")


def create_fancy_tree2():
    return create_fancy_tree({
        'Entry2': [
            'Extend entry 2'
        ],
        'Entry3': 'Brife new entry'
    }, "Test dict #2")


def create_fancy_tree3():
    return create_fancy_tree({
        'Root':
            [
                {
                    'Entry1': 1,
                    'SomeBody': 2
                },
                {
                    'Entry1': 2,
                    'SomeBody': 3
                },
                {
                    'Entry1': 3,
                    'SomeBody': 'body'
                }
            ]}, "Org dict")


class TestFancyTree(gold_data_manager.TestBase):

    def check_dump(self, value_to_check):
        if isinstance(value_to_check, fancy_tree.FancyTree):
            stream = io.StringIO()
            value_to_check.dump(stream)
            value = stream.getvalue()
        else:
            value = str(value_to_check)
        self.check(value)

    def test_fancydict_create(self):
        fancy_tree = create_fancy_tree1()
        self.check_dump(fancy_tree)

    def test_fancydict_join_simple(self):
        fancy_tree = create_fancy_tree1()
        fancy_tree2 = create_fancy_tree2()
        fancy_tree.join(fancy_tree2)
        self.check_dump(fancy_tree)

    def test_fancydict_join_on_path(self):
        fancy_tree = create_fancy_tree1()
        fancy_tree2 = create_fancy_tree2()
        fancy_tree.join(fancy_tree2, "Entry33")
        self.check_dump(fancy_tree)

    def test_fancydict_join_on_path2(self):
        fancy_tree = create_fancy_tree1()
        fancy_tree2 = create_fancy_tree2()
        fancy_tree.join(fancy_tree2, "Entry33")
        self.check_dump(fancy_tree('Entry2.1'))

    def test_fancydict_filter_simple(self):
        fancy_tree = create_fancy_tree3()
        filter_simple = create_fancy_tree(
            {
                'filter': {
                    'Entry1': 1
                },
                'body': {
                    'AddedByFilter': 'hit!'
                }
            }, 'Filter simple')
        fancy_tree.clone()
        fancy_tree.filter(filter_simple, 'Root')
        self.check_dump(fancy_tree)

    def test_fancydict_filter_simple2(self):
        fancy_tree = create_fancy_tree3()
        filter_simple = create_fancy_tree(
            {
                'filter': {
                    'Entry1': 1
                },
                'body': {
                    'AddedByFilter': 'hit!',
                }
            }, 'Filter simple')
        cloned = fancy_tree.clone()
        fancy_tree.filter(filter_simple, 'Root')
        self.check_dump(cloned)

    def test_fancydict_spckeys(self):
        # Join special keys
        fancy_tree = create_fancy_tree({
            'Simple entry': 1,
            'Array entry': [
                1, 2, '33'
            ],
            '$default Ent4': 4,
            'Dict entry': {
                'Ent1': [1, 2, 3],
                'Ent2': '123'
            }
        }, "Dict for join test")

        fancy_join = create_fancy_tree({
            '$force Simple entry': [111, 222, 333],
            'real new key': 'real new value',
        }, 'Fancy join')

        fancy_tree.join(fancy_join)
        self.check_dump(fancy_tree)

    def test_fancydict_join_empty(self):
        fancy_tree = create_fancy_tree1()
        fancy_tree.load_dict(None, 'Empty data', 'Entry2')
        self.check_dump(fancy_tree)
