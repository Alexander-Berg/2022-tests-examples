from builtins import object
from collections import Counter
from fractions import Fraction

import pytest
from mock import call
from past.utils import old_div

from kelvin.common.utils import OrderedCounter
from kelvin.problems.checkers import CommonChecker, DragChecker, InlineChecker

EPSILON = 0.00000001


@pytest.fixture
def common_checker():
    return CommonChecker()


@pytest.fixture
def drag_checker():
    """Экземпляр чекера перетаскивания"""
    choices = [
        {'id': 1, 'value': 10},
        {'id': 'qwe', 'value': 89},
    ]
    return DragChecker(choices, 'answer')


@pytest.fixture
def inline_checker():
    """Экземпляр чекера инлайна"""
    return InlineChecker([], 'expressions', 'answer')


@pytest.fixture
def inline_checker_rational():
    """Экземпляр чекера инлайна типа rational"""
    return InlineChecker(
        {'1': {'type': 'rational'}},
        'tree',
        {}
    )


class TestCommonChecker(object):
    """Тест общих проверок"""

    def test_definotions(self):
        definitions = CommonChecker.definitions()
        assert definitions == CommonChecker.COMMON_DEFINITIONS
        assert definitions is not CommonChecker.COMMON_DEFINITIONS

    def test_check_tree(self, mocker):
        """Тест шага прохождения по дереву выражения"""
        mocked_equal = mocker.patch.object(CommonChecker, 't_EQUAL')
        mocked_equal.return_value = 'equal'
        mocked_not = mocker.patch.object(CommonChecker, 't_NOT')
        mocked_not.return_value = 'not'
        mocked_none = mocker.patch.object(CommonChecker, 't_NONE')
        mocked_none.return_value = 'none'
        mocked_number = mocker.patch.object(CommonChecker, 't_NUMBER')
        mocked_number.return_value = 'number'
        tree = {
            'type': 'EQUAL',
            'sources': [
                {
                    'type': 'NOT',
                    'source': {
                        'type': 'NONE',
                    },
                },
                {
                    'type': 'NUMBER',
                    'source': 2,
                }
            ],
        }
        checker = CommonChecker()
        assert checker.check_tree(tree) == 'equal'
        assert mocked_none.mock_calls == [call()]
        assert mocked_number.mock_calls == [call(2)]
        assert mocked_not.mock_calls == [call('none')]
        assert mocked_equal.mock_calls == [call('not', 'number')]

    def test_none(self):
        assert CommonChecker().t_NONE() is None

    def test_t_boolean(self, common_checker):
        assert common_checker.t_BOOLEAN(1) is True
        assert common_checker.t_BOOLEAN(False) is False

    def test_t_number(self, common_checker):
        assert common_checker.t_NUMBER('2') == 2
        assert common_checker.t_NUMBER(3.14) == 3.14

    def test_t_rational(self, common_checker):
        assert common_checker.t_RATIONAL('1 2/9') == Fraction(11, 9)
        assert common_checker.t_RATIONAL('3.14') == Fraction(157, 50)

    def test_t_string(self, common_checker):
        assert common_checker.t_STRING('derp') == u'derp'
        assert isinstance(common_checker.t_STRING('derp'), str)

    def test_t_union(self, common_checker):
        assert common_checker.t_UNION(1, 2, 3) == {1, 2, 3}

    @pytest.mark.parametrize(
        'args,result',
        (
            ([1, 1, 3], Counter({1: 2, 3: 1})),
            ([1, 1.0, 3], Counter({1: 2, 3: 1})),
            ([1, '1', 3], Counter({1: 1, 3: 1, '1': 1})),
            ([True, 1, 3], Counter({1: 2, 3: 1})),
        ),
    )
    def test_t_multiunion(self, common_checker, args, result):
        assert common_checker.t_MULTIUNION(*args) == result

    @pytest.mark.parametrize(
        'args,result',
        (
            # Проверяем автоматическое приведение к OrderedCounter, далее будем
            # явно использовать его
            (
                (
                    Counter([1]),
                    Counter([
                        frozenset([1]),
                    ]),
                ),
                True,
            ),
            (
                (
                    Counter([1, 2]),
                    Counter([
                        frozenset([1]),
                    ]),
                ),
                False,
            ),
            (
                (
                    OrderedCounter([2, 4, 3]),
                    OrderedCounter([
                        frozenset([1, 2, 3]),
                        frozenset([2, 4]),
                        frozenset([2, 3]),
                    ]),
                ),
                True,
            ),
            (
                (
                    OrderedCounter([2, 4, 1]),
                    OrderedCounter([
                        frozenset([1, 3]),
                        frozenset([2, 4]),
                        frozenset([1]),
                    ]),
                ),
                False,
            ),
            # Проверка на одинаковые значения. Из-за Counter они сольются в
            # один и индексы у них будут рядом с первым вхождением.
            # Пример: [1, 4, 1, 3, 4] --> [1, 1, 4, 4, 3]
            (
                (
                    OrderedCounter([2, '2', 4, 3, 4]),  # [2, '2', 4, 4, 3]
                    OrderedCounter([
                        frozenset([1, 2, 3]),       # 0
                        frozenset([2, 4]),          # 1
                        frozenset(['2', 3]),        # 3
                        frozenset([3, 4, '2']),     # 4
                        frozenset([2, 4]),          # 2
                    ]),
                ),
                True,
            ),
            # Проверяем автоматическое отрубание при длине > 50
            (
                (
                    OrderedCounter({1: 100}),
                    OrderedCounter([frozenset([1])]),
                ),
                False,
            ),
            (
                (
                    OrderedCounter([1]),
                    OrderedCounter([frozenset([1])] * 100),
                ),
                False,
            ),
            # Простой тест
            (
                (
                    OrderedCounter([0, 1, 2, 3]),
                    OrderedCounter([
                        frozenset([0, 3]),
                        frozenset([0, 1]),
                        frozenset([1, 2]),
                        frozenset([2]),
                    ]),
                ),
                True,
            ),
            # Поддержка разных типов данных
            (
                (
                    OrderedCounter([False, 1, '2', 3., Fraction(2, 3)]),
                    OrderedCounter([
                        frozenset([False, 3.]),
                        frozenset([False, 1]),
                        frozenset([1, '2']),
                        frozenset(['2']),
                        frozenset([Fraction(2, 3), 3.]),
                    ]),
                ),
                True,
            ),
            # Поддержка пустых ответов
            (
                (
                    OrderedCounter([1, 2, None, 1, None, None]),
                    OrderedCounter([
                        frozenset([None]),
                        frozenset([1, 2]),
                        frozenset([1, 2]),
                        frozenset([1, 2]),
                        frozenset([None]),
                        frozenset([None]),
                    ]),
                ),
                True,
            ),
            (
                (
                    OrderedCounter([1, 2, None, 1, 2, None]),
                    OrderedCounter([
                        frozenset([None]),
                        frozenset([1, 2]),
                        frozenset([1, 2]),
                        frozenset([1, 2]),
                        frozenset([None]),
                        frozenset([None]),
                    ]),
                ),
                False,
            ),
        ),
    )
    def test_t_is_permutation_from(self, common_checker, args, result):
        assert common_checker.t_IS_PERMUTATION_FROM(*args) == result

    @pytest.mark.parametrize(
        'source,result',
        (
            ({0, 0, 0}, 1),
            ({1, 3, 1, 1, 1}, 2),
            ({1, 2, None, 3}, 3),
            (Counter({0: 3}), 3),
            (Counter({1: 1, 2: 3}), 4),
            (Counter({1: 5, None: 3}), 5),
        ),
    )
    def test_t_size(self, common_checker, source, result):
        assert common_checker.t_SIZE(source) == result

    def test_t_is_none(self, common_checker):
        assert common_checker.t_IS_NONE(None) is True
        assert common_checker.t_IS_NONE(False) is False

    def test_t_not(self, common_checker):
        assert common_checker.t_NOT(True) is False
        assert common_checker.t_NOT(0) is True
        assert common_checker.t_NOT(Fraction(0, 1)) is True

    def test_t_or(self, common_checker):
        assert common_checker.t_OR(False, False, True) is True
        assert common_checker.t_OR(2, 0) == 2

    def test_t_and(self, common_checker):
        assert common_checker.t_AND(False, False, True) is False
        assert common_checker.t_AND(2, 0) == 0

    def test_t_more(self, common_checker):
        assert common_checker.t_MORE(2, 3) is False
        assert common_checker.t_MORE(2, 1) is True
        assert common_checker.t_MORE(Fraction(1, 3), Fraction(1, 4)) is True

    def test_t_eqmore(self, common_checker):
        assert common_checker.t_EQMORE(2, 3) is False
        assert common_checker.t_EQMORE(2, 2) is True
        assert common_checker.t_EQMORE(Fraction(1, 2), Fraction(2, 4)) is True

    def test_t_less(self, common_checker):
        assert common_checker.t_LESS(4, 3) is False
        assert common_checker.t_LESS(1, 2) is True
        assert common_checker.t_LESS(Fraction(1, 4), Fraction(1, 3)) is True

    def test_t_eqless(self, common_checker):
        assert common_checker.t_EQLESS(4, 3) is False
        assert common_checker.t_EQLESS(2, 2) is True
        assert common_checker.t_EQLESS(Fraction(1, 2), Fraction(2, 4)) is True

    @pytest.mark.parametrize(
        'x,y,result',
        (
            (True, False, False),
            (True, 1, False),
            (1, 1, True),
            (1, 1.0, True),
            (1.2 * 100, 120, True),
            (old_div(123.0, 100.0), 1.23, True),
            (old_div(123.000001, 100), 1.23, False),
            (Fraction('1/7'), Fraction('0.1428'), False),
            (Fraction('12/24'), Fraction(1, 2), True),
            (Fraction(123, 123), Fraction(1, 1), True),
            (1, '1', False),
            ('1', '1.0', False),
            ({1, 2.0}, {1, 2}, True),
            ({2}, {1, 2}, False),
            ({1, 1, 2}, {1, 2, 2.0}, True),
            ({1, '2'}, {1, 2}, False),
            ({1, Fraction(1, 2)}, {1, Fraction(12, 24)}, True),
            ({1: 2, 2: 1}, {1: 1, 2: 2}, False),
            ({1: 1, 2.0: 1}, {1: 1, 2: 1}, True),
            ({1: 1, 2.0: Fraction(1, 2)}, {1: 1, 2: Fraction(2, 4)}, True),
            ({1: 1}, {1}, False),
        ),
    )
    def test_t_equal(self, common_checker, x, y, result):
        assert common_checker.t_EQUAL(x, y) is result

    @pytest.mark.parametrize(
        'x,y,result',
        (
            ({1}, {1, 2}, True),
            ({1, 2, 3}, {1, 2}, False),
            ({Fraction(1, 2), 2}, {Fraction(2, 4), 1, 2}, True),
            ({1, 2, 3, 4}, {1, 2, Fraction(9, 2)}, False),
            (Counter({1: 1}), {1, 2}, False),
            (Counter({1: 1}), Counter({1: 2}), True),
            (Counter({1: 1, 2: 4}), Counter({1: 2}), False),
            (Counter({'1': 1}), Counter({1: 2}), False),
        ),
    )
    def test_t_is_subset(self, common_checker, x, y, result):
        assert common_checker.t_IS_SUBSET(x, y) is result

    @pytest.mark.parametrize(
        'x,y,result',
        (
            ({1, 3}, {1, 2}, True),
            ({1, 2, 3}, {1, 2}, False),
            ({1, 2}, {Fraction(1, 2), 2}, True),
            ({Fraction(4, 8), 2}, {Fraction(1, 2), 2}, False),
            (Counter({1: 1}), {1, 2}, False),
            (Counter({1: 1}), Counter({1: 2}), False),
            (Counter({1: 1, 2: 4}), Counter({1: 2}), True),
            (Counter({'1': 1}), Counter({1: 2}), True),
        ),
    )
    def test_t_unique_items(self, common_checker, x, y, result):
        assert common_checker.t_UNIQUE_ITEMS(x, y) is result

    @pytest.mark.parametrize(
        'args,result',
        (
            ([2, 3, 4], 9),
            ([None, None], 0),
            ([None, 5], 5),
            ([Fraction(1, 2), Fraction(1, 3)], Fraction(5, 6)),
        ),
    )
    def test_t_sum(self, common_checker, args, result):
        assert common_checker.t_SUM(*args) == result

    @pytest.mark.parametrize(
        'x,y,result',
        (
            (5, 3, 2),
            (0, 0, 0),
            (0, 5, -5),
            (3.1, 3.0, .1),
            (Fraction(4, 9), Fraction(1, 3), Fraction(1, 9)),
            (Fraction(-4, 9), Fraction(1, 3), Fraction(-7, 9)),
        ),
    )
    def test_t_minus(self, common_checker, x, y, result):
        assert abs(common_checker.t_MINUS(x, y) - result) < EPSILON

    @pytest.mark.parametrize(
        'args,result',
        (
            ([2, 3, 4], 24),
            ([None, None], 0),
            ([None, 9], 0),
            ([Fraction(3, 5), Fraction(2, 4)], Fraction(3, 10)),
            ([Fraction(1, 2), None], 0),
        ),
    )
    def test_t_mult(self, common_checker, args, result):
        assert common_checker.t_MULT(*args) == result

    def test_t_different(self, common_checker):
        assert common_checker.t_DIFFERENT(2, 3, 10) is True
        assert common_checker.t_DIFFERENT(2, 3, 10, 3) is False
        assert common_checker.t_DIFFERENT(2, 3, 10, Fraction(12, 4)) is False

    @pytest.mark.parametrize(
        'x,y,result',
        (
            (7, 5, 2),
            (5, 7, 5),
            (0, 7, 0),
            (7, 0, None),
            (0, 0, None),
            (3.1, 1, None),
            (-10, 8, 6),
            (10, -8, 2),
            (-10, -8, 6),
            (10, 4.1, None),
            (10, None, None),
            (None, 4, None),
        ),
    )
    def test_t_remainder(self, common_checker, x, y, result):
        assert common_checker.t_REMAINDER(x, y) == result

    @pytest.mark.parametrize(
        'x,y,result',
        (
            (7, 5, 1),
            (5, 7, 0),
            (12, 5, 2),
            (0, 7, 0),
            (7, 0, None),
            (0, 0, None),
            (3.1, 1, None),
            (10, 3.1, None),
            (10, None, None),
            (None, 4, None),
            (-16, 8, -2),
            (-17, 8, -3),
            (-23, 8, -3),
            (-23, -8, 3),
            (-16, -8, 2),
            (-16, -5, 4),
            (16, -8, -2),
            (23, -8, -2),
            (7, -8, 0),
        ),
    )
    def test_t_quotient(self, common_checker, x, y, result):
        assert common_checker.t_QUOTIENT(x, y) == result


class TestDragChecker(object):
    """Тесты проверки маркеров перетаскивания"""

    def test_definitions(self):
        definitions = DragChecker.definitions()
        assert definitions.pop('FIELD')
        assert (definitions['EXPRESSION']['oneOf'].pop(-1) ==
                {'$ref': '#/check_definitions/FIELD'})
        assert (definitions['OPERATORS']['oneOf'].pop(-1) ==
                {'$ref': '#/check_definitions/FIELD'})

    def test_init(self, drag_checker):
        """Тест инициализации"""
        assert drag_checker.answer == 'answer'
        assert drag_checker.choices == {1: 10, 'qwe': 89}

    def test_t_field(self):
        """Тест получения значения из ответа пользователя"""
        choices = [
            {'id': 3, 'value': 10},
            {'id': 4, 'value': 20},
        ]
        answer = {
            '1': 4,
        }
        checker = DragChecker(choices, answer)

        # найденный вариант
        assert checker.t_FIELD(1) == 20

        # ненайденный вариант
        assert checker.t_FIELD(2) is None


class TestInlineChecker(object):
    """Тест проверки инлайн-маркера"""

    def test_definitions(self):
        definitions = InlineChecker.definitions()
        assert definitions.pop('INPUT')
        assert (definitions['EXPRESSION']['oneOf'].pop(-1) ==
                {'$ref': '#/check_definitions/INPUT'})
        assert (definitions['OPERATORS']['oneOf'].pop(-1) ==
                {'$ref': '#/check_definitions/INPUT'})

    def test_init(self):
        """Тест инициализации проверки"""
        checker = InlineChecker('inputs', 'expressions', 'answer')
        assert checker.expressions == 'expressions'
        assert checker.answer == 'answer'
        assert checker.inputs == 'inputs'

    def test_check(self, mocker):
        """Тест запуска проверки"""
        mocked_check_tree = mocker.patch.object(InlineChecker, 'check_tree')

        # Случай обычной работы
        mocked_check_tree.return_value = True
        forest = {
            '1': 'tree1',
            '2': 'tree2',
        }
        checker = InlineChecker([], forest, 'answer')

        assert checker.check() == {
            '1': True,
            '2': True,
        }, u'Неправильный ответ'
        assert mocked_check_tree.mock_calls == [
            call('tree1'),
            call('tree2'),
        ], u'Нужно проверить каждое дерево'

        # Случай, когда на инпут не было ответа
        mocked_check_tree.reset_mock()
        mocked_check_tree.side_effect = InlineChecker.MissingAnswerException

        assert checker.check() == {
            '1': False,
            '2': False,
        }, u'Неправильный ответ'
        assert mocked_check_tree.mock_calls == [
            call('tree1'),
            call('tree2'),
        ], u'Нужно проверить каждое дерево'

        # Случай неудавшегося приведения к числу
        mocked_check_tree.reset_mock()
        mocked_check_tree.side_effect = InlineChecker.InvalidNumberException

        assert checker.check() == {
            '1': False,
            '2': False,
        }, u'Неправильный ответ'
        assert mocked_check_tree.mock_calls == [
            call('tree1'),
            call('tree2'),
        ], u'Нужно проверить каждое дерево'

    def test_t_input(self):
        """Проверяем как нормальное получение ввода, так и исключение"""
        answer = {
            '1': 4,
            '2': None,
            '4': '3,14',
            '5': u'  Нормалёзация ',
            '6': u'Нечисло',
            '7': '(6,28)',
            '8': 'English text',
            '9': ' qwe \nr\tty'
        }
        checker = InlineChecker(
            {
                '1': {'type': 'choice'},
                '2': {'type': 'separator'},
                '4': {
                    'type': 'field',
                    'options': {'type_content': 'number'},
                },
                '5': {
                    'type': 'field',
                    'options': {'type_content': 'text'},
                },
                '6': {
                    'type': 'field',
                    'options': {'type_content': 'number'},
                },
                '7': {
                    'type': 'field',
                    'options': {'type_content': 'number'},
                },
                '8': {
                    'type': 'field',
                    'options': {'type_content': 'strict'},
                },
                '9': {
                    'type': 'field',
                    'options': {'type_content': 'spaceless'},
                },
            },
            'tree',
            answer,
        )

        # Найденный ввод
        assert checker.t_INPUT(1) == 4

        # Ввод `None`
        assert checker.t_INPUT(2) is None

        # Ненайденный ввод
        with pytest.raises(InlineChecker.MissingAnswerException) as e:
            checker.t_INPUT(3)

        # Правильное число
        assert checker.t_INPUT(4) == 3.14

        # Нормализация строки
        assert checker.t_INPUT(5) == u'нормалезация'

        # Неправильное число
        with pytest.raises(InlineChecker.InvalidNumberException) as e:
            assert checker.t_INPUT(6)

        # Число в скобках
        assert checker.t_INPUT(7) == 6.28

        # Строка без преобразований
        assert checker.t_INPUT(8) == 'English text'

        # Строка с удаленными пробельными символами
        assert checker.t_INPUT(9) == 'qwerty'

        # Нет ввода
        with pytest.raises(InlineChecker.MissingAnswerException) as e:
            checker.t_INPUT(10)

    @pytest.mark.parametrize(
        'answer,result',
        (
            # Тесты для целых чисел
            ('1', Fraction(1, 1)),
            ('-1', Fraction(-1, 1)),
            ('0', Fraction(0, 1)),
            ('1234567890', Fraction(1234567890, 1)),
            ('        123     ', Fraction(123, 1)),
            # Тесты десятичной формы записи дроби
            ('1.0', Fraction(1, 1)),
            ('1.2', Fraction(6, 5)),
            ('-1.2', Fraction(-6, 5)),
            ('-0.5', Fraction(-1, 2)),
            ('12345.54321', Fraction(1234554321, 100000)),
            ('1,2', Fraction(6, 5)),
            ('    1.2    ', Fraction(6, 5)),
            # Тесты простой формы записи дроби
            ('4/2', Fraction(2, 1)),
            ('-4/2', Fraction(-2, 1)),
            ('2 4/2', Fraction(4, 1)),
            ('-1 4/2', Fraction(-3, 1)),
            ('   4      /    2   ', Fraction(2, 1)),
            (' -1   4  /   2  ', Fraction(-3, 1)),
        )
    )
    def test_t_input_rational(self, inline_checker_rational, answer, result):
        """Проверяем инпут с дробями"""
        inline_checker_rational.answer['1'] = answer
        assert inline_checker_rational.t_INPUT(1) == result

    @pytest.mark.parametrize(
        'answer',
        (
            # Тесты на общие ошибки
            '1 000 000',
            '111 22',
            '1e2',
            '0123',
            '-0',
            '+1234',
            '',
            # Тесты на ошибки в десятичной форме записи дроби
            '1 .2',
            '1. 2',
            '1 , 2',
            '  1  . 2  ',
            '000.123',
            '1.2.2',
            '1.2,2',
            '1 1.2',
            '- 1.2',
            '1.(2)',
            '(1.2)',
            '1.',
            '1,',
            '.2',
            ',2',
            '+1.2',
            # Тесты на ошибки в простой форме записи дроби
            '-1/-2',
            '0/0',
            '0000/0',
            '0/000',
            '0/999999',
            '0/123',
            '-0/123',
            '1 -1/2',
            '0 1/2',
            '-0 1/2',
            '1.2 1/2',
            '1.2/1.2',
            '+1/2',
            '+1 1/2',
        )
    )
    def test_t_input_rational_errors(self, inline_checker_rational, answer):
        """Проверяем ошибки в инпуте с дробями"""
        inline_checker_rational.answer['1'] = answer
        with pytest.raises(InlineChecker.InvalidRationalException) as e:
            assert inline_checker_rational.t_INPUT(1)
