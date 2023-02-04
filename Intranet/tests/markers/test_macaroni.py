from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import MacaroniMarker, Marker


class TestMacaroniMarker(object):
    """
    Тесты маркера сопоставления
    """
    DEFAULT_TEST_MARKUP = {
        'type': 'macaroni',
        'options': {
            'left': {
                'naming': 'abc',
                'choices': [1, 2, 3],
            },
            'right': {
                'naming': 'abc',
                'choices': [4, 5, 6],
            },
        },
    }
    DEFAULT_TEST_ANSWER = [
        [[0, 0], [1, 1], [2, 2], [0, 1]],
        [[1, 2]],
    ]
    CHECK_DATA = (
        (
            # Пользователь пропустил задание
            DEFAULT_TEST_MARKUP,
            [[[0, 0], [1, 1], [2, 2], [0, 1]]],
            None,
            (Marker.SKIPPED, 4)
        ),
        (
            # Пользователь дал пустой ответ
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abc',
                        'choices': [1, 2, 3],
                    },
                    'right': {
                        'naming': 'abc',
                        'choices': [4, 5, 6],
                    },
                },
            },
            DEFAULT_TEST_ANSWER,
            [],
            ({
                "edges_status": [],
                "missing_edges": [[1, 2]],
                "compare_with_answer": 1,
            }, 1)
        ),
        (
            # Пользователь дал правильный (но не первый ответ)
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abc',
                        'choices': [1, 2, 3],
                    },
                    'right': {
                        'naming': 'abc',
                        'choices': [4, 5, 6],
                    },
                },
            },
            DEFAULT_TEST_ANSWER,
            [[1, 2]],
            ({"compare_with_answer": 1}, 0)
        ),
        (
            # Пользователь дал неправильный ответ. Проверяем, что разница
            # считается относительного оптимального ответа
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abc',
                        'choices': [1, 2, 3],
                    },
                    'right': {
                        'naming': 'abc',
                        'choices': [4, 5, 6],
                    },
                },
            },
            DEFAULT_TEST_ANSWER,
            [[1, 2], [1, 1]],
            ({
                "edges_status": [1, 0],
                "missing_edges": [],
                "compare_with_answer": 1,
            }, 1)
        ),
        (
            # Пользователь указал много лишних ответов
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abc',
                        'choices': [1, 2, 3],
                    },
                    'right': {
                        'naming': 'abc',
                        'choices': [4, 5, 6],
                    },
                },
            },
            [[[1, 2]]],
            [[1, 2], [1, 1], [2, 2], [0, 2]],
            ({
                "edges_status": [1, 0, 0, 0],
                "missing_edges": [],
                "compare_with_answer": 0,
            }, 3)
        ),
        (
            # Все к одному (пользователь указал правильный ответ)
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abc',
                        'choices': [1, 2, 3],
                    },
                    'right': {
                        'naming': 'abc',
                        'choices': [4],
                    },
                },
            },
            [[[0, 0], [1, 0], [2, 0]]],
            [[0, 0], [2, 0], [1, 0]],
            ({"compare_with_answer": 0}, 0)
        ),
        (
            # Все к одному (пользователь указал неправильный ответ)
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abc',
                        'choices': [1, 2, 3],
                    },
                    'right': {
                        'naming': 'abc',
                        'choices': [4, 5],
                    },
                },
            },
            [[[0, 0], [1, 0], [2, 0]]],
            [[0, 0], [2, 0], [1, 1]],
            ({
                "edges_status": [1, 1, 0],
                "missing_edges": [[1, 0]],
                "compare_with_answer": 0,
            }, 2)
        ),
    )
    VALID_FORMAT = (
        ({
            'type': 'macaroni',
            'options': {
                'left': {
                    'naming': 'abcLocal',
                    'choices': [],
                    'title': '1 to 3',
                },
                'right': {
                    'naming': 'numbers',
                    'choices': [],
                    'title': '4 to 6',
                },
            },
        }, [[[0, 0]]]),
        ({
            'type': 'macaroni',
            'options': {
                'left': {
                    'naming': 'abcLocal',
                    'hidden': True,
                    'choices': [
                        u'Кролик',
                        u'Лисица',
                        u'Собака',
                    ],
                    'title': u'Звери',
                },
                'right': {
                    'naming': 'abcLocal',
                    'hidden': False,
                    'choices': [
                        u'Зайцы',
                        u'Собаки',
                    ],
                },
            },
        }, [[[1, 1]], [[0, 0], [1, 1]]]),
    )
    INVALID_FORMAT = (
        (
            {
                'type': 'macaroni',
            },
            [[[0, 0]]],
            ["'options' is a required property"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {},
            },
            [[[0, 0]]],
            ["'left' is a required property"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'choices': [
                            '12', '13'
                        ],
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                },
            },
            [[[0, 0]]],
            ["'naming' is a required property"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'numbers',
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                },
            },
            [[[0, 0]]],
            ["'choices' is a required property"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'newNaming',
                        'choices': [],
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': [],
                    },
                },
            },
            [[[0, 0]]],
            ["'newNaming' is not one of ['abc', 'abcLower', 'abcLocal', "
             "'abcLocalLower', 'numbers']"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abcLocal',
                        'choices': ['a', 'b', 'c'],
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': ['d', 'e', 'f'],
                    },
                },
            },
            [[[1, 2, 3]]],
            ["[1, 2, 3] is too long"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abcLocal',
                        'choices': ['a', 'b', 'c'],
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': ['d', 'e', 'f'],
                    },
                },
            },
            [[[1]]],
            ["[1] is too short"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abcLocal',
                        'choices': ['a', 'b', 'c'],
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': ['d', 'e', 'f'],
                    },
                },
            },
            [[]],
            ["[] is too short"],
        ),
        (
            {
                'type': 'macaroni',
                'options': {
                    'left': {
                        'naming': 'abcLocal',
                        'choices': ['a', 'b', 'c'],
                    },
                    'right': {
                        'naming': 'abcLocal',
                        'choices': ['d', 'e', 'f'],
                    },
                },
            },
            [],
            ["[] is too short"],
        ),
    )
    VALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER[0],
        ),
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER[1],
        ),
    )
    INVALID_USER_ANSWER_FORMAT = (
        (
            DEFAULT_TEST_MARKUP,
            DEFAULT_TEST_ANSWER,
            DEFAULT_TEST_ANSWER,
            '{} is not valid under any of the given schemas'
            .format(DEFAULT_TEST_ANSWER),
        ),
    )

    @pytest.mark.parametrize('marker,answer,user_answer,result', CHECK_DATA)
    def test_check(self, marker, answer, user_answer, result):
        """
        Проверка подсчета ошибок
        """
        assert MacaroniMarker(marker, answer).check(user_answer) == result

    def test_get_embedded_objects(self):
        """
        Проверяем нахождение формул и ресурсов в разметке
        """
        marker = MacaroniMarker({
            'type': 'macaroni',
            'options': {
                'left': {
                    'naming': 'numbers',
                    'choices': [
                        u'{formula:1}',
                        u'',
                        u'Еще {resource:100} вариант {formula:2}',
                    ],
                },
                'right': {
                    'naming': 'abc',
                    'choices': [
                        u'',
                        u'{resource:4}',
                        u'Еще вариант {formula:3}',
                    ],
                },
            },
        }, [[[0, 0], [1, 1], [2, 2]]])
        expected = [
            ('formula', '1'),
            ('resource', '100'),
            ('formula', '2'),
            ('resource', '4'),
            ('formula', '3'),
        ]
        assert marker.get_embedded_objects() == expected, (
            u'Неправильно найдены объекты')

    @pytest.mark.parametrize('marker_data,marker_answer',
                             VALID_FORMAT)
    def test_validate_positive(self, marker_data, marker_answer):
        """
        Примеры правильной разметки маркера
        """
        try:
            MacaroniMarker(marker_data, marker_answer).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize('marker_data,marker_answer,errors',
                             INVALID_FORMAT)
    def test_validate_negative(self, marker_data, marker_answer, errors):
        """
        Примеры неправильной разметки маркера макарон
        """
        with pytest.raises(ValidationError) as excinfo:
            MacaroniMarker(marker_data, marker_answer).validate()
        assert excinfo.value.messages == errors, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize(
        'markup,marker_answer,user_answer',
        VALID_USER_ANSWER_FORMAT
    )
    def test_validate_user_answer_positive(
            self, markup, marker_answer, user_answer):
        """
        Примеры правильного пользовательского ответа
        маркера выделений
        """
        try:
            MacaroniMarker(
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
        маркера выделений
        """
        with pytest.raises(ValidationError) as excinfo:
            MacaroniMarker(
                markup,
                marker_answer
            ).validate_answer(user_answer)
        assert excinfo.value.message == error, (
            u'Неправильное сообщение об ошибке')
