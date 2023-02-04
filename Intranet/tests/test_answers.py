from builtins import object
from copy import deepcopy

import pytest

from kelvin.problems.answers import Answer, check_answer
from kelvin.problems.markers import Marker
from kelvin.problems.models import Problem


class TestAnswer(object):
    """
    Тесты модели ответа
    """
    init_data = (
        (
            ('1', '1'),
            {},
            '1',
            True,
        ),
        (
            tuple(),
            {'markers': '1', 'theory': '1'},
            '1',
            True,
        ),
        (
            tuple(),
            {'markers': '1', 'theory': '1', 'completed': False},
            '1',
            False,
        ),
    )

    @pytest.mark.parametrize('args,kwargs,expected_markers,expected_completed',
                             init_data)
    def test_init(self, args, kwargs, expected_markers, expected_completed):
        """
        Проверяем аттрибуты объекта ответа
        """
        with pytest.raises(TypeError):
            Answer()

        answer = Answer(*args, **kwargs)
        assert hasattr(answer, 'markers'), u'Должен быть аттрибут маркеров'
        assert hasattr(answer, 'theory'), u'Должен быть аттрибут теории'
        assert hasattr(answer, 'mistakes'), u'Должен быть аттрибут ошибок'
        assert hasattr(answer, 'max_mistakes'), u'Должен быть аттрибут ошибок'
        assert hasattr(answer, 'completed'), (
            u'Должен быть аттрибут завершенности попытки')
        assert answer.markers == expected_markers, (
            u'Неправильное значение маркеров')
        assert answer.completed is expected_completed, (
            u'Неправильное значение завершенности попытки')

    get_summary_data = (
        # нет ответа
        (
            None,
            2,
            False,
            False,
            {
                'answered': False,
                'status': None,
            },
        ),
        # нет ответа на незавершенную контрольную
        (
            None,
            2,
            False,
            True,
            {
                'answered': True,
                'status': Answer.SUMMARY_INCORRECT,
            },
        ),
        # ответ не завершен
        (
            [
                {
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': False,
                    'spent_time': 1,
                    'points': 10,
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
            2,
            False,
            False,
            {
                'answered': False,
                'status': None,
                'time': 1,
                'attempt_number': 0,
                'max_attempts': 2,
                'points': 10,
            },
        ),
        # Ответ не завершен в пройденной контрольной
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': False,
                    'spent_time': 2,
                    'points': 0,
                    'answered': True,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
            3,
            False,
            True,
            {
                'answered': True,
                'status': Answer.SUMMARY_INCORRECT,
                'time': 2,
                'attempt_number': 0,
                'max_attempts': 3,
                'points': 0,
            },
        ),
        # правильная попытка
        (
            [
                {
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 1,
                    'points': 10,
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
            3,
            False,
            False,
            {
                'answered': True,
                'status': Answer.SUMMARY_CORRECT,
                'time': 1,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 10,
            },
        ),
        # неправильная попытка, затем правильная
        (
            [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                },
                {
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 1,
                    'points': 10,
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
            3,
            False,
            False,
            {
                'answered': True,
                'status': Answer.SUMMARY_CORRECT,
                'time': 3,
                'attempt_number': 2,
                'max_attempts': 3,
                'points': 10,
            },
        ),
        # неправильная попытка
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
            3,
            False,
            False,
            {
                'answered': True,
                'status': Answer.SUMMARY_INCORRECT,
                'time': 2,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 0,
            },
        ),
        # пропущенный вопрос
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
            3,
            False,
            False,
            {
                'answered': True,
                'status': Answer.SUMMARY_INCORRECT,
                'time': 2,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 0,
            },
        ),
        # вопрос требующий проверки
        (
            [
                {
                    'mistakes': None,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': None,
                            'max_mistakes': 1,
                            'answer_status': 2,
                        },
                    },
                },
            ],
            3,
            False,
            False,
            {
                'answered': False,
                'status': None,
                'time': 2,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 0,
            },
        ),
        # отвеченный вопрос текущей контрольной
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'answered': True,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
            3,
            True,
            False,
            {
                'answered': True,
                'status': None,
                'time': 2,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 0
            },
        ),
        # неотвеченный вопрос текущей контрольной
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'answered': False,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
            3,
            True,
            False,
            {
                'answered': False,
                'status': None,
                'time': 2,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 0
            },
        ),
        # ручная проверка. Ответ только от ученика
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': None,
                    'answered': False,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                    'custom_answer': [
                        {
                            'type': 'solution',
                            'message': 'Text',
                        },
                    ]
                },
            ],
            3,
            False,
            False,
            {
                'answered': False,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': None,
                'status': None,
                'time': 2
            },
        ),
        # ручная проверка. Учитель поставил отрицательный статус
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'answered': False,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                    'custom_answer': [
                        {
                            'type': 'solution',
                            'message': 'Text',
                        },
                        {
                            'type': 'check',
                            'message': 'Text',
                            'points': 2,
                            'status': Answer.SUMMARY_INCORRECT,
                        },
                    ]
                },
            ],
            3,
            False,
            False,
            {
                'answered': True,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 2,
                'status': Answer.SUMMARY_INCORRECT,
                'time': 2
            },
        ),
        # ручная проверка. Ученик ответил снова
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'answered': False,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                    'custom_answer': [
                        {
                            'type': 'solution',
                            'message': 'Text',
                        },
                        {
                            'type': 'check',
                            'message': 'Text',
                            'points': 2,
                            'status': Answer.SUMMARY_INCORRECT,
                        },
                    ]
                },
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 0,
                    'answered': False,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                    'custom_answer': [
                        {
                            'type': 'solution',
                            'message': 'Text',
                        },
                    ]
                },
            ],
            3,
            False,
            False,
            {
                'answered': False,
                'attempt_number': 2,
                'max_attempts': 3,
                'points': 0,
                'status': None,
                'time': 4
            },
        ),
        # ручная проверка. Учитель поставил положительный статус
        (
            [
                {
                    'mistakes': 2,
                    'max_mistakes': 2,
                    'completed': True,
                    'spent_time': 2,
                    'points': 10,
                    'answered': False,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                        '2': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                    'custom_answer': [
                        {
                            'type': 'solution',
                            'message': 'Text',
                            'status': Answer.SUMMARY_INCORRECT,
                        },
                        {
                            'type': 'check',
                            'message': 'Text',
                            'points': 0,
                        },
                        {
                            'type': 'check',
                            'message': 'Я ошибся в прошлый раз',
                            'points': 10,
                            'status': Answer.SUMMARY_CORRECT,
                        },
                    ]
                },
            ],
            3,
            False,
            False,
            {
                'answered': True,
                'attempt_number': 1,
                'max_attempts': 3,
                'points': 10,
                'status': Answer.SUMMARY_CORRECT,
                'time': 2
            },
        ),
    )

    @pytest.mark.parametrize('answers,max_attempts,hidden_results,'
                             'force_results,expected',
                             get_summary_data)
    def test_get_summary(self, answers, max_attempts, hidden_results,
                         force_results, expected):
        """
        Тест сводки ответов на вопрос
        """
        assert Answer.get_summary(
            answers, max_attempts, hidden_results=hidden_results,
            force_results=force_results
        ) == expected

    get_points_data = (
        ([], 0, 0, 0, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={})], 1, 1, 0, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=0, markers={})], 1, 1, 1,
         Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={}),
          dict(mistakes=0, markers={})], 2, 2, 2, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={}),
          dict(mistakes=1, markers={}),
          dict(mistakes=0, markers={})], 3, 3, 3,
         Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={}),
          dict(mistakes=0, markers={})], 3, 2, 2,
         Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={}),
          dict(mistakes=1, markers={}),
          dict(mistakes=0, markers={})], 3, 3, 3, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={}),
          dict(mistakes=1, markers={}),
          dict(mistakes=0, markers={})], 3, 3, 3, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={}),
          dict(mistakes=1, markers={}),
          dict(mistakes=1, markers={})], 3, 3, 0, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, markers={})], 3, 1, 0, Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, points=6, markers={})], 3, 1, 0,
         Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=1, checked_points=6, markers={})], 3, 1, 6,
         Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=None, markers={})], 3, 1, None,
         Answer.ATTEMPTS_COUNT_TYPE),
        ([dict(mistakes=3, markers={}),
          dict(mistakes=2, max_mistakes=5, markers={})], 2, 2, 2,
         Answer.MISTAKES_COUNT_TYPE),
        ([dict(mistakes=3, markers={}),
          dict(mistakes=5, max_mistakes=5, markers={})], 2, 2, 0,
         Answer.MISTAKES_COUNT_TYPE),
        (
            [
                dict(mistakes=3, markers={
                    '1': {'mistakes': 1},
                    '2': {'mistakes': 0},
                }),
            ],
            2,
            {'1': 1, '2': 1},
            1,
            Answer.ATTEMPTS_COUNT_TYPE,
        ),
        (
            [
                dict(mistakes=3, markers={
                    '1': {'mistakes': 1},
                    '2': {'mistakes': 0},
                }),
                dict(mistakes=3, markers={
                    '1': {'mistakes': 1},
                    '2': {'mistakes': 0},
                }),
            ],
            2,
            {'1': 1, '2': 1},
            1,
            Answer.ATTEMPTS_COUNT_TYPE,
        ),
        (
            [
                dict(mistakes=3, markers={
                    '1': {'mistakes': 1},
                    '2': {'mistakes': 2},
                }),
            ],
            2,
            {'1': 1, '2': 1},
            0,
            Answer.ATTEMPTS_COUNT_TYPE,
        ),
        (
            [
                dict(mistakes=3, markers={
                    '1': {'mistakes': 1, 'max_mistakes': 2},
                    '2': {'mistakes': 0, 'max_mistakes': 4},
                }),
            ],
            2,
            {'1': 1, '2': 1},
            2,
            Answer.MISTAKES_COUNT_TYPE,
        ),
        (
            [
                dict(mistakes=3, markers={
                    '1': {'mistakes': 1, 'max_mistakes': 1},
                    '2': {'mistakes': 2, 'max_mistakes': 4},
                }),
            ],
            2,
            {'1': 1, '2': 1},
            1,
            Answer.MISTAKES_COUNT_TYPE,
        ),
    )

    @pytest.mark.parametrize(
        (
            'answers',
            'max_attempts',
            'max_points',
            'expected',
            'count_type',
        ),
        get_points_data,
    )
    def test_get_points(
        self, answers, max_attempts, max_points, count_type, expected,
    ):
        """
        Тест подсчета баллов за вопрос
        """
        assert Answer.get_points(
            answers, max_attempts, max_points, count_type) == expected


check_answer_data = (
    (
        {
            'layout': [
                {
                    'content': {
                        'type': 'field',
                        'id': 1,
                        'options': {}
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': '3',
                '2': 'qwerty'
            }
        },
        {
            '1': {
                'user_answer': '3',
            },
            '2': {
                'user_answer': '3',
            },
        },
        {},
        {
            '1': {
                'user_answer': '3',
                'answer_status': 1,
                'mistakes': 0,
                'max_mistakes': 1,
            },
            '2': {
                'user_answer': '3',
                'answer_status': 0,
                'mistakes': 1,
                'max_mistakes': 1,
            },
        },
        False,
        1,
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'type': 'field',
                        'id': 1,
                        'options': {}
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': '2',
                '2': 'qwerty'
            }
        },
        {
            '1': {
                'user_answer': '2',
            },
            '2': {
                'user_answer': '3',
            },
        },
        {},
        {
            '1': {
                'user_answer': '2',
                'answer_status': 1,
                'mistakes': 0,
                'max_mistakes': 1,
            },
            '2': {
                'user_answer': '3',
                'answer_status': 0,
                'mistakes': 1,
                'max_mistakes': 1,
            },
        },
        False,
        1,
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'type': 'field',
                        'id': 1,
                        'options': {}
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': '3',
                '2': 'qwerty'
            }
        },
        {
            '1': {
                'user_answer': '3',
            },
            '2': {
                'user_answer': '3',
            },
        },
        {
            'mistakes': 1,
            'max_mistakes': 2,
        },
        {
            '1': {
                'user_answer': '3',
            },
            '2': {
                'user_answer': '3',
            },
        },
        True,
        1,
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'type': 'textandfile',
                        'id': 1,
                        'options': {}
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': None,
                '2': 'qwerty'
            }
        },
        {
            '1': {
                'user_answer': {'text': '3', 'file': ['link']},
            },
            '2': {
                'user_answer': '3',
            },
        },
        {},
        {
            '1': {
                'answer_status': Marker.UNCHECKED,
                'max_mistakes': 1,
                'mistakes': None,
                'user_answer': {'text': '3', 'file': ['link']},
            },
            '2': {
                'answer_status': Marker.INCORRECT,
                'max_mistakes': 1,
                'mistakes': 1,
                'user_answer': '3',
            },
        },
        False,
        None,
    ),
)


@pytest.mark.parametrize('markup,answer_markers,kwargs,'
                         'expected_markers,old,expected_mistakes',
                         check_answer_data)
def test_check_answer(markup, answer_markers, kwargs,
                      expected_markers, old, expected_mistakes):
    """
    Тест проверки ответа
    """
    problem = Problem(markup=markup)
    answer = Answer(theory={}, markers=deepcopy(answer_markers), **kwargs)
    expected_max_mistakes = 2
    checked_answer = check_answer(problem, answer)
    assert checked_answer.markers == expected_markers, (
        u'Неправильно проверили ответы на маркеры')
    assert (checked_answer is answer) == old, (
        u'Проверяем создался ли новый объект')
    assert answer.markers == answer_markers, (
        u'Исходный ответ не должен измениться')
    assert checked_answer.mistakes == expected_mistakes, (
        u'Неправилное число ошибок в проверенном ответе')
    assert checked_answer.max_mistakes == expected_max_mistakes, (
        u'Неправилное число ошибок в проверенном ответе')
