from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.problems.markers import ChoiceMarker, Marker


class TestChoiceMarker(object):
    """
    Тесты маркера выбора
    """
    CHECK_DATA = (
        (
            {
                'type': 'choice',
                'options': {
                    'type_display': None,
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                },
            },
            [0, 1],
            [0],
            ([Marker.CORRECT], 1),
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'type_display': None,
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'flavor': 'checkbox',
                },
            },
            [0, 1],
            [0, 1],
            ([Marker.CORRECT, Marker.CORRECT], 0),
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'type_display': None,
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                },
            },
            [0, 1],
            [],
            ([], 2),
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'type_display': None,
                },
            },
            [0],
            [1],
            ([Marker.INCORRECT], 2),
        ),
        (
            {
                'type': 'choice',
                'answer': [0, 2],
                'options': {
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'type_display': None,
                },
            },
            [0, 1],
            None,
            (Marker.SKIPPED, 2),
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'flavor': 'radio',
                },
            },
            [1],
            [1],
            ([Marker.CORRECT], 0),
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'flavor': 'radio',
                },
            },
            [1],
            [0],
            ([Marker.INCORRECT], 1),
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'flavor': 'radio',
                },
            },
            [1],
            None,
            (Marker.SKIPPED, 1),
        ),
    )
    INVALID_USER_ANSWER_FORMAT = (
        (
            [],
            ['Choice marker with `radio` '
             'flavor can only have one right answer'],
        ),
        (
            [0, 1],
            ['Choice marker with `radio` '
             'flavor can only have one right answer'],
        ),
    )
    VALID_FORMAT = (
        ({
            'type': 'choice',
            'options': {
                'choices': [],
            },
        }, []),
        ({
            'type': 'choice',
            'options': {
                'choices': [
                    u'раз',
                    u'два',
                    u'четыре',
                ],
            },
        }, [0, 2]),
        ({
            'type': 'choice',
            'options': {
                'choices': [
                    u'раз',
                    u'два',
                    u'четыре',
                ],
                'type_display': 'horizontal',
            },
        }, [0, 2]),
    )
    INVALID_FORMAT = (
        (
            {
                'type': 'choice',
                'options': {},
            },
            [],
            ["'choices' is a required property"],
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': ['1', '2']
                },
            },
            '1',
            ["'1' is not valid under any of the given schemas"],
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': ['1', '2'],
                    'type_display': 'block',
                },
            },
            [],
            ["'block' is not one of [None, "
             "'table', 'vertical', 'horizontal', 'absolute', "
             "'2cols', '3cols']"],
        ),
        (
            {
                'type': 'choice',
                'options': {
                    'choices': [
                        u'Брежнев',
                        u'Горбачев',
                        u'Ленин',
                    ],
                    'flavor': 'radio',
                },
            },
            [1, 2],
            ['Choice marker with `radio` flavor'
             ' can only have one right answer'],
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
        assert ChoiceMarker(marker, answer).check(user_answer) == result

    @pytest.mark.parametrize('user_answer,errors', INVALID_USER_ANSWER_FORMAT)
    def test_validate_answer_negative(self, user_answer, errors):
        """
        Проверка валидации ответа пользователя на одиночный выбор
        """
        marker_data = {
            'type': 'choice',
            'options': {
                'choices': [
                    u'Брежнев',
                    u'Горбачев',
                    u'Ленин',
                ],
                'flavor': 'radio',
            }
        }
        marker_answer = [1]

        with pytest.raises(ValidationError) as excinfo:
            (ChoiceMarker(marker_data, marker_answer)
                .validate_answer(user_answer))
        assert excinfo.value.messages == errors, (
            u'Неправильное сообщение об ошибке')

    def test_get_embedded_objects(self):
        """
        Проверяем нахождение формул и ресурсов в разметке
        """
        marker = ChoiceMarker({
            'type': 'choice',
            'options': {
                'choices': [
                    u'{formula:1}',
                    u'{resource:2}',
                    u'Еще вариант {formula:2}',
                ],
            },
        }, [2, 3])
        expected = [
            ('formula', '1'),
            ('resource', '2'),
            ('formula', '2'),
        ]
        assert marker.get_embedded_objects() == expected, (
            u'Неправильно найдены объекты')

    @pytest.mark.parametrize('marker_data,marker_answer', VALID_FORMAT)
    def test_validate_positive(self, marker_data, marker_answer):
        """
        Примеры правильной разметки маркера ввода
        """
        try:
            ChoiceMarker(marker_data, marker_answer).validate()
        except ValidationError as e:
            pytest.fail('Unexpected exception: {0}'.format(e))

    @pytest.mark.parametrize('marker_data,marker_answer,errors',
                             INVALID_FORMAT)
    def test_validate_negative(self, marker_data, marker_answer, errors):
        """
        Примеры неправильной разметки маркера ввода
        """
        with pytest.raises(ValidationError) as excinfo:
            ChoiceMarker(marker_data, marker_answer).validate()
        assert excinfo.value.messages == errors, (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize('marker,answer,max_mistakes', MAX_MISTAKES_DATA)
    def test_max_attempts(self, marker, answer, max_mistakes):
        """Тест максимального числа ошибок"""
        assert ChoiceMarker(marker, answer).max_mistakes == max_mistakes, (
            u'Неправильное число ошибок')
