# -*- coding: utf-8 -*-

from testutils import (
    TestCase,
)

from intranet.yandex_directory.src.yandex_directory.core.predicates import (
    And,
    Or,
    Not,
    Is,
    Always,
)


class TestPredicates(TestCase):
    def setUp(self):
        super(TestPredicates, self).setUp()
        self.cache = {}

    def predicate_1(self, obj, **kwargs):
        return 'foo' in obj

    def predicate_2(self, obj, **kwargs):
        return 'bar' in obj

    def predicate_3(self, num, **kwargs):
        return num == 1

    def test_bool_predicate(self):
        # Проверяем, что работают простые булевы предикаты
        bool_predicate = True
        assert not Not(bool_predicate).evaluate(self.cache)

    def test_always(self):
        obj = {'foo', 'bar'}
        predicate = Always()
        assert predicate.evaluate(self.cache, obj=obj), 'Always should return True on each evaluation.'

    def test_and(self):
        # Проверяем, что в сете содержатся строки 'foo' и 'bar'
        obj = {'foo', 'bar'}
        assert And(self.predicate_1, self.predicate_2).evaluate(self.cache, obj=obj)

    def test_or(self):
        # Проверяем, что в сете содержится либо строка 'foo', либо 'bar'
        obj = {'foo','test'}
        assert Or(self.predicate_1, self.predicate_2).evaluate(self.cache, obj=obj)

    def test_not(self):
        # Проверяем, что в сете не содержится строка 'bar'
        obj = {'test'}
        assert Not(self.predicate_2).evaluate(self.cache, obj=obj)

    def test_is(self):
        # Проверяем, что в сете содержится строка 'foo'
        obj = {'foo'}
        assert Is(self.predicate_1).evaluate(self.cache, obj=obj)

    def test_all(self):
        # либо в сете содержится строка 'foo' и не содержится строка 'bar'
        # либо проверяемое число - 1
        obj1 = {'foo'}
        obj2 = {'bar'}

        # тут в obj есть 'foo' и нет 'bar'
        assert Or(
            And(
                self.predicate_1,
                Not(self.predicate_2),
            ),
            self.predicate_3
        ).evaluate(self.cache, obj=obj1, num=2)
        # тут num = 1
        assert Or(
            And(
                self.predicate_1,
                Not(self.predicate_2),
            ),
            self.predicate_3
        ).evaluate(self.cache, obj=obj2, num=1)
