from unittest import TestCase

from plan.common.utils.collection import mapping


class GroupByAsDictTest(TestCase):
    def setUp(self):
        class Initer(object):
            def __init__(self, **kwargs):
                self.__dict__.update(kwargs)

            def __repr__(self):
                return str(self.__dict__)

        self.obj_1 = Initer(a=1, b=10)
        self.obj_2 = Initer(a=1, b=10)
        self.obj_3 = Initer(a=1, b=20)

    def test_group_by_one_key(self):
        self.assertEqual(
            mapping.groupby_as_dict(
                iterable=[self.obj_1, self.obj_2, self.obj_3],
                keys=['a']
            ),
            {
                1: [self.obj_1, self.obj_2, self.obj_3],
            }
        )

    def test_group_by_two_keys(self):
        self.assertEqual(
            mapping.groupby_as_dict(
                iterable=[self.obj_1, self.obj_2, self.obj_3],
                keys=['a', 'b']),
            {
                (1, 10): [self.obj_1, self.obj_2],
                (1, 20): [self.obj_3],
            }
        )
