from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import Marker, MatchingMarker


class TestMatchingMarker(object):
    """
    Тесты маркера сопоставления
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'matching',
        'options': {
            'keys': {
                'title': u'Звери',
                'naming': 'abcLocal',
                'choices': [
                    u'Кролик',
                    u'Лисица',
                    u'Собака',
                ],
                'hidden': True,
            },
            'values': {
                'title': u'Семейство',
                'naming': 'abcLocal',
                'choices': [
                    u'Зайцы',
                    u'Собаки',
                ],
            },
        },
    }
    DEFAULT_TEST_ANSWER = {
        u'А': ['A'],
        u'Б': ['A', 'B'],
        u'В': 'B'
    }
    CHECK_DATA = (
        (
            {
                'type': 'matching',
            },
            DEFAULT_TEST_ANSWER,
            None,
            (Marker.SKIPPED, 4)
        ),
        (
            {
                'type': 'matching',
            },
            DEFAULT_TEST_ANSWER,
            {},
            ({}, 4)
        ),
        (
            {
                'type': 'matching',
            },
            DEFAULT_TEST_ANSWER,
            {u'Б': ['A']},
            ({u'Б': [Marker.CORRECT]}, 3)
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'keys': {
                        'title': u'Звери',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Кролик',
                            u'Лисица',
                            u'Собака',
                        ],
                        'hidden': True,
                    },
                    'values': {
                        'title': u'Семейство',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Зайцы',
                            u'Собаки',
                        ],
                    },
                },
            },
            DEFAULT_TEST_ANSWER,
            {u'А': 'A', u'Б': ['A', 'B'], u'В': 'B'},
            ({u'А': Marker.CORRECT, u'Б': [Marker.CORRECT, Marker.CORRECT],
              u'В': Marker.CORRECT}, 0),
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'type_display': None,
                    'keys': {
                        'title': u'Звери',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Кролик',
                            u'Лисица',
                            u'Собака',
                        ],
                    },
                    'values': {
                        'title': u'Семейство',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Зайцы',
                            u'Собаки',
                        ],
                        'hidden': False,
                    },
                },
            },
            {u'А': 'A', u'Б': 'B', u'В': 'B'},
            {u'А': 'T', u'Б': 'B', u'В': ['A', 'B']},
            ({u'А': Marker.INCORRECT, u'Б': Marker.CORRECT,
              u'В': [Marker.INCORRECT, Marker.CORRECT]}, 1),
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'type_display': 'row',
                    'keys': {
                        'title': u'Звери',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Кролик',
                            u'Лисица',
                            u'Собака',
                        ],
                    },
                    'values': {
                        'title': u'Семейство',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Зайцы',
                            u'Собаки',
                        ],
                        'hidden': False,
                    },
                },
            },
            {u'А': 'A', u'Б': 'B', u'В': 'B'},
            {u'Б': 'B'},
            ({u'Б': Marker.CORRECT}, 2),
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'type_display': 'column',
                    'keys': {
                        'title': u'Звери',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Кролик',
                            u'Лисица',
                            u'Собака',
                        ],
                    },
                    'values': {
                        'title': u'Семейство',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Зайцы',
                            u'Собаки',
                        ],
                        'hidden': False,
                    },
                },
            },
            {u'А': 'A', u'Б': 'B', u'В': 'B'},
            {u'Б': 'B', u'Д': 'A'},
            ({u'Б': Marker.CORRECT}, 2),
        ),
    )
    VALID_FORMAT = (
        (
            {
                'type': 'matching',
                'options': {
                    'keys': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                    'values': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                },
            },
            {}
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'type_display': 'row',
                    'keys': {
                        'title': u'Звери',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Кролик',
                            u'Лисица',
                            u'Собака',
                        ],
                    },
                    'values': {
                        'title': u'Семейство',
                        'naming': 'abcLocal',
                        'choices': [
                            u'Зайцы',
                            u'Собаки',
                        ],
                    },
                },
            },
            DEFAULT_TEST_ANSWER
        ),
    )
    INVALID_FORMAT = (
        (
            {
                'type': 'matching',
                'options': {
                    'keys': {
                        'naming': '',
                    },
                    'values': {
                        'naming': 'numbers',
                        'choices': [],
                    },
                },
            },
            {},
            ["'choices' is a required property"],
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'keys': {
                        'naming': 'abcLocal',
                        'choices': [
                            123,
                        ],
                    },
                    'values': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                },
            },
            {},
            ["123 is not of type 'string'"],
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'keys': {
                        'choices': [
                            '123',
                        ],
                    },
                    'values': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                },
            },
            {},
            ["'naming' is a required property"],
        ),
        (
            {
                'type': 'matching',
                'options': {
                    'keys': {
                        'naming': 'abcLocal',
                        'choices': [
                            '123',
                            '456',
                        ],
                    },
                    'values': {
                        'naming': 'abcLocal',
                        'choices': [
                            '321',
                            '654',
                        ],
                    },
                    'type_display': 'table',
                },
            },
            {},
            ["'table' is not one of [None, 'column', 'row']"],
        ),
    )
    VALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER,
        ),
    )
    INVALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            [DEFAULT_TEST_ANSWER],
            '{} is not valid under any of '
            'the given schemas'.format([DEFAULT_TEST_ANSWER]),
        ),
    )
    MAX_MISTAKES_DATA = (
        (
            VALID_FORMAT[0][0],
            VALID_FORMAT[0][1],
            1,
        ),
        (
            VALID_FORMAT[1][0],
            VALID_FORMAT[1][1],
            3,
        ),
    )

    @pytest.mark.parametrize('marker,answer,user_answer,result', CHECK_DATA)
    def test_check(self, marker, answer, user_answer, result):
        """
        Проверка подсчета ошибок
        """
        assert MatchingMarker(marker, answer).check(user_answer) == result

    def test_get_embedded_objects(self):
        """
        Проверяем нахождение формул и ресурсов в разметке
        """
        marker = MatchingMarker({
            'type': 'matching',
            'options': {
                'keys': {
                    'title': u'{resource:100}',
                    'naming': 'numbers',
                    'choices': [
                        u'{formula:1}',
                        u'',
                        u'Еще {resource:100} вариант {formula:2}',
                    ],
                },
                'values': {
                    'title': u'{formula:200}',
                    'naming': 'numbers',
                    'choices': [
                        u'',
                        u'{resource:4}',
                        u'Еще вариант {formula:3}',
                    ],
                },
            },
        }, {'1': '2', '2': '1', '3': '3'})
        expected = [
            ('resource', '100'),
            ('formula', '1'),
            ('resource', '100'),
            ('formula', '2'),
            ('formula', '200'),
            ('resource', '4'),
            ('formula', '3'),
        ]
        assert marker.get_embedded_objects() == expected, (
            u'Неправильно найдены объекты')

    @pytest.mark.parametrize('marker_data,marker_answer', VALID_FORMAT)
    def test_validate_positive(self, marker_data, marker_answer):
        """
        Примеры правильной разметки маркера сопоставления
        """
        try:
            MatchingMarker(marker_data, marker_answer).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize('marker_data,marker_answer,errors', INVALID_FORMAT)
    def test_validate_negative(self, marker_data, marker_answer, errors):
        """
        Примеры неправильной разметки маркера сопоставления
        """
        with pytest.raises(ValidationError) as excinfo:
            MatchingMarker(marker_data, marker_answer).validate()
        assert excinfo.value.messages == errors, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('marker_data,marker_answer,max_mistakes', MAX_MISTAKES_DATA)
    def test_max_attempts(self, marker_data, marker_answer, max_mistakes):
        """Тест максимального числа ошибок"""
        assert MatchingMarker(marker_data,
                              marker_answer).max_mistakes == max_mistakes, (
            u'Неправильное число ошибок')

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer',
        VALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_positive(
            self, markup, marker_answer, user_answer):
        """
        Примеры правильного пользовательского ответа
        маркера сопоставления
        """
        try:
            MatchingMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer,error',
        INVALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_negative(
            self, markup, marker_answer, user_answer, error):
        """
        Примеры неправильного пользовательского ответа
        маркера сопоставления
        """
        with pytest.raises(ValidationError) as excinfo:
            MatchingMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')
